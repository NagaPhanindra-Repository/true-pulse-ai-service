package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.service.VerificationService;
import com.codmer.turepulseai.service.VerificationSessionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MockVerificationController
 *
 * Simulates Trulioo ID verification without real API calls
 * Allows full frontend testing before getting company credentials
 *
 * Transition to production:
 *   1. Replace this controller with TruliooVerificationController
 *   2. Inject TruliooVerificationService instead of using mock logic
 *   3. Update application.yml to use real Trulioo API key
 *   4. No frontend changes needed - endpoints remain identical
 */
@Slf4j
@RestController
@RequestMapping("/api/verification")
@AllArgsConstructor
public class MockVerificationController {

    private final VerificationService verificationService;
    private final VerificationSessionService verificationSessionService;

    // In-memory store for demo (replace with database in production)
    private static final Map<String, VerificationSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, UserVerificationStatus> userVerifications = new ConcurrentHashMap<>();

    // Country code mapping - supports both phone codes (+91, +1, etc.) and ISO codes (IN, US, etc.)
    private static final Map<String, String> COUNTRY_NAMES = new HashMap<>() {{
        // Phone code mappings (primary - matches frontend)
        put("+91", "🇮🇳 India");
        put("+1", "🇺🇸 United States / Canada");
        put("+44", "🇬🇧 United Kingdom");
        put("+49", "🇩🇪 Germany");
        put("+33", "🇫🇷 France");
        put("+61", "🇦🇺 Australia");
        put("+65", "🇸🇬 Singapore");

        // ISO code mappings (fallback for backward compatibility)
        put("IN", "🇮🇳 India");
        put("US", "🇺🇸 United States");
        put("GB", "🇬🇧 United Kingdom");
        put("DE", "🇩🇪 Germany");
        put("FR", "🇫🇷 France");
        put("CA", "🇨🇦 Canada");
        put("AU", "🇦🇺 Australia");
        put("SG", "🇸🇬 Singapore");
    }};

    /**
     * Convert country code to display name with flag
     * @param countryCode Phone code (e.g., "+91", "+1") or ISO code (e.g., "IN", "US")
     * @return Display name with flag (e.g., "🇮🇳 India")
     */
    private String getCountryDisplayName(String countryCode) {
        return COUNTRY_NAMES.getOrDefault(countryCode, "🌍 " + countryCode);
    }

    // --- ENDPOINT 1: Create Verification Session ---

    /**
     * POST /api/verification/create
     *
     * Called by: Frontend when user clicks "Verify with Government ID" button
     * Expected by frontend: { verificationUrl: "...", sessionId: "..." }
     * Returns: URL that opens in popup + unique session ID for later lookup
     *
     * @param request VerificationRequest with countryCode (e.g., "IN", "US")
     * @param auth Spring Security Authentication (current logged-in user)
     * @return verificationUrl (mock page) + sessionId
     */
    @PostMapping("/create")
    public ResponseEntity<?> createVerification(
            @RequestBody VerificationRequest request,
            Authentication auth) {

        try {
            String username = auth.getName();
            String countryCode = request.getCountryCode();

            // Generate unique session ID
            String sessionId = UUID.randomUUID().toString();

            // Create session record
            VerificationSession session = new VerificationSession();
            session.setSessionId(sessionId);
            session.setUsername(username);
            session.setCountryCode(countryCode);
            session.setCreatedAt(LocalDateTime.now());
            session.setStatus("PENDING");

            sessions.put(sessionId, session);

            log.info("Created mock verification session {} for user {}", sessionId, username);

            // Return response that frontend expects
            Map<String, Object> response = new HashMap<>();
            response.put("verificationUrl", "http://localhost:8080/api/verification/mock-page/" + sessionId);
            response.put("sessionId", sessionId);
            response.put("status", "created");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to create verification", e);
            return ResponseEntity.status(400).body(
                new ErrorResponse("Failed to create verification", e.getMessage())
            );
        }
    }

    // --- ENDPOINT 2: Check Current User Verification Status (Polling) ---

    /**
     * GET /api/verification/status/current
     *
     * Called by: Frontend polling loop (every 1 second, max 2 minutes)
     * Purpose: Check if the current user has completed ID verification
     * Returns: { isVerified: true/false }
     *
     * Frontend stops polling when: isVerified = true
     *
     * @param auth Spring Security Authentication (who's logged in)
     * @return isVerified status
     */
    @GetMapping("/status/current")
    public ResponseEntity<?> checkVerificationStatus(Authentication auth) {
        try {
            String username = auth.getName();

            // Check database first
            boolean isVerifiedInDb = verificationService.isUserVerified(username);

            // Also check in-memory store for backward compatibility
            UserVerificationStatus status = userVerifications.get(username);
            boolean isVerified = isVerifiedInDb || (status != null && status.isVerified());

            log.debug("Checking verification status for {}: {}", username, isVerified);

            Map<String, Object> response = new HashMap<>();
            response.put("isVerified", isVerified);
            response.put("username", username);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to check verification status", e);
            return ResponseEntity.status(400).body(
                new ErrorResponse("Failed to check status", e.getMessage())
            );
        }
    }

