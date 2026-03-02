package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "memory_discussions", indexes = {
    @Index(name = "idx_discussions_memory", columnList = "feature_memory_id"),
    @Index(name = "idx_discussions_type", columnList = "decision_type"),
    @Index(name = "idx_discussions_recorded", columnList = "recorded_at"),
    @Index(name = "idx_discussions_user", columnList = "user_id")
})
public class MemoryDiscussion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_memory_id", nullable = false)
    private FeatureMemory featureMemory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String discussionText;

    @Column(nullable = false, length = 50)
    private String decisionType; // requirement, edge-case, change, conflict, clarification

    @Column(columnDefinition = "TEXT[]")
    private String[] tags;

    @Column
    private LocalDate meetingDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "discussion", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<MemoryAttachment> attachments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

