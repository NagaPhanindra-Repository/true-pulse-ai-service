package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing the follower relationship between users
 * A user can follow another user (like Twitter/Instagram model)
 * One user can have millions of followers
 *
 * Use userName instead of User entity reference for scalability and performance
 * This avoids loading entire User objects when dealing with millions of followers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_followers",
       indexes = {
           @Index(name = "idx_user_followers_user_username", columnList = "user_username"),
           @Index(name = "idx_user_followers_follower_username", columnList = "follower_username"),
           @Index(name = "idx_user_followers_created_at", columnList = "created_at")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_follower", columnNames = {"user_username", "follower_username"})
       })
public class UserFollower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Username of the user being followed
     * Not a foreign key for performance at scale
     */
    @Column(name = "user_username", nullable = false, length = 100)
    private String userUsername;

    /**
     * Username of the follower
     * Not a foreign key for performance at scale
     */
    @Column(name = "follower_username", nullable = false, length = 100)
    private String followerUsername;

    /**
     * When the follow relationship was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional: Track if follower is active/blocked
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        // Set createdAt on persist only
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        // Set default for isActive
        if (isActive == null) {
            isActive = true;
        }

        // Validation: A user cannot follow themselves
        if (userUsername != null && userUsername.equals(followerUsername)) {
            throw new IllegalStateException("A user cannot follow themselves");
        }
    }
}

