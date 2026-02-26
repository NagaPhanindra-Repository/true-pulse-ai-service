package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Persists verification sessions to database
 * Fixes production issue where sessions lost between servers or app restarts
 */
@Entity
@Table(name = "verification_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @Column(nullable = false)
    private String requestedUserName;

    @Column
    private String requestedEmail;

    @Column(nullable = false)
    private String countryCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VerificationStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private boolean consumed;

    @Column(nullable = false)
    private LocalDateTime expiresAt; // Sessions expire after 1 hour

    public enum VerificationStatus {
        PENDING,
        VERIFIED,
        EXPIRED,
        REJECTED
    }

    /**
     * Check if session is still valid (not expired)
     */
    public boolean isValid() {
        return LocalDateTime.now().isBefore(this.expiresAt);
    }
}

