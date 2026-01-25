package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "discussions", indexes = {@Index(name = "idx_feedback_point_id", columnList = "feedback_point_id")})
public class Discussion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_point_id", nullable = false)
    private FeedbackPoint feedbackPoint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    public void setUser(User user) {
        if (this.user != null && this.user.getDiscussions() != null) {
            this.user.getDiscussions().remove(this);
        }
        this.user = user;
        if (user != null && user.getDiscussions() != null && !user.getDiscussions().contains(this)) {
            user.getDiscussions().add(this);
        }
    }

    public void setFeedbackPoint(FeedbackPoint feedbackPoint) {
        if (this.feedbackPoint != null && this.feedbackPoint.getDiscussions() != null) {
            this.feedbackPoint.getDiscussions().remove(this);
        }
        this.feedbackPoint = feedbackPoint;
        if (feedbackPoint != null && feedbackPoint.getDiscussions() != null && !feedbackPoint.getDiscussions().contains(this)) {
            feedbackPoint.getDiscussions().add(this);
        }
    }
}
