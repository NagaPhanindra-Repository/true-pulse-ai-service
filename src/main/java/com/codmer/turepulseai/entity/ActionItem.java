package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "action_items", indexes = {@Index(name = "idx_retro_id", columnList = "retro_id")})
public class ActionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean completed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionItemStatus status;

    // retro is required when creating an action item (retro already exists)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retro_id", nullable = false)
    private Retro retro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @Column
    private String assignedUserName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        completed = false;
        status = ActionItemStatus.OPEN;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // manage bidirectional retro association
    public void setRetro(Retro retro) {
        if (this.retro != null && this.retro.getActionItems() != null) {
            this.retro.getActionItems().remove(this);
        }
        this.retro = retro;
        if (retro != null && retro.getActionItems() != null && !retro.getActionItems().contains(this)) {
            retro.getActionItems().add(this);
        }
    }

    // manage bidirectional assigned user association
    public void setAssignedUser(User user) {
        if (this.assignedUser != null && this.assignedUser.getAssignedActionItems() != null) {
            this.assignedUser.getAssignedActionItems().remove(this);
        }
        this.assignedUser = user;
        if (user != null && user.getAssignedActionItems() != null && !user.getAssignedActionItems().contains(this)) {
            user.getAssignedActionItems().add(this);
        }
    }

    public enum ActionItemStatus {
        OPEN, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
