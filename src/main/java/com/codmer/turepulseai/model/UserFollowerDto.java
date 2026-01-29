package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for UserFollower entity
 * Used for transferring follower relationship data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowerDto {

    /**
     * ID of the follower relationship
     */
    private Long id;

    /**
     * Username of the user being followed
     */
    private String userUsername;

    /**
     * Username of the follower
     */
    private String followerUsername;

    /**
     * When the follow relationship was created
     */
    private LocalDateTime createdAt;

    /**
     * Whether the follower relationship is active
     */
    private Boolean isActive;
}

