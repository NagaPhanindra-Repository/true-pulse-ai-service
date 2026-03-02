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
@Table(name = "jira_integrations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "jira_url"})
}, indexes = {
    @Index(name = "idx_jira_integrations_user", columnList = "user_id"),
    @Index(name = "idx_jira_integrations_active", columnList = "is_active")
})
public class JiraIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String name;

    @Column(nullable = false, length = 500)
    private String jiraUrl;

    @Column(length = 500)
    private String baseUrl;

    @Column(nullable = false, length = 255)
    private String jiraEmail;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedApiToken;

    @ElementCollection
    @CollectionTable(name = "jira_integration_project_keys", joinColumns = @JoinColumn(name = "jira_integration_id"))
    @Column(name = "project_key")
    private List<String> projectKeys = new ArrayList<>();

    @OneToMany(mappedBy = "jiraIntegration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<JiraProject> projects = new ArrayList<>();

    @Column(nullable = false)
    private Boolean isActive;

    @Column
    private LocalDateTime lastSyncAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "jiraIntegration", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<FeatureMemory> memories = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}