    // --- ENDPOINT 3: Get Verification Result by Session ID ---

    /**
     * GET /api/verification/status/{sessionId}
     *
     * Called by: Backend webhook handler or manual result lookup
     * Purpose: Get detailed verification result for specific session
     * Returns: Full verification details (country, status, date, etc.)
     *
     * This maps to VerificationResult in real Trulioo integration
     *
     * @param sessionId Session ID from /create endpoint
     * @return Verification result details
     */
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<?> getVerificationResult(
            @PathVariable String sessionId) {

        try {
            VerificationSession session = sessions.get(sessionId);

            if (session == null) {
                return ResponseEntity.status(404).body(
                    new ErrorResponse("Session not found", "Session ID: " + sessionId)
                );
            }

            // Build result response
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", session.getSessionId());
            result.put("username", session.getUsername());
            result.put("countryCode", session.getCountryCode());
            result.put("status", session.getStatus());
            result.put("createdAt", session.getCreatedAt());
            result.put("verifiedAt", session.getVerifiedAt());

            log.info("Retrieved verification result for session {}", sessionId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to get verification result", e);
            return ResponseEntity.status(400).body(
                new ErrorResponse("Failed to get result", e.getMessage())
            );
        }
    }

    // --- ENDPOINT 4: Mock Verification Page (User-facing HTML) ---

    /**
     * GET /api/verification/mock-page/{sessionId}
     *
     * Called by: Popup window opened by frontend
     * Purpose: Show mock verification page that user can "verify" on
     *
     * This simulates the Trulioo modal that would open in real integration
     * User clicks "Verify" → marks session as VERIFIED in backend
     *
     * @param sessionId Session ID from /create endpoint
     * @return HTML page with Verify button
     */
    @GetMapping("/mock-page/{sessionId}")
    public String showMockVerificationPage(@PathVariable String sessionId) {

        VerificationSession session = sessions.get(sessionId);
        VerificationSessionService.VerificationSession preSignupSession = verificationSessionService.getSession(sessionId);

        if (session == null && preSignupSession == null) {
            return "<h1>❌ Invalid Session</h1>" +
                   "<p>Session ID not found: " + sessionId + "</p>";
        }

        String countryCode = session != null ? session.getCountryCode() : preSignupSession.getCountryCode();
        String countryDisplay = getCountryDisplayName(countryCode);
        boolean isPreSignup = session == null;

        // Read and process the HTML template file
        try {
            String htmlTemplate = readHtmlTemplate();

            // Replace placeholders with actual values
            String apiPath = isPreSignup ? "/api/verification/pre-signup/approve/" : "/api/verification/approve/";

            String processedHtml = htmlTemplate
                .replace("{{COUNTRY_NAME}}", countryDisplay)
                .replace("{{COUNTRY_CODE}}", countryCode)
                .replace("{{SESSION_ID}}", sessionId)
                .replace("{{IS_PRE_SIGNUP}}", String.valueOf(isPreSignup))
                .replace("{{API_PATH}}", apiPath + sessionId);

            return processedHtml;
        } catch (Exception e) {
            log.error("Error loading verification HTML template", e);
            return "<html><body><h1>Error loading verification page</h1><p>" + e.getMessage() + "</p></body></html>";
        }
    }

    /**
     * Reads the HTML template file from resources
     */
    private String readHtmlTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/verification-signup.html");
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // --- ENDPOINT 5: Approve Verification (Called from mock page) ---

    /**
     * POST /api/verification/approve/{sessionId}
     *
     * Called by: Mock verification page HTML button (AJAX)
     * Purpose: Mark session as VERIFIED when user clicks button
     *
     * In real Trulioo: This would be called by Trulioo webhook after real verification
     *
     * @param sessionId Session ID to approve
     * @return success response
     */
    @PostMapping("/approve/{sessionId}")
    public ResponseEntity<?> approveVerification(@PathVariable String sessionId) {
        try {
            VerificationSession session = sessions.get(sessionId);

            if (session == null) {
                return ResponseEntity.status(404).body(
                    new ErrorResponse("Session not found", sessionId)
                );
            }

            // Mark as verified
            session.setStatus("VERIFIED");
            session.setVerifiedAt(LocalDateTime.now());

            // Store user verification status in memory
            UserVerificationStatus userStatus = new UserVerificationStatus();
            userStatus.setUsername(session.getUsername());
            userStatus.setVerified(true);
            userStatus.setCountryCode(session.getCountryCode());
            userStatus.setVerifiedAt(LocalDateTime.now());

            userVerifications.put(session.getUsername(), userStatus);

            // Sync with database
            verificationService.markUserAsVerified(session.getUsername());

            log.info("User {} verified with country {}", session.getUsername(), session.getCountryCode());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "VERIFIED");
            response.put("message", "User verified successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to approve verification", e);
            return ResponseEntity.status(400).body(
                new ErrorResponse("Failed to approve", e.getMessage())
            );
        }
    }

