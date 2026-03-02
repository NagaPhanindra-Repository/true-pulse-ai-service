package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "git_branch_mappings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"feature_memory_id", "branch_name"})
}, indexes = {
    @Index(name = "idx_branch_name", columnList = "branch_name"),
    @Index(name = "idx_branch_feature_memory", columnList = "feature_memory_id")
})
public class GitBranchMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_memory_id", nullable = false)
    private FeatureMemory featureMemory;

    @Column(nullable = false, length = 255)
    private String branchName;

    @Column(columnDefinition = "TEXT")
    private String repositoryUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

