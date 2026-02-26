package com.codmer.turepulseai.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.codmer.turepulseai.entity.VerificationSessionEntity;
import com.codmer.turepulseai.entity.VerificationSessionEntity.VerificationStatus;
import com.codmer.turepulseai.repository.VerificationSessionRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

/**
 * VerificationSessionService
 * Manages pre-signup verification sessions for users during registration flow.
 * Uses database persistence instead of in-memory storage for production reliability.
 */
@Slf4j
@Service
@AllArgsConstructor
public class VerificationSessionService {

    private final VerificationSessionRepository verificationSessionRepository;

    /**
     * Create a new verification session
     */
    public VerificationSessionEntity createSession(String userName, String email, String countryCode) {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(1); // Session expires in 1 hour

        VerificationSessionEntity session = VerificationSessionEntity.builder()
                .sessionId(sessionId)
                .requestedUserName(userName)
                .requestedEmail(email)
                .countryCode(countryCode)
                .status(VerificationStatus.PENDING)
                .createdAt(now)
                .consumed(false)
                .expiresAt(expiresAt)
                .build();

        VerificationSessionEntity saved = verificationSessionRepository.save(session);
        log.info("Created verification session {} for user {}", sessionId, userName);
        return saved;
    }

    /**
     * Get session by sessionId
     */
    public VerificationSessionEntity getSession(String sessionId) {
        Optional<VerificationSessionEntity> session = verificationSessionRepository.findBySessionId(sessionId);
        if (session.isEmpty()) {
            log.warn("Session not found: {}", sessionId);
        }
        return session.orElse(null);
    }

    /**
     * Check if session is approved/verified
     */
    public boolean isSessionApproved(String sessionId) {
        VerificationSessionEntity session = getSession(sessionId);
        if (session == null) {
            return false;
        }
        return VerificationStatus.VERIFIED.equals(session.getStatus()) && session.isValid();
    }

    /**
     * Approve/verify a session
     */
    public VerificationSessionEntity approveSession(String sessionId) {
        VerificationSessionEntity session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        session.setStatus(VerificationStatus.VERIFIED);
        session.setVerifiedAt(LocalDateTime.now());
        VerificationSessionEntity updated = verificationSessionRepository.save(session);
        log.info("Approved verification session {} for user {}", sessionId, session.getRequestedUserName());
        return updated;
    }

    /**
     * Mark session as consumed (prevent reuse)
     */
    public VerificationSessionEntity consumeSession(String sessionId) {
        VerificationSessionEntity session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        if (!VerificationStatus.VERIFIED.equals(session.getStatus())) {
            throw new IllegalStateException("Session not verified yet: " + sessionId);
        }
        session.setConsumed(true);
        VerificationSessionEntity updated = verificationSessionRepository.save(session);
        log.info("Consumed verification session {} for user {}", sessionId, session.getRequestedUserName());
        return updated;
    }

    /**
     * Reject a session
     */
    public VerificationSessionEntity rejectSession(String sessionId, String reason) {
        VerificationSessionEntity session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        session.setStatus(VerificationStatus.REJECTED);
        VerificationSessionEntity updated = verificationSessionRepository.save(session);
        log.warn("Rejected verification session {} for user {} - Reason: {}",
                sessionId, session.getRequestedUserName(), reason);
        return updated;
    }

    /**
     * Delete session from database
     */
    public void deleteSession(String sessionId) {
        VerificationSessionEntity session = getSession(sessionId);
        if (session != null) {
            verificationSessionRepository.delete(session);
            log.info("Deleted verification session {}", sessionId);
        }
    }

    /**
     * Clean up expired sessions - can be called periodically
     */
    public void cleanupExpiredSessions() {
        verificationSessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up expired verification sessions");
    }

    /**
     * CRITICAL: Verify session is valid for signup
     * Checks: session exists, is verified, user details match, not consumed, not expired
     */
    public boolean isSessionVerifiedForUser(String sessionId, String userName, String email) {
        VerificationSessionEntity session = getSession(sessionId);
        if (session == null) {
            log.warn("Session not found for verification: {}", sessionId);
            return false;
        }

        // Check if session is verified
        if (!VerificationStatus.VERIFIED.equals(session.getStatus())) {
            log.warn("Session {} is not verified yet. Status: {}", sessionId, session.getStatus());
            return false;
        }

        // Check if session matches the requested username
        if (!session.getRequestedUserName().equalsIgnoreCase(userName)) {
            log.warn("Session {} username mismatch. Expected: {}, Got: {}",
                    sessionId, session.getRequestedUserName(), userName);
            return false;
        }

        // Check if session matches the requested email
        if (!session.getRequestedEmail().equalsIgnoreCase(email)) {
            log.warn("Session {} email mismatch. Expected: {}, Got: {}",
                    sessionId, session.getRequestedEmail(), email);
            return false;
        }

        // Check if session has already been consumed
        if (session.isConsumed()) {
            log.warn("Session {} has already been consumed", sessionId);
            return false;
        }

        // Check if session is still valid (not expired)
        if (!session.isValid()) {
            log.warn("Session {} has expired", sessionId);
            return false;
        }

        log.info("Session {} verified for user {}", sessionId, userName);
        return true;
    }

    /**
     * VerificationSession DTO - no longer used internally, kept for backwards compatibility
     */
    @Data
    @AllArgsConstructor
    public static class VerificationSession {
        private String sessionId;
        private String requestedUserName;
        private String requestedEmail;
        private String countryCode;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime verifiedAt;
        private boolean consumed;
        private String rejectionReason;

        public VerificationSession() {
            this.consumed = false;
        }

        public boolean isValid() {
            if (createdAt == null) {
                return false;
            }
            LocalDateTime expiresAt = createdAt.plusHours(1);
            return LocalDateTime.now().isBefore(expiresAt);
        }

        public boolean isReadyForSignup() {
            return "VERIFIED".equals(status) && !consumed && isValid();
        }

        @Override
        public String toString() {
            return "VerificationSession{" +
                    "sessionId='" + sessionId + '\'' +
                    ", requestedUserName='" + requestedUserName + '\'' +
                    ", requestedEmail='" + requestedEmail + '\'' +
                    ", countryCode='" + countryCode + '\'' +
                    ", status='" + status + '\'' +
                    ", consumed=" + consumed +
                    '}';
        }
    }
}
