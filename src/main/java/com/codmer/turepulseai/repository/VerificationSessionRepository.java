package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.VerificationSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationSessionRepository extends JpaRepository<VerificationSessionEntity, String> {

    /**
     * Find session by sessionId (unique identifier sent to frontend)
     */
    Optional<VerificationSessionEntity> findBySessionId(String sessionId);

    /**
     * Delete expired sessions (cleanup)
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}

