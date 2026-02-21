package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String emailAddress);
    Boolean existsByEmail(String emailAddress);
    Optional<User> findByUserNameOrEmail(String userName, String emailAddress);

    /**
     * Get random users excluding:
     * 1. The logged-in user
     * 2. Users that the logged-in user is already following
     *
     * Uses database-specific RANDOM() function for PostgreSQL
     *
     * @param excludeUsername - Username to exclude from results (logged-in user)
     * @param limit - Number of random users to return
     * @return List of random users not being followed
     */
    @Query(value = "SELECT u.* FROM users u " +
           "WHERE u.user_name != :excludeUsername " +
           "AND u.user_name NOT IN (" +
           "  SELECT user_username FROM user_followers " +
           "  WHERE follower_username = :excludeUsername AND is_active = true" +
           ") " +
           "ORDER BY RANDOM() LIMIT :limit",
           nativeQuery = true)
    List<User> findRandomUsers(@Param("excludeUsername") String excludeUsername, @Param("limit") int limit);

}

