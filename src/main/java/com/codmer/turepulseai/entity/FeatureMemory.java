package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "feature_memories", indexes = {
    @Index(name = "idx_feature_memories_user", columnList = "user_id"),
    @Index(name = "idx_feature_memories_story_key", columnList = "jira_story_key"),
    @Index(name = "idx_feature_memories_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"jira_integration_id", "jira_story_key"})
})
public class FeatureMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_integration_id")
    private JiraIntegration jiraIntegration;

    @Column(length = 100)
    private String jiraStoryId;

    @Column(nullable = false, length = 50)
    private String jiraStoryKey;

    @Column(columnDefinition = "TEXT")
    private String jiraStoryTitle;

    @Column(columnDefinition = "TEXT")
    private String jiraStoryDescription;

    @Column(length = 50)
    private String jiraStoryType;

    @Column(length = 255)
    private String jiraAssignee;

    @Column(length = 100)
    private String jiraStatus;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String initialDescription;

    @Column(nullable = false, length = 50)
    private String status = "active"; // active, completed, archived

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "featureMemory", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<MemoryDiscussion> discussions = new ArrayList<>();

    @OneToMany(mappedBy = "featureMemory", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<MemoryAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "featureMemory", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<GitBranchMapping> branches = new ArrayList<>();

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

