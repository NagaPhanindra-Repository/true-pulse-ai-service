package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for getting followers
 * Contains summary statistics and list of followers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowersResponse {

    /**
     * Username of the user whose followers are being retrieved
     */
    private String username;

    /**
     * Total count of followers
     */
    private Long totalFollowers;

    /**
     * List of follower details
     */
    private List<UserFollowerDto> followers;
}

