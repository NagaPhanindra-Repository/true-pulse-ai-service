package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
@Table(name = "feedback_points", indexes = {@Index(name = "idx_retro_id", columnList = "retro_id")})
public class FeedbackPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    // Retro reference is required: a feedback point must be attached to an existing Retro when created.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retro_id", nullable = false)
    private Retro retro;

    @OneToMany(mappedBy = "feedbackPoint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Discussion> discussions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    // manage bidirectional relationship when setting retro later
    public void setRetro(Retro retro) {
        // remove from existing retro if present
        if (this.retro != null && this.retro.getFeedbackPoints() != null) {
            this.retro.getFeedbackPoints().remove(this);
        }
        this.retro = retro;
        if (retro != null && retro.getFeedbackPoints() != null && !retro.getFeedbackPoints().contains(this)) {
            retro.getFeedbackPoints().add(this);
        }
    }

    public enum FeedbackType {
        LIKED, LEARNED, LACKED, LONGED_FOR
    }
}
