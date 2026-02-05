package com.codmer.turepulseai.model;

import com.codmer.turepulseai.entity.EntityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Comprehensive entity response that includes all entity details based on entity type.
 * This is used for GET /api/entities/my-entities endpoint to return complete entity information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityDetailResponse {
    // Base entity information
    private Long id;
    private EntityType type;
    private String displayName;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Type-specific profile details
    // Only one of these will be populated based on entity type

    // BUSINESS type details
    private BusinessProfileDto businessProfile;

    // BUSINESS_LEADER type details
    private BusinessLeaderProfileDto businessLeaderProfile;

    // POLITICIAN type details
    private PoliticianProfileDto politicianProfile;

    // CELEBRITY type details
    private CelebrityProfileDto celebrityProfile;
}

