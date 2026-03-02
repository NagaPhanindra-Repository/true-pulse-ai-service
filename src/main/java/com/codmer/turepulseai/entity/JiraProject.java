package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "jira_projects", indexes = {
    @Index(name = "idx_jira_projects_integration", columnList = "jira_integration_id"),
    @Index(name = "idx_jira_projects_key", columnList = "project_key")
})
public class JiraProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jira_integration_id", nullable = false)
    private JiraIntegration jiraIntegration;

    @Column(nullable = false, length = 100)
    private String projectKey;

    @Column(nullable = false, length = 255)
    private String projectName;

    @Column(length = 100)
    private String projectTypeKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

