package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.FollowersResponse;
import com.codmer.turepulseai.model.UserFollowerDto;

import java.util.List;

/**
 * Service interface for managing user follower relationships
 * Handles follow/unfollow operations and follower queries
 */
public interface UserFollowerService {

    /**
     * Follow a user
     * Logged-in user follows the specified username
     *
     * @param userUsernameToFollow - Username to follow
     * @return UserFollowerDto of created relationship
     */
    UserFollowerDto followUser(String userUsernameToFollow);

    /**
     * Unfollow a user
     * Logged-in user unfollows the specified username
     *
     * @param userUsernameToUnfollow - Username to unfollow
     */
    void unfollowUser(String userUsernameToUnfollow);

    /**
     * Get all followers of logged-in user
     * Uses JWT token to identify user
     *
     * @return FollowersResponse with follower list and count
     */
    FollowersResponse getMyFollowers();

    /**
     * Get all users that logged-in user is following
     *
     * @return List of UserFollowerDto
     */
    List<UserFollowerDto> getMyFollowing();

    /**
     * Get all followers of a specific user (by username)
     *
     * @param username - Username to get followers for
     * @return FollowersResponse with follower list and count
     */
    FollowersResponse getFollowersByUsername(String username);

    /**
     * Check if logged-in user is following a specific user
     *
     * @param username - Username to check
     * @return true if following, false otherwise
     */
    boolean isFollowing(String username);

    /**
     * Get follower count for logged-in user
     *
     * @return Count of followers
     */
    long getMyFollowerCount();

    /**
     * Get following count for logged-in user
     *
     * @return Count of users being followed
     */
    long getMyFollowingCount();

    /**
     * Get all follower relationships (admin function)
     *
     * @return List of all UserFollowerDto
     */
    List<UserFollowerDto> getAllFollowerRelationships();

    /**
     * Get a specific follower relationship by ID
     *
     * @param id - ID of the relationship
     * @return UserFollowerDto
     */
    UserFollowerDto getFollowerRelationshipById(Long id);

    /**
     * Delete a follower relationship by ID (admin function)
     *
     * @param id - ID of the relationship
     */
    void deleteFollowerRelationship(Long id);
}

