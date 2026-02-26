package com.codmer.turepulseai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory verification session store for mock verification.
 */
@Slf4j
@Service
public class VerificationSessionService {

    private final Map<String, VerificationSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UserVerificationStatus> userVerifications = new ConcurrentHashMap<>();

    public VerificationSession createSession(String requestedUserName, String requestedEmail, String countryCode) {
        String sessionId = UUID.randomUUID().toString();
        VerificationSession session = new VerificationSession();
        session.setSessionId(sessionId);
        session.setRequestedUserName(requestedUserName);
        session.setRequestedEmail(requestedEmail);
        session.setCountryCode(countryCode);
        session.setCreatedAt(LocalDateTime.now());
        session.setStatus("PENDING");
        sessions.put(sessionId, session);
        return session;
    }

    public VerificationSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public boolean isSessionVerifiedForUser(String sessionId, String userName, String email) {
        VerificationSession session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }
        if (!"VERIFIED".equalsIgnoreCase(session.getStatus())) {
            return false;
        }
        if (session.isConsumed()) {
            return false;
        }
        if (session.getRequestedUserName() != null && userName != null
                && !session.getRequestedUserName().equalsIgnoreCase(userName)) {
            return false;
        }
        if (session.getRequestedEmail() != null && email != null
                && !session.getRequestedEmail().equalsIgnoreCase(email)) {
            return false;
        }
        return true;
    }

    public void approveSession(String sessionId) {
        VerificationSession session = sessions.get(sessionId);
        if (session == null) {
            return;
        }
        session.setStatus("VERIFIED");
        session.setVerifiedAt(LocalDateTime.now());
    }

    public void consumeSession(String sessionId) {
        VerificationSession session = sessions.get(sessionId);
        if (session != null) {
            session.setConsumed(true);
        }
    }

    public UserVerificationStatus getUserStatus(String userName) {
        return userVerifications.get(userName);
    }

    public void setUserVerified(String userName, String countryCode) {
        if (userName == null) {
            return;
        }
        UserVerificationStatus status = new UserVerificationStatus();
        status.setUserName(userName);
        status.setVerified(true);
        status.setCountryCode(countryCode);
        status.setVerifiedAt(LocalDateTime.now());
        userVerifications.put(userName, status);
    }

    public void resetUserVerification(String userName) {
        if (userName != null) {
            userVerifications.remove(userName);
        }
    }

    public static class VerificationSession {
        private String sessionId;
        private String requestedUserName;
        private String requestedEmail;
        private String countryCode;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime verifiedAt;
        private boolean consumed;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getRequestedUserName() { return requestedUserName; }
        public void setRequestedUserName(String requestedUserName) { this.requestedUserName = requestedUserName; }

        public String getRequestedEmail() { return requestedEmail; }
        public void setRequestedEmail(String requestedEmail) { this.requestedEmail = requestedEmail; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

        public boolean isConsumed() { return consumed; }
        public void setConsumed(boolean consumed) { this.consumed = consumed; }
    }

    public static class UserVerificationStatus {
        private String userName;
        private boolean verified;
        private String countryCode;
        private LocalDateTime verifiedAt;

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        public LocalDateTime getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    }
}

