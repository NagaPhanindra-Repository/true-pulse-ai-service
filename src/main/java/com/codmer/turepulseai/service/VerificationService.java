package com.codmer.turepulseai.service;

import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VerificationService
 *
 * Service for managing user identity verification status
 * Integrates with User entity to persist verification status
 */
@Slf4j
@Service
@AllArgsConstructor
public class VerificationService {

    private final UserRepository userRepository;

    /**
     * Mark a user as verified in the database
     *
     * @param username Username of the user to verify
     * @return true if user was found and verified, false otherwise
     */
    @Transactional
    public boolean markUserAsVerified(String username) {
        try {
            User user = userRepository.findByUserNameOrEmail(username, username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            user.setVerified(true);
            userRepository.save(user);

            log.info("Marked user {} as verified in database", username);
            return true;

        } catch (Exception e) {
            log.error("Failed to mark user {} as verified", username, e);
            return false;
        }
    }

    /**
     * Check if a user is verified
     *
     * @param username Username to check
     * @return true if user exists and is verified, false otherwise
     */
    public boolean isUserVerified(String username) {
        try {
            User user = userRepository.findByUserNameOrEmail(username, username)
                    .orElse(null);

            return user != null && user.isVerified();

        } catch (Exception e) {
            log.error("Failed to check verification status for user {}", username, e);
            return false;
        }
    }

    /**
     * Reset verification status (for testing/retry)
     *
     * @param username Username of the user to reset
     * @return true if user was found and reset, false otherwise
     */
    @Transactional
    public boolean resetUserVerification(String username) {
        try {
            User user = userRepository.findByUserNameOrEmail(username, username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            user.setVerified(false);
            userRepository.save(user);

            log.info("Reset verification status for user {}", username);
            return true;

        } catch (Exception e) {
            log.error("Failed to reset verification for user {}", username, e);
            return false;
        }
    }
}

