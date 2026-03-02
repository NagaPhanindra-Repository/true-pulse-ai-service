package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureMemoryDto {
    private UUID id;
    private Long userId;
    private String title;
    private String description;
    private String jiraStoryKey;
    private String jiraStoryUrl;
    private String jiraStoryTitle;
    private String jiraStoryDescription;
    private String project;
    private String assignee;
    private String linkedBranch;
    private String branchName;
    private String status;
    private Long discussionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

