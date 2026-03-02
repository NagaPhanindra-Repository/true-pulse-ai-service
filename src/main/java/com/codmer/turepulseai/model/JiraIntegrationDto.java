package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraIntegrationDto {
    private UUID id;
    private String jiraUrl;
    private String jiraEmail;
    private String[] projectKeys;
    private Boolean isActive;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
}

