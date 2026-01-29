package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a follow relationship
 * Only requires the username to follow
 * Follower username comes from JWT token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUserRequest {

    /**
     * Username of the user to follow
     */
    private String userUsernameToFollow;
}

