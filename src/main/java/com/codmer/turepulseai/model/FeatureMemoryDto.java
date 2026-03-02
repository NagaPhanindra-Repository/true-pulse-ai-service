package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureMemoryDto {
    private UUID id;
    private Long userId;
    private UUID jiraIntegrationId;
    private String jiraStoryKey;
    private String jiraStoryTitle;
    private String jiraStoryDescription;
    private String jiraStoryType;
    private String jiraAssignee;
    private String jiraStatus;
    private String project;
    private String url;
    private List<String> labels;
    private String initialDescription;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private Long discussionCount;
}

