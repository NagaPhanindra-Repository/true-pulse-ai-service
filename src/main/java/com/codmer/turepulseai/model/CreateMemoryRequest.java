package com.codmer.turepulseai.model;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMemoryRequest {
    private String title;
    private String description;
    private UUID jiraIntegrationId;
    private String jiraStoryKey;
    private String jiraStoryUrl;
    private String project;
    private String assignee;
    private String linkedBranch;
    private String branchName;
    private String status;
}

