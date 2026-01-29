package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.UserFollower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserFollower entity
 * Handles queries for follower relationships at scale
 */
@Repository
public interface UserFollowerRepository extends JpaRepository<UserFollower, Long> {

    /**
     * Find all followers of a specific user
     * Returns all users who are following the given username
     */
    List<UserFollower> findByUserUsername(String userUsername);

    /**
     * Find all users that a specific user is following
     */
    List<UserFollower> findByFollowerUsername(String followerUsername);

    /**
     * Check if a follower relationship exists
     */
    Optional<UserFollower> findByUserUsernameAndFollowerUsername(String userUsername, String followerUsername);

    /**
     * Check if user A follows user B
     */
    boolean existsByUserUsernameAndFollowerUsername(String userUsername, String followerUsername);

    /**
     * Count total followers for a user
     */
    long countByUserUsername(String userUsername);

    /**
     * Count total users that a user is following
     */
    long countByFollowerUsername(String followerUsername);

    /**
     * Get active followers only
     */
    List<UserFollower> findByUserUsernameAndIsActive(String userUsername, Boolean isActive);

    /**
     * Delete a specific follow relationship
     */
    void deleteByUserUsernameAndFollowerUsername(String userUsername, String followerUsername);

    /**
     * Custom query to get follower details with pagination support
     * Can be extended with Pageable for millions of followers
     */
    @Query("SELECT uf FROM UserFollower uf WHERE uf.userUsername = :username AND uf.isActive = true ORDER BY uf.createdAt DESC")
    List<UserFollower> findActiveFollowersByUsername(@Param("username") String username);
}

