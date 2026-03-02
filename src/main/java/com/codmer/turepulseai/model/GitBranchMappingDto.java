package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitBranchMappingDto {
    private UUID id;
    private String branchName;
    private String repositoryUrl;
    private LocalDateTime createdAt;
}

