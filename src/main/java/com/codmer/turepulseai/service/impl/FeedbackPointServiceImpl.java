package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.FeedbackPointDto;
import com.codmer.turepulseai.entity.FeedbackPoint;
import com.codmer.turepulseai.entity.Retro;
import com.codmer.turepulseai.repository.FeedbackPointRepository;
import com.codmer.turepulseai.repository.RetroRepository;
import com.codmer.turepulseai.service.FeedbackPointService;
import com.codmer.turepulseai.util.RateLimitHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.codmer.turepulseai.model.FeedbackPointAnalysisRequest;
import com.codmer.turepulseai.model.FeedbackPointAnalysisResponse;
import com.codmer.turepulseai.entity.ActionItem;
import com.codmer.turepulseai.entity.Discussion;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackPointServiceImpl implements FeedbackPointService {

    private final FeedbackPointRepository feedbackPointRepository;
    private final RetroRepository retroRepository;
    private final org.springframework.ai.chat.client.ChatClient chatClient;
    private final RateLimitHandler rateLimitHandler;
    private final Executor aiAnalysisExecutor;

    @Override
    public FeedbackPointDto create(FeedbackPointDto dto) {
        Retro retro = fetchRetro(dto.getRetroId());
        FeedbackPoint f = new FeedbackPoint();
        f.setType(FeedbackPoint.FeedbackType.valueOf(dto.getType()));
        f.setDescription(dto.getDescription());
        f.setRetro(retro);
        return toDto(feedbackPointRepository.save(f));
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackPointDto getById(Long id) {
        return feedbackPointRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FeedbackPoint not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackPointDto> getAll() {
        return feedbackPointRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public FeedbackPointDto update(Long id, FeedbackPointDto dto) {
        // Validate that path ID matches dto ID if provided
        if (dto.getId() != null && !id.equals(dto.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path ID and DTO ID do not match");
        }

        FeedbackPoint f = feedbackPointRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FeedbackPoint not found"));
        if (dto.getType() != null) f.setType(FeedbackPoint.FeedbackType.valueOf(dto.getType()));
        if (dto.getDescription() != null) f.setDescription(dto.getDescription());
        if (dto.getRetroId() != null && (f.getRetro() == null || !dto.getRetroId().equals(f.getRetro().getId()))) {
            f.setRetro(fetchRetro(dto.getRetroId()));
        }
        return toDto(feedbackPointRepository.save(f));
    }

    @Override
    public void delete(Long id) {
        if (!feedbackPointRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FeedbackPoint not found");
        }
        feedbackPointRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackPointAnalysisResponse analyzeFeedbackPoint(FeedbackPointAnalysisRequest request) {
        if (request == null || request.getRetroId() == null || request.getFeedbackPointId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "retroId and feedbackPointId are required");
        }

        long startTime = System.currentTimeMillis();
        log.info("Starting feedback analysis for retroId={}, feedbackPointId={}", request.getRetroId(), request.getFeedbackPointId());

        Retro retro = fetchRetro(request.getRetroId());
        FeedbackPoint currentPoint = feedbackPointRepository.findById(request.getFeedbackPointId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback point not found"));

        if (currentPoint.getRetro() == null || !retro.getId().equals(currentPoint.getRetro().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback point does not belong to the provided retro");
        }

        // Build contexts (fast operations, can be parallelized)
        long contextStartTime = System.currentTimeMillis();
        String currentContext = buildCurrentFeedbackContext(retro, currentPoint);

        List<Retro> pastRetros = retroRepository.findByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                retro.getUser().getId(), retro.getCreatedAt());
        String historyContext = buildHistoricalFeedbackContext(pastRetros, currentPoint);
        long contextElapsedMs = System.currentTimeMillis() - contextStartTime;
        log.debug("Context building completed in {}ms", contextElapsedMs);

        // Execute summarization tasks in PARALLEL for better performance
        long summaryStartTime = System.currentTimeMillis();
        CompletableFuture<String> currentSummaryFuture = CompletableFuture.supplyAsync(
                () -> summarizeCurrentFeedback(currentContext),
                aiAnalysisExecutor
        );

        CompletableFuture<String> historicalSummaryFuture = CompletableFuture.supplyAsync(
                () -> summarizeHistoricalFeedback(historyContext),
                aiAnalysisExecutor
        );

        // Wait for both summaries to complete
        CompletableFuture<String> currentSummary = currentSummaryFuture
                .exceptionally(ex -> {
                    log.error("Error in current summary task: {}", ex.getMessage());
                    return "Unable to analyze current feedback at this moment. Please try again.";
                });

        CompletableFuture<String> historicalSummary = historicalSummaryFuture
                .exceptionally(ex -> {
                    log.error("Error in historical summary task: {}", ex.getMessage());
                    return "Unable to analyze historical patterns at this moment. Please try again.";
                });

        // Combine results
        String feedbackType = currentPoint.getType().toString();
        String finalSummary = currentSummary.thenCombine(historicalSummary,
                        (current, historical) -> mergeFeedbackSummaries(current, historical, feedbackType))
                .exceptionally(ex -> {
                    log.error("Error in merge summary task: {}", ex.getMessage());
                    return "Analysis completed with limitations. Please try again for full analysis.";
                })
                .join();  // Block and wait for result

        long summaryElapsedMs = System.currentTimeMillis() - summaryStartTime;
        long totalElapsedMs = System.currentTimeMillis() - startTime;

        log.info("Feedback analysis completed in {}ms (summaries: {}ms)", totalElapsedMs, summaryElapsedMs);

        return new FeedbackPointAnalysisResponse(
                retro.getId(),
                currentPoint.getId(),
                currentPoint.getType().toString(),
                finalSummary
        );
    }

    private String buildCurrentFeedbackContext(Retro retro, FeedbackPoint currentPoint) {
        StringBuilder sb = new StringBuilder(256);  // Pre-allocate capacity
        sb.append("Retro Title: ").append(retro.getTitle()).append("\n");
        sb.append("Retro Description: ").append(retro.getDescription()).append("\n");
        sb.append("Feedback Type: ").append(currentPoint.getType()).append("\n");
        sb.append("Feedback Description: ").append(currentPoint.getDescription()).append("\n\n");

        if (currentPoint.getDiscussions() != null && !currentPoint.getDiscussions().isEmpty()) {
            sb.append("Discussions for this feedback:\n");
            for (Discussion discussion : currentPoint.getDiscussions()) {
                sb.append("- ").append(discussion.getNote()).append("\n");
            }
        } else {
            sb.append("Discussions for this feedback: None\n");
        }

        if (retro.getActionItems() != null && !retro.getActionItems().isEmpty()) {
            sb.append("\nAction Items in this retro:\n");
            for (ActionItem item : retro.getActionItems()) {
                sb.append("- ").append(item.getDescription())
                        .append(" | Status: ").append(item.getStatus())
                        .append(" | Completed: ").append(item.isCompleted())
                        .append(" | Assigned: ").append(item.getAssignedUserName() != null ? item.getAssignedUserName() : "Unassigned")
                        .append("\n");
            }
        } else {
            sb.append("\nAction Items in this retro: None\n");
        }

        return sb.toString();
    }

    private String buildHistoricalFeedbackContext(List<Retro> pastRetros, FeedbackPoint currentPoint) {
        if (pastRetros == null || pastRetros.isEmpty()) {
            return "No past retros available.";
        }

        String currentText = currentPoint.getDescription() != null ? currentPoint.getDescription() : "";
        StringBuilder sb = new StringBuilder(512);  // Pre-allocate capacity
        sb.append("Past retros with similar feedback or patterns:\n");

        for (Retro retro : pastRetros) {
            if (retro.getFeedbackPoints() == null || retro.getFeedbackPoints().isEmpty()) {
                continue;
            }

            for (FeedbackPoint fp : retro.getFeedbackPoints()) {
                if (isSimilarFeedback(fp, currentPoint, currentText)) {
                    sb.append("Retro: ").append(retro.getTitle()).append("\n");
                    sb.append("- Feedback Type: ").append(fp.getType()).append("\n");
                    sb.append("- Feedback Description: ").append(fp.getDescription()).append("\n");
                    if (fp.getDiscussions() != null && !fp.getDiscussions().isEmpty()) {
                        sb.append("- Discussions:\n");
                        for (Discussion discussion : fp.getDiscussions()) {
                            sb.append("  * ").append(discussion.getNote()).append("\n");
                        }
                    }
                    if (retro.getActionItems() != null && !retro.getActionItems().isEmpty()) {
                        sb.append("- Action Items:\n");
                        for (ActionItem item : retro.getActionItems()) {
                            sb.append("  * ").append(item.getDescription())
                                    .append(" | Status: ").append(item.getStatus())
                                    .append(" | Completed: ").append(item.isCompleted())
                                    .append("\n");
                        }
                    }
                    sb.append("\n");
                }
            }
        }

        if (sb.toString().equals("Past retros with similar feedback or patterns:\n")) {
            sb.append("No similar feedback points found in past retros.\n");
        }

        return sb.toString();
    }

    private boolean isSimilarFeedback(FeedbackPoint pastPoint, FeedbackPoint currentPoint, String currentText) {
        if (pastPoint.getType() == currentPoint.getType()) {
            return true;
        }
        if (pastPoint.getDescription() == null || currentText.isEmpty()) {
            return false;
        }
        String pastText = pastPoint.getDescription().toLowerCase();
        String[] tokens = currentText.toLowerCase().split("\\s+");
        for (String token : tokens) {
            if (token.length() > 4 && pastText.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String summarizeCurrentFeedback(String context) {
        String systemPrompt = """
                You are the Scrum Master who has been working with this team across multiple sprints.
                Analyze the CURRENT feedback point and its discussions.
                Identify whether the feedback is appreciation or a concern.
                Infer the dominant emotion (e.g., pride, excitement, concern, frustration, fatigue) from wording and context.
                Explain likely root causes, team dynamics, and immediate next-step actions.
                Offer concise coaching suggestions and recognize team effort if appreciation is present.
                Use natural, human language and avoid repetitive sentence starters.
                Do not mention AI or that you are a model.
                """;

        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        messages.add(new org.springframework.ai.chat.messages.UserMessage(
            context + "\nSummarize current feedback in 5-7 sentences. Include: emotion, appreciation/concern classification, key evidence, and immediate next action."));

        try {
            String content = rateLimitHandler.executeWithRateLimitRetry(() -> {
                var response = chatClient.prompt(new org.springframework.ai.chat.prompt.Prompt(messages)).call();
                return response.content();
            }, "summarizeCurrentFeedback");
            return content == null ? "No current feedback analysis available." : content.trim();
        } catch (Exception ex) {
            log.error("Error summarizing current feedback", ex);
            throw ex;
        }
    }

    private String summarizeHistoricalFeedback(String history) {
        String systemPrompt = """
                You are a senior Scrum Master reviewing historical retrospectives.
                Identify if the feedback pattern is recurring, how the team addressed it in the past,
                and whether the issue is improving or persisting. Extract practical lessons learned.
                Highlight one concrete pattern shift (better, same, or worse) and why.
                Keep wording concise, specific, and non-repetitive.
                """;

        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        messages.add(new org.springframework.ai.chat.messages.UserMessage(
            history + "\nSummarize historical patterns in 4-6 sentences. Include: recurrence level, previous handling, trend direction, and one practical lesson."));

        try {
            String content = rateLimitHandler.executeWithRateLimitRetry(() -> {
                var response = chatClient.prompt(new org.springframework.ai.chat.prompt.Prompt(messages)).call();
                return response.content();
            }, "summarizeHistoricalFeedback");
            return content == null ? "No historical feedback analysis available." : content.trim();
        } catch (Exception ex) {
            log.error("Error summarizing historical feedback", ex);
            throw ex;
        }
    }

    private String mergeFeedbackSummaries(String currentSummary, String historicalSummary, String feedbackType) {
        int styleSeed = ThreadLocalRandom.current().nextInt(6);
        String openerStyleMode = switch (styleSeed) {
            case 0 -> "playful";
            case 1 -> "warm";
            case 2 -> "reflective";
            case 3 -> "celebratory";
            case 4 -> "steady-coaching";
            default -> "energizing";
        };
        String casualnessLevel = switch (styleSeed % 3) {
            case 0 -> "low";
            case 1 -> "medium";
            default -> "high";
        };

        String systemPrompt = """
                You are the Scrum Master who has been working with this team over past sprints.
                Combine the current feedback analysis with historical patterns into a final response.
                Requirements:
                                                                - The feedback belongs to section: %s. Use this section explicitly while framing your response.
                                                                - Section intent guide:
                                                                    * LIKED: reinforce positives, behaviors to repeat, and momentum.
                                                                    * LEARNED: highlight insight gained and how to apply it next sprint.
                                                                    * LACKED: acknowledge gap, likely blockers, and concrete corrective step.
                                                                    * LONGED_FOR: reflect unmet need and practical way to introduce/support it.
                                - Infer the dominant emotion first (joy, pride, relief, excitement, gratitude, concern, frustration, fatigue, tension, uncertainty) and match your opener to it.
                                - Start with a feeling-based opener that matches the emotion in the feedback.
                                - For positives, rotate from options like: "Yay, that's a big win!", "Woohoo, this is real momentum!", "What a fantastic milestone!", "I'm genuinely impressed...", "This is a proud moment...", "Such inspiring teamwork!", "What a breakthrough!", "This really energizes the team!", "I'm thrilled to see...", "This is a big win!", "What a creative solution!", "You all should feel proud!", "Love this progress!", "Brilliant execution here!", "That is seriously solid teamwork.", "What a strong step forward!", "This is the kind of progress we needed.", "Huge credit to the team on this one.", "This is exciting to see.", "Now this is a confidence boost!", "Great signal that the team is leveling up.", "This is a meaningful achievement.", "Outstanding follow-through.", "This is exactly the momentum we want.", "What a sharp improvement!".
                                - For concerns, rotate from options like: "Hmm, I can feel the tension here...", "Oof, this sounds heavy...", "I sense the challenge here...", "This must feel tough...", "I can see the frustration...", "Let's acknowledge the struggle...", "This is a real hurdle...", "I hear the concern...", "It's okay to feel this way...", "This is a tough spot...", "Let's work through this together...", "I appreciate the honesty here...", "I can feel the strain in this one.", "This is a tricky patch, and that's real.", "I hear the fatigue behind this.", "This friction is important to surface.", "That's a tough pattern to carry sprint after sprint.", "This pain point deserves focused attention.", "I can see why this feels draining.", "This is hard, and you're naming it clearly.", "Let's slow this down and unpack it.", "This concern is valid and worth addressing.", "I hear both effort and frustration here.", "This is exactly the kind of issue retros should surface.", "That's a meaningful warning sign for the team.".
                                - For mixed emotions (progress + pain), use openers like: "Ah, good progress with a clear challenge still in play...", "Phew, we moved forward but there's still pressure here...", "Alright, this is better, and we still have work to do...", "Nice, we can see improvement and one stubborn gap...", "Hey, this is encouraging while still highlighting a risk...", "Okay, this is a step up with one friction point to resolve...".
                                - Also allow lively human interjections when natural: "Yay, ...", "Hmm, ...", "Ah, ...", "Ahh, ...", "Oof, ...", "Woohoo, ...", "Hey, ...", "Nice, ...", "Alright, ...", "Alrighty, ...", "Phew, ...", "Wow, ...", "Okay, ...", "Right, ...", "Honestly, ...", "Look, ...", "Love this, ...", "Good sign, ...", "Fair point, ...", "Totally hear that, ...".
                                - Use a fresh opener each time and vary sentence rhythm. Avoid repeating the same first 2-3 words.
                                - Do NOT start with "This feedback" and avoid repeatedly starting with generic concern words.
                                - Avoid cliches and robotic templates; sound like a real facilitator speaking in a live retro.
                                - Clearly cover: whether this is appreciation or concern, how the team handled it before,
                                    whether it is recurring/improving, and the most practical next step.
                                - Add one concise coaching tip framed as collaborative guidance ("let's", "we can", "try").
                                - Keep it to 1-2 lines, human, direct, and conversational. Do not mention AI or models.
                """.formatted(feedbackType);

        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        messages.add(new org.springframework.ai.chat.messages.UserMessage(
            "Feedback Type Section: " + feedbackType +
            "\n\n" +
                "Current Analysis:\n" + currentSummary + "\n\nHistorical Analysis:\n" + historicalSummary +
            "\n\nOpener style mode for this response: " + openerStyleMode +
            " | Casualness: " + casualnessLevel +
            "\nGenerate final summary in 1-2 lines with varied wording and a non-repetitive opener."));

        try {
            String content = rateLimitHandler.executeWithRateLimitRetry(() -> {
                var response = chatClient.prompt(new org.springframework.ai.chat.prompt.Prompt(messages)).call();
                return response.content();
            }, "mergeFeedbackSummaries");
            return content == null ? "No combined analysis available." : content.trim();
        } catch (Exception ex) {
            log.error("Error merging feedback summaries", ex);
            throw ex;
        }
    }

    private Retro fetchRetro(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "retroId is required");
        }
        return retroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Retro not found"));
    }

    private FeedbackPointDto toDto(FeedbackPoint f) {
        return new FeedbackPointDto(
                f.getId(),
                f.getType().toString(),
                f.getDescription(),
                f.getRetro() != null ? f.getRetro().getId() : null,
                f.getCreatedAt(),
                f.getUpdatedAt()
        );
    }
}

