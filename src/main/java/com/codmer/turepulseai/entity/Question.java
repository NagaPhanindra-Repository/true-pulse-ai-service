package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
@Table(name = "questions", indexes = {@Index(name = "idx_user_id", columnList = "user_id")})
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Answer> answers = new ArrayList<>();

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
        if (this.user != null && this.user.getQuestions() != null) {
            this.user.getQuestions().remove(this);
        }
        this.user = user;
        if (user != null && user.getQuestions() != null && !user.getQuestions().contains(this)) {
            user.getQuestions().add(this);
        }
    }
}
