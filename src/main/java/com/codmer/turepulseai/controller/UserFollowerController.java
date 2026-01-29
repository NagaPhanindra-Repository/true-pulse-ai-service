package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.FollowUserRequest;
import com.codmer.turepulseai.model.FollowersResponse;
import com.codmer.turepulseai.model.UserFollowerDto;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.UserFollowerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for managing user follower relationships
 * Handles follow/unfollow operations and follower queries
 */
@Slf4j
@RestController
@RequestMapping("/api/followers")
@RequiredArgsConstructor
public class UserFollowerController {

    private final UserFollowerService userFollowerService;
    private final UserRepository userRepository;

    /**
     * Follow a user
     * Logged-in user follows the specified username
     *
     * POST /api/followers/follow
     * Request Body: { "userUsernameToFollow": "john_doe" }
     */
    @PostMapping("/follow")
    public ResponseEntity<UserFollowerDto> followUser(@RequestBody FollowUserRequest request) {
        log.info("Follow user request: {}", request.getUserUsernameToFollow());

        UserFollowerDto followerDto = userFollowerService.followUser(request.getUserUsernameToFollow());

        return ResponseEntity.created(URI.create("/api/followers/" + followerDto.getId()))
                .body(followerDto);
    }

    /**
     * Unfollow a user
     * Logged-in user unfollows the specified username
     *
     * DELETE /api/followers/unfollow/{username}
     */
    @DeleteMapping("/unfollow/{username}")
    public ResponseEntity<Map<String, String>> unfollowUser(@PathVariable String username) {
        log.info("Unfollow user request: {}", username);

        userFollowerService.unfollowUser(username);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully unfollowed user: " + username);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all followers of logged-in user
     * Uses JWT token from Authorization header to identify user
     * No request body, params, or path variables needed
     *
     * GET /api/followers/my-followers
     * Authorization: Bearer <JWT_TOKEN>
     */
    @GetMapping("/my-followers")
    public ResponseEntity<FollowersResponse> getMyFollowers() {
        log.info("Get my followers request");

        FollowersResponse response = userFollowerService.getMyFollowers();

        return ResponseEntity.ok(response);
    }

    /**
     * Get all users that logged-in user is following
     *
     * GET /api/followers/my-following
     * Authorization: Bearer <JWT_TOKEN>
     */
    @GetMapping("/my-following")
    public ResponseEntity<List<UserFollowerDto>> getMyFollowing() {
        log.info("Get my following request");

        List<UserFollowerDto> following = userFollowerService.getMyFollowing();

        return ResponseEntity.ok(following);
    }

    /**
     * Get all users that the logged-in user is following
     * Returns user details for each person being followed
     *
     * GET /api/followers/my-following-users
     * Authorization: Bearer <JWT_TOKEN>
     *
     * Response: List of following users with username, firstName, lastName
     */
    @GetMapping("/my-following-users")
    public ResponseEntity<List<Map<String, String>>> getMyFollowingUsers() {
        log.info("Get all users that logged-in user is following");

        List<UserFollowerDto> followingList = userFollowerService.getMyFollowing();

        // Convert to user details list
        List<Map<String, String>> followingUsers = followingList.stream()
                .map(userFollower -> {
                    // Get the user being followed from username in userFollower
                    com.codmer.turepulseai.entity.User followedUser =
                            userRepository.findByUserName(userFollower.getUserUsername())
                                    .orElse(null);

                    if (followedUser != null) {
                        Map<String, String> userDetails = new HashMap<>();
                        userDetails.put("username", followedUser.getUserName());
                        userDetails.put("firstName", followedUser.getFirstName());
                        userDetails.put("lastName", followedUser.getLastName());
                        return userDetails;
                    }
                    return null;
                })
                .filter(user -> user != null)
                .collect(Collectors.toList());

        log.info("Found {} users that logged-in user is following", followingUsers.size());

        return ResponseEntity.ok(followingUsers);
    }

    /**
     * Get all followers of a specific user (by username)
     *
     * GET /api/followers/user/{username}
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<FollowersResponse> getFollowersByUsername(@PathVariable String username) {
        log.info("Get followers for username: {}", username);

        FollowersResponse response = userFollowerService.getFollowersByUsername(username);

        return ResponseEntity.ok(response);
    }

    /**
     * Check if logged-in user is following a specific user
     *
     * GET /api/followers/is-following/{username}
     */
    @GetMapping("/is-following/{username}")
    public ResponseEntity<Map<String, Boolean>> isFollowing(@PathVariable String username) {
        log.info("Check if following: {}", username);

        boolean isFollowing = userFollowerService.isFollowing(username);

        Map<String, Boolean> response = new HashMap<>();
        response.put("isFollowing", isFollowing);

        return ResponseEntity.ok(response);
    }

    /**
     * Get follower count for logged-in user
     *
     * GET /api/followers/my-follower-count
     */
    @GetMapping("/my-follower-count")
    public ResponseEntity<Map<String, Long>> getMyFollowerCount() {
        log.info("Get my follower count");

        long count = userFollowerService.getMyFollowerCount();

        Map<String, Long> response = new HashMap<>();
        response.put("followerCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * Get following count for logged-in user
     *
     * GET /api/followers/my-following-count
     */
    @GetMapping("/my-following-count")
    public ResponseEntity<Map<String, Long>> getMyFollowingCount() {
        log.info("Get my following count");

        long count = userFollowerService.getMyFollowingCount();

        Map<String, Long> response = new HashMap<>();
        response.put("followingCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all follower relationships (admin function)
     *
     * GET /api/followers
     */
    @GetMapping
    public ResponseEntity<List<UserFollowerDto>> getAllFollowerRelationships() {
        log.info("Get all follower relationships");

        List<UserFollowerDto> relationships = userFollowerService.getAllFollowerRelationships();

        return ResponseEntity.ok(relationships);
    }

    /**
     * Get a specific follower relationship by ID
     *
     * GET /api/followers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserFollowerDto> getFollowerRelationshipById(@PathVariable Long id) {
        log.info("Get follower relationship by ID: {}", id);

        UserFollowerDto followerDto = userFollowerService.getFollowerRelationshipById(id);

        return ResponseEntity.ok(followerDto);
    }

    /**
     * Delete a follower relationship by ID (admin function)
     *
     * DELETE /api/followers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFollowerRelationship(@PathVariable Long id) {
        log.info("Delete follower relationship by ID: {}", id);

        userFollowerService.deleteFollowerRelationship(id);

        return ResponseEntity.noContent().build();
    }
}

