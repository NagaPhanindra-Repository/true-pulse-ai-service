package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureMemoryDetailDto {
    private UUID id;
    private String jiraStoryKey;
    private String jiraStoryTitle;
    private String jiraStoryDescription;
    private String jiraStoryType;
    private String jiraAssignee;
    private String jiraStatus;
    private String initialDescription;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private List<MemoryDiscussionDto> discussions;
    private List<GitBranchMappingDto> branches;
}

