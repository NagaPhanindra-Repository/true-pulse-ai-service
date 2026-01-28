package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "answers", indexes = {@Index(name = "idx_question_id", columnList = "question_id"), @Index(name = "idx_user_id", columnList = "user_id")})
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

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

    public void setQuestion(Question question) {
        if (this.question != null && this.question.getAnswers() != null) {
            this.question.getAnswers().remove(this);
        }
        this.question = question;
        if (question != null && question.getAnswers() != null && !question.getAnswers().contains(this)) {
            question.getAnswers().add(this);
        }
    }

    public void setUser(User user) {
        if (this.user != null && this.user.getAnswers() != null) {
            this.user.getAnswers().remove(this);
        }
        this.user = user;
        if (user != null && user.getAnswers() != null && !user.getAnswers().contains(this)) {
            user.getAnswers().add(this);
        }
    }
}

