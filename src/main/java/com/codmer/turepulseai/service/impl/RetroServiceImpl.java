package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.RetroDto;
import com.codmer.turepulseai.model.RetroDetailDto;
import com.codmer.turepulseai.model.RetroAnalysisResponse;
import com.codmer.turepulseai.entity.Retro;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.entity.FeedbackPoint;
import com.codmer.turepulseai.entity.Discussion;
import com.codmer.turepulseai.entity.ActionItem;
import com.codmer.turepulseai.repository.RetroRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.RetroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RetroServiceImpl implements RetroService {

    private final RetroRepository retroRepository;
    private final UserRepository userRepository;
    private final org.springframework.ai.chat.client.ChatClient chatClient;


    @Override
    @Transactional(readOnly = true)
    public RetroDetailDto getRetroDetails(Long retroId) {
        Retro retro = retroRepository.findById(retroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retro not found"));

        return toDetailDto(retro);
    }

    private RetroDetailDto toDetailDto(Retro retro) {
        RetroDetailDto dto = new RetroDetailDto();
        dto.setId(retro.getId());
        dto.setTitle(retro.getTitle());
        dto.setDescription(retro.getDescription());
        dto.setUserId(retro.getUser() != null ? retro.getUser().getId() : null);
        dto.setCreatedBy(retro.getUser() != null ? retro.getUser().getUserName() : null);
        dto.setCreatedAt(retro.getCreatedAt());
        dto.setUpdatedAt(retro.getUpdatedAt());

        // Map Feedback Points with nested Discussions
        if (retro.getFeedbackPoints() != null && !retro.getFeedbackPoints().isEmpty()) {
            List<RetroDetailDto.FeedbackPointDetailDto> feedbackDtos = retro.getFeedbackPoints()
                    .stream()
                    .map(this::toFeedbackPointDetailDto)
                    .collect(Collectors.toList());
            dto.setFeedbackPoints(feedbackDtos);
        } else {
            dto.setFeedbackPoints(new java.util.ArrayList<>());
        }

        // Map Action Items with assignee details
        if (retro.getActionItems() != null && !retro.getActionItems().isEmpty()) {
            List<RetroDetailDto.ActionItemDetailDto> actionItemDtos = retro.getActionItems()
                    .stream()
                    .map(this::toActionItemDetailDto)
                    .collect(Collectors.toList());
            dto.setActionItems(actionItemDtos);
        } else {
            dto.setActionItems(new java.util.ArrayList<>());
        }


        return dto;
    }

    private RetroDetailDto.FeedbackPointDetailDto toFeedbackPointDetailDto(FeedbackPoint fp) {
        RetroDetailDto.FeedbackPointDetailDto dto = new RetroDetailDto.FeedbackPointDetailDto();
        dto.setId(fp.getId());
        dto.setType(fp.getType().toString());
        dto.setDescription(fp.getDescription());
        dto.setCreatedAt(fp.getCreatedAt());
        dto.setUpdatedAt(fp.getUpdatedAt());

        // Map nested discussions with user details
        if (fp.getDiscussions() != null && !fp.getDiscussions().isEmpty()) {
            List<RetroDetailDto.DiscussionDetailDto> discussionDtos = fp.getDiscussions()
                    .stream()
                    .map(this::toDiscussionDetailDto)
                    .collect(Collectors.toList());
            dto.setDiscussions(discussionDtos);
        } else {
            dto.setDiscussions(new java.util.ArrayList<>());
        }

        return dto;
    }

    private RetroDetailDto.DiscussionDetailDto toDiscussionDetailDto(Discussion discussion) {
        RetroDetailDto.DiscussionDetailDto dto = new RetroDetailDto.DiscussionDetailDto();
        dto.setId(discussion.getId());
        dto.setNote(discussion.getNote());

        if (discussion.getUser() != null) {
            dto.setUserId(discussion.getUser().getId());
            dto.setUserName(discussion.getUser().getUserName());
            dto.setUserFirstName(discussion.getUser().getFirstName());
            dto.setUserLastName(discussion.getUser().getLastName());
        }

        dto.setCreatedAt(discussion.getCreatedAt());
        dto.setUpdatedAt(discussion.getUpdatedAt());

        return dto;
    }

    private RetroDetailDto.ActionItemDetailDto toActionItemDetailDto(ActionItem actionItem) {
        RetroDetailDto.ActionItemDetailDto dto = new RetroDetailDto.ActionItemDetailDto();
        dto.setId(actionItem.getId());
        dto.setDescription(actionItem.getDescription());
        dto.setDueDate(actionItem.getDueDate());
        dto.setCompleted(actionItem.isCompleted());
        dto.setStatus(actionItem.getStatus().toString());
        dto.setCompletedAt(actionItem.getCompletedAt());

        if (actionItem.getAssignedUser() != null) {
            dto.setAssignedUserId(actionItem.getAssignedUser().getId());
            dto.setAssignedUserName(actionItem.getAssignedUser().getUserName());
            dto.setAssignedUserFirstName(actionItem.getAssignedUser().getFirstName());
            dto.setAssignedUserLastName(actionItem.getAssignedUser().getLastName());
        }

        dto.setCreatedAt(actionItem.getCreatedAt());
        dto.setUpdatedAt(actionItem.getUpdatedAt());

        return dto;
    }


    @Override
    public RetroDto create(RetroDto dto) {
        User user = fetchUser(dto.getUserId());
        Retro r = new Retro();
        r.setTitle(dto.getTitle());
        r.setDescription(dto.getDescription());
        r.setUser(user);
        Retro saved = retroRepository.save(r);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RetroDto getById(Long id) {
        return retroRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retro not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetroDto> getAll() {
        return retroRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetroDto> getRetrosByUserId(Long userId) {
        return retroRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(RetroDto::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public RetroDto update(Long id, RetroDto dto) {
        // Validate that path ID matches dto ID if provided
        if (dto.getId() != null && !id.equals(dto.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path ID and DTO ID do not match");
        }

        Retro r = retroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retro not found"));
        if (dto.getTitle() != null) r.setTitle(dto.getTitle());
        if (dto.getDescription() != null) r.setDescription(dto.getDescription());
        if (dto.getUserId() != null && (r.getUser() == null || !dto.getUserId().equals(r.getUser().getId()))) {
            r.setUser(fetchUser(dto.getUserId()));
        }
        return toDto(retroRepository.save(r));
    }

    @Override
    public void delete(Long id) {
        if (!retroRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retro not found");
        }
        retroRepository.deleteById(id);
    }

    private User fetchUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
    }

    private RetroDto toDto(Retro r) {
        Long userId = r.getUser() != null ? r.getUser().getId() : null;
        return new RetroDto(r.getUpdatedAt(), r.getCreatedAt(), userId, r.getDescription(), r.getTitle(), r.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public RetroAnalysisResponse analyzeRetro(Long retroId) {
        Retro retro = retroRepository.findById(retroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retro not found"));

        String currentRetroContext = buildCurrentRetroContext(retro);
        String currentRetroSummary = summarizeCurrentRetro(retro, currentRetroContext);

        List<Retro> pastRetros = retroRepository.findByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                retro.getUser().getId(), retro.getCreatedAt());
        String pastRetrosSummary = summarizePastRetros(retro.getUser().getUserName(), pastRetros);

        String finalSummary = mergeSummaries(currentRetroSummary, pastRetrosSummary);
        return new RetroAnalysisResponse(retro.getId(), finalSummary);
    }

    private String buildCurrentRetroContext(Retro retro) {
        StringBuilder sb = new StringBuilder();
        sb.append("Retro Title: ").append(retro.getTitle()).append("\n");
        sb.append("Retro Description: ").append(retro.getDescription()).append("\n\n");

        List<FeedbackPoint> feedbackPoints = retro.getFeedbackPoints();
        if (feedbackPoints != null && !feedbackPoints.isEmpty()) {
            sb.append("Feedback Points by Section:\n");
            for (FeedbackPoint fp : feedbackPoints) {
                sb.append("- ").append(fp.getType()).append(": ").append(fp.getDescription()).append("\n");
                if (fp.getDiscussions() != null && !fp.getDiscussions().isEmpty()) {
                    sb.append("  Discussions:\n");
                    for (Discussion discussion : fp.getDiscussions()) {
                        sb.append("  * ").append(discussion.getNote()).append("\n");
                    }
                }
            }
        } else {
            sb.append("Feedback Points: None\n");
        }

        List<ActionItem> actionItems = retro.getActionItems();
        if (actionItems != null && !actionItems.isEmpty()) {
            long completedCount = actionItems.stream().filter(ActionItem::isCompleted).count();
            sb.append("\nAction Items (Completed ").append(completedCount).append("/").append(actionItems.size()).append("):\n");
            for (ActionItem item : actionItems) {
                sb.append("- ").append(item.getDescription())
                        .append(" | Status: ").append(item.getStatus())
                        .append(" | Completed: ").append(item.isCompleted())
                        .append(" | Assigned: ").append(item.getAssignedUserName() != null ? item.getAssignedUserName() : "Unassigned")
                        .append("\n");
            }
        } else {
            sb.append("\nAction Items: None\n");
        }

        return sb.toString();
    }

    private String summarizeCurrentRetro(Retro retro, String context) {
        String systemPrompt = """
                You are a senior Scrum Master and retrospective facilitator.
                Analyze the current sprint retro feedback and action items.
                Produce a concise summary that covers:
                - How the team did overall this sprint
                - What went well (LIKED) and what was learned (LEARNED)
                - Where the team lacked (LACKED) and what they longed for (LONGED_FOR)
                - Appreciation received and completion of action items
                - Where the team is lacking and what to improve next sprint
                Mention standout team members if their names appear in action items or discussions.
                Keep it brief and precise.
                """;

        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        messages.add(new org.springframework.ai.chat.messages.UserMessage(
                "Current Retro ID: " + retro.getId() + "\n" + context +
                        "\nSummarize the current sprint in 2-3 short paragraphs."));

        var response = chatClient.prompt(new org.springframework.ai.chat.prompt.Prompt(messages)).call();
        String content = response.content();
        return content == null ? "No analysis available" : content.trim();
    }

    private String summarizePastRetros(String scrumMasterUserName, List<Retro> pastRetros) {
        if (pastRetros == null || pastRetros.isEmpty()) {
            return "No past retros available to identify historical patterns.";
        }

        StringBuilder history = new StringBuilder();
        for (Retro retro : pastRetros) {
            history.append("Retro: ").append(retro.getTitle()).append("\n");
            if (retro.getFeedbackPoints() != null) {
                for (FeedbackPoint fp : retro.getFeedbackPoints()) {
                    history.append("- ").append(fp.getType()).append(": ").append(fp.getDescription()).append("\n");
                }
            }
            if (retro.getActionItems() != null) {
                long completedCount = retro.getActionItems().stream().filter(ActionItem::isCompleted).count();
                history.append("Action Items Completed: ").append(completedCount)
                        .append("/").append(retro.getActionItems().size()).append("\n");
            }
            history.append("\n");
        }

        String systemPrompt = """
                You are a senior Scrum Master reviewing historical retrospectives.
                Identify patterns, improvement trends, and recurring gaps across past sprints.
                Comment on how the team is improving, how they are tackling action items,
                and highlight any persistent issues that need focus.
                Provide a concise history-based assessment.
                """;

        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        messages.add(new org.springframework.ai.chat.messages.UserMessage(
                "Scrum Master: " + scrumMasterUserName + "\n" + history +
                        "\nSummarize historical patterns in 4-6 sentences."));

        var response = chatClient.prompt(new org.springframework.ai.chat.prompt.Prompt(messages)).call();
        String content = response.content();
        return content == null ? "No historical analysis available" : content.trim();
    }

    private String mergeSummaries(String currentSummary, String historicalSummary) {
        String systemPrompt = """
                You are a senior retrospective facilitator.
                Combine the current sprint summary and historical patterns into a single final insight.
                Produce 3-4 lines that cover:
                - Overall sprint performance and key feedback (LIKED, LEARNED, LACKED, LONGED_FOR)
                - Action item completion and appreciation
                - Historical trend and improvement direction
                - Key people doing well (if mentioned)
                Avoid repetition and keep the response concise.
                """;

        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        messages.add(new org.springframework.ai.chat.messages.UserMessage(
                "Current Summary:\n" + currentSummary + "\n\nHistorical Summary:\n" + historicalSummary +
                        "\n\nGenerate final 2-3 line summary."));

        var response = chatClient.prompt(new org.springframework.ai.chat.prompt.Prompt(messages)).call();
        String content = response.content();
        return content == null ? "No combined analysis available" : content.trim();
    }
}