    // --- ENDPOINT 6: Reset Verification (For testing/retry) ---

    /**
     * POST /api/verification/reset
     *
     * Called by: Frontend when user clicks "Change Country" button
     * Purpose: Clear verification status so user can try again
     *
     * @param auth Spring Security Authentication
     * @return success response
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetVerification(Authentication auth) {
        try {
            String username = auth.getName();
            userVerifications.remove(username);

            // Sync with database
            verificationService.resetUserVerification(username);

            log.info("Reset verification for user {}", username);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "reset");
            response.put("message", "Verification reset. Try again.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to reset verification", e);
            return ResponseEntity.status(400).body(
                new ErrorResponse("Failed to reset", e.getMessage())
            );
        }
    }

    // --- ENDPOINT 7: Create Pre-Signup Verification Session ---

    /**
     * POST /api/verification/pre-signup/create
     *
     * Called by: Frontend when user clicks "Verify with Government ID" button (pre-signup)
     * Expected by frontend: { verificationUrl: "...", sessionId: "..." }
     * Returns: URL that opens in popup + unique session ID for later lookup
     *
     * @param request PreSignupVerificationRequest with countryCode, userName, email
     * @return verificationUrl (mock page) + sessionId
     */
    @PostMapping("/pre-signup/create")
    public ResponseEntity<?> createPreSignupVerification(@RequestBody PreSignupVerificationRequest request) {
        try {
            VerificationSessionService.VerificationSession session = verificationSessionService.createSession(
                    request.getUserName(),
                    request.getEmail(),
                    request.getCountryCode()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("verificationUrl", "http://localhost:8080/api/verification/mock-page/" + session.getSessionId());
            response.put("sessionId", session.getSessionId());
            response.put("status", "created");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create pre-signup verification", e);
            return ResponseEntity.status(400).body(
                    new ErrorResponse("Failed to create pre-signup verification", e.getMessage())
            );
        }
    }

    // --- ENDPOINT 8: Get Pre-Signup Verification Status by Session ID ---

    /**
     * GET /api/verification/pre-signup/status/{sessionId}
     *
     * Called by: Backend webhook handler or manual result lookup (pre-signup)
     * Purpose: Get detailed verification result for specific session
     * Returns: Full verification details (country, status, date, etc.)
     *
     * This maps to VerificationResult in real Trulioo integration
     *
     * @param sessionId Session ID from /pre-signup/create endpoint
     * @return Verification result details
     */
    @GetMapping("/pre-signup/status/{sessionId}")
    public ResponseEntity<?> getPreSignupStatus(@PathVariable String sessionId) {
        try {
            VerificationSessionService.VerificationSession session = verificationSessionService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.status(404).body(
                        new ErrorResponse("Session not found", "Session ID: " + sessionId)
                );
            }

            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", session.getSessionId());
            result.put("requestedUserName", session.getRequestedUserName());
            result.put("requestedEmail", session.getRequestedEmail());
            result.put("countryCode", session.getCountryCode());
            result.put("status", session.getStatus());
            result.put("createdAt", session.getCreatedAt());
            result.put("verifiedAt", session.getVerifiedAt());
            result.put("consumed", session.isConsumed());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get pre-signup verification status", e);
            return ResponseEntity.status(400).body(
                    new ErrorResponse("Failed to get pre-signup status", e.getMessage())
            );
        }
    }

    // --- ENDPOINT 9: Approve Pre-Signup Verification (Called from mock page) ---

    /**
     * POST /api/verification/pre-signup/approve/{sessionId}
     *
     * Called by: Mock verification page HTML button (AJAX) for pre-signup
     * Purpose: Mark session as VERIFIED when user clicks button
     *
     * In real Trulioo: This would be called by Trulioo webhook after real verification
     *
     * @param sessionId Session ID to approve
     * @return success response
     */
    @PostMapping("/pre-signup/approve/{sessionId}")
    public ResponseEntity<?> approvePreSignup(@PathVariable String sessionId) {
        try {
            VerificationSessionService.VerificationSession session = verificationSessionService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.status(404).body(
                        new ErrorResponse("Session not found", sessionId)
                );
            }

            verificationSessionService.approveSession(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "VERIFIED");
            response.put("message", "Pre-signup verification approved");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to approve pre-signup verification", e);
            return ResponseEntity.status(400).body(
                    new ErrorResponse("Failed to approve pre-signup", e.getMessage())
            );
        }
    }

    // --- HELPER DTOs ---

    public static class VerificationRequest {
        private String countryCode;

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    }

    public static class PreSignupVerificationRequest {
        private String userName;
        private String email;
        private String countryCode;

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    }

    public static class VerificationSession {
        private String sessionId;
        private String username;
        private String countryCode;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime verifiedAt;

        // Getters/Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    }

    public static class UserVerificationStatus {
        private String username;
        private boolean verified;
        private String countryCode;
        private LocalDateTime verifiedAt;

        // Getters/Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        public LocalDateTime getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    }

    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() { return error; }
        public String getMessage() { return message; }
    }
}

