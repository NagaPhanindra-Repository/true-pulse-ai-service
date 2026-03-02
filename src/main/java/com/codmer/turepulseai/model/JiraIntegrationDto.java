package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraIntegrationDto {
    private String id;
    private String userId;
    private String name;
    private String jiraUrl;
    private String baseUrl;
    private String jiraEmail;
    private List<String> projectKeys;
    private List<JiraProject> projects;
    private Boolean isActive;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

