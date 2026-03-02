package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMemoryRequest {
    private UUID jiraIntegrationId;
    private String jiraStoryKey;
    private String initialDescription;
    private String branchName;
}

