package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.FeedbackPointDto;
import com.codmer.turepulseai.entity.FeedbackPoint;
import com.codmer.turepulseai.entity.Retro;
import com.codmer.turepulseai.repository.FeedbackPointRepository;
import com.codmer.turepulseai.repository.RetroRepository;
import com.codmer.turepulseai.service.FeedbackPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.codmer.turepulseai.model.FeedbackPointAnalysisRequest;
import com.codmer.turepulseai.model.FeedbackPointAnalysisResponse;
import com.codmer.turepulseai.entity.ActionItem;
import com.codmer.turepulseai.entity.Discussion;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackPointServiceImpl implements FeedbackPointService {

    private final FeedbackPointRepository feedbackPointRepository;
    private final RetroRepository retroRepository;
    private final org.springframework.ai.chat.client.ChatClient chatClient;

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

        Retro retro = fetchRetro(request.getRetroId());
        FeedbackPoint currentPoint = feedbackPointRepository.findById(request.getFeedbackPointId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback point not found"));

        if (currentPoint.getRetro() == null || !retro.getId().equals(currentPoint.getRetro().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback point does not belong to the provided retro");
        }

        String currentContext = buildCurrentFeedbackContext(retro, currentPoint);
        String currentSummary = summarizeCurrentFeedback(currentContext);

        List<Retro> pastRetros = retroRepository.findByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                retro.getUser().getId(), retro.getCreatedAt());
        String historyContext = buildHistoricalFeedbackContext(pastRetros, currentPoint);
        String historicalSummary = summarizeHistoricalFeedback(historyContext);

        String finalSummary = mergeFeedbackSummaries(currentSummary, historicalSummary);

        return new FeedbackPointAnalysisResponse(
                retro.getId(),
                currentPoint.getId(),
                currentPoint.getType().toString(),
                finalSummary
        );
    }

    private String buildCurrentFeedbackContext(Retro retro, FeedbackPoint currentPoint) {
        StringBuilder sb = new StringBuilder();
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
        StringBuilder sb = new StringBuilder();
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
                Explain likely root causes, team dynamics, and immediate next-step actions.
                Offer concise coaching suggestions and recognize team effort if appreciation is present.
                Write in a human, empathetic tone. Do not mention AI or that you are a model.
                """;

        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        messages.add(new org.springframework.ai.chat.messages.UserMessage(
                context + "\nSummarize current feedback in 5-7 sentences."));

        var response = chatClient.prompt(new org.springframework.ai.chat.prompt.Prompt(messages)).call();
        String content = response.content();
        return content == null ? "No current feedback analysis available." : content.trim();
    }

    private String summarizeHistoricalFeedback(String history) {
        String systemPrompt = """
                You are a senior Scrum Master reviewing historical retrospectives.
                Identify if the feedback pattern is recurring, how the team addressed it in the past,
                and whether the issue is improving or persisting. Extract practical lessons learned.
                """;

        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        messages.add(new org.springframework.ai.chat.messages.UserMessage(
                history + "\nSummarize historical patterns in 4-6 sentences."));

        var response = chatClient.prompt(new org.springframework.ai.chat.prompt.Prompt(messages)).call();
        String content = response.content();
        return content == null ? "No historical feedback analysis available." : content.trim();
    }

    private String mergeFeedbackSummaries(String currentSummary, String historicalSummary) {
        String systemPrompt = """
                You are the Scrum Master who has been working with this team over past sprints.
                Combine the current feedback analysis with historical patterns into a final response.
                Requirements:
                - Start with a feeling-based opener (e.g., "Yay...", "Great work...", "I appreciate..." for positives,
                  or "I hear the frustration...", "I feel the strain..." for concerns).
                - Do NOT start with "This feedback" and do not repeat the same sentence pattern each time.
                - Cover whether this is appreciation or concern, how the team handled it before,
                  whether it is recurring or improving, and clear guidance for next steps.
                - Try for a suggestion or coaching tip that can help in tackling it.
                - Keep it to 3-4 lines, human and direct. Do not mention AI or models.
                """;

        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        messages.add(new org.springframework.ai.chat.messages.UserMessage(
                "Current Analysis:\n" + currentSummary + "\n\nHistorical Analysis:\n" + historicalSummary +
                        "\n\nGenerate final summary."));

        var response = chatClient.prompt(new org.springframework.ai.chat.prompt.Prompt(messages)).call();
        String content = response.content();
        return content == null ? "No combined analysis available." : content.trim();
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

