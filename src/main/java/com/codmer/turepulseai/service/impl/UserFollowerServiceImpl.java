package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.entity.UserFollower;
import com.codmer.turepulseai.model.FollowersResponse;
import com.codmer.turepulseai.model.UserFollowerDto;
import com.codmer.turepulseai.repository.UserFollowerRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.UserFollowerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing user follower relationships
 * Handles business logic for follow/unfollow operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserFollowerServiceImpl implements UserFollowerService {

    private final UserFollowerRepository userFollowerRepository;
    private final UserRepository userRepository;

    @Override
    public UserFollowerDto followUser(String userUsernameToFollow) {
        log.info("Follow user request for: {}", userUsernameToFollow);

        // Validate input
        if (userUsernameToFollow == null || userUsernameToFollow.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username to follow is required");
        }

        // Get logged-in user
        String loggedInUsername = getLoggedInUsername();

        // Validate: User cannot follow themselves
        if (loggedInUsername.equals(userUsernameToFollow)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot follow yourself");
        }

        // Verify that the user to follow exists
        userRepository.findByUserName(userUsernameToFollow)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User to follow not found: " + userUsernameToFollow));

        // Check if already following
        if (userFollowerRepository.existsByUserUsernameAndFollowerUsername(userUsernameToFollow, loggedInUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already following this user");
        }

        // Create follow relationship
        UserFollower userFollower = UserFollower.builder()
                .userUsername(userUsernameToFollow)
                .followerUsername(loggedInUsername)
                .isActive(true)
                .build();

        UserFollower saved = userFollowerRepository.save(userFollower);

        log.info("User {} now follows {}", loggedInUsername, userUsernameToFollow);

        return toDto(saved);
    }

    @Override
    public void unfollowUser(String userUsernameToUnfollow) {
        log.info("Unfollow user request for: {}", userUsernameToUnfollow);

        // Validate input
        if (userUsernameToUnfollow == null || userUsernameToUnfollow.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username to unfollow is required");
        }

        // Get logged-in user
        String loggedInUsername = getLoggedInUsername();

        // Find the follow relationship
        UserFollower relationship = userFollowerRepository
                .findByUserUsernameAndFollowerUsername(userUsernameToUnfollow, loggedInUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "You are not following this user"));

        // Delete the relationship
        userFollowerRepository.delete(relationship);

        log.info("User {} unfollowed {}", loggedInUsername, userUsernameToUnfollow);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowersResponse getMyFollowers() {
        String loggedInUsername = getLoggedInUsername();

        log.info("Getting followers for user: {}", loggedInUsername);

        // Get all followers
        List<UserFollower> followers = userFollowerRepository.findActiveFollowersByUsername(loggedInUsername);

        // Convert to DTOs
        List<UserFollowerDto> followerDtos = followers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        // Get count
        long totalCount = userFollowerRepository.countByUserUsername(loggedInUsername);

        log.info("Found {} followers for user: {}", totalCount, loggedInUsername);

        return new FollowersResponse(loggedInUsername, totalCount, followerDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserFollowerDto> getMyFollowing() {
        String loggedInUsername = getLoggedInUsername();

        log.info("Getting users followed by: {}", loggedInUsername);

        // Get all users that logged-in user is following
        List<UserFollower> following = userFollowerRepository.findByFollowerUsername(loggedInUsername);

        log.info("User {} is following {} users", loggedInUsername, following.size());

        return following.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FollowersResponse getFollowersByUsername(String username) {
        log.info("Getting followers for username: {}", username);

        // Validate input
        if (username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        // Verify user exists
        userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));

        // Get all followers
        List<UserFollower> followers = userFollowerRepository.findActiveFollowersByUsername(username);

        // Convert to DTOs
        List<UserFollowerDto> followerDtos = followers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        // Get count
        long totalCount = userFollowerRepository.countByUserUsername(username);

        log.info("Found {} followers for user: {}", totalCount, username);

        return new FollowersResponse(username, totalCount, followerDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(String username) {
        String loggedInUsername = getLoggedInUsername();

        log.info("Checking if {} is following {}", loggedInUsername, username);

        return userFollowerRepository.existsByUserUsernameAndFollowerUsername(username, loggedInUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public long getMyFollowerCount() {
        String loggedInUsername = getLoggedInUsername();

        log.info("Getting follower count for: {}", loggedInUsername);

        return userFollowerRepository.countByUserUsername(loggedInUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public long getMyFollowingCount() {
        String loggedInUsername = getLoggedInUsername();

        log.info("Getting following count for: {}", loggedInUsername);

        return userFollowerRepository.countByFollowerUsername(loggedInUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserFollowerDto> getAllFollowerRelationships() {
        log.info("Getting all follower relationships");

        return userFollowerRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserFollowerDto getFollowerRelationshipById(Long id) {
        log.info("Getting follower relationship by ID: {}", id);

        UserFollower userFollower = userFollowerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Follower relationship not found"));

        return toDto(userFollower);
    }

    @Override
    public void deleteFollowerRelationship(Long id) {
        log.info("Deleting follower relationship by ID: {}", id);

        if (!userFollowerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Follower relationship not found");
        }

        userFollowerRepository.deleteById(id);

        log.info("Deleted follower relationship ID: {}", id);
    }

    /**
     * Get logged-in username from SecurityContext
     */
    private String getLoggedInUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (username == null || username.equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        // Verify user exists in database
        userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        return username;
    }

    /**
     * Convert UserFollower entity to DTO
     */
    private UserFollowerDto toDto(UserFollower userFollower) {
        return new UserFollowerDto(
                userFollower.getId(),
                userFollower.getUserUsername(),
                userFollower.getFollowerUsername(),
                userFollower.getCreatedAt(),
                userFollower.getIsActive()
        );
    }
}

