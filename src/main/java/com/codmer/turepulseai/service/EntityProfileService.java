package com.codmer.turepulseai.service;

import com.codmer.turepulseai.entity.EntityType;
import com.codmer.turepulseai.model.*;

import java.util.List;

public interface EntityProfileService {
    EntityProfileDto createEntity(EntityProfileDto dto);
    EntityCreateResponse createEntityWithDetails(EntityCreateRequest request);
    EntityProfileDto getEntity(Long id);
    List<EntityProfileDto> getEntities(EntityType type);
    List<EntityProfileDto> getEntitiesByCreator(Long createdByUserId);
    EntityProfileDto updateEntity(Long id, EntityProfileDto dto);
    void deleteEntity(Long id);

    BusinessProfileDto upsertBusiness(Long entityId, BusinessProfileDto dto);
    BusinessLeaderProfileDto upsertBusinessLeader(Long entityId, BusinessLeaderProfileDto dto);
    PoliticianProfileDto upsertPolitician(Long entityId, PoliticianProfileDto dto);
    CelebrityProfileDto upsertCelebrity(Long entityId, CelebrityProfileDto dto);

    BusinessProfileDto getBusiness(Long entityId);
    BusinessLeaderProfileDto getBusinessLeader(Long entityId);
    PoliticianProfileDto getPolitician(Long entityId);
    CelebrityProfileDto getCelebrity(Long entityId);

    void deleteBusiness(Long entityId);
    void deleteBusinessLeader(Long entityId);
    void deletePolitician(Long entityId);
    void deleteCelebrity(Long entityId);

    /**
     * Get all entities created by the logged-in user
     * @param username - Username of the logged-in user
     * @return List of entities created by the user
     */
    List<EntityProfileDto> getMyEntities(String username);

    /**
     * Get all entities created by the logged-in user with full details
     * @param username - Username of the logged-in user
     * @return List of entities with complete profile information
     */
    List<EntityDetailResponse> getMyEntitiesWithDetails(String username);

    /**
     * Get random entities from the database
     * @param limit - Number of random entities to retrieve (default 10)
     * @return List of random entities
     */
    List<EntityDetailResponse> getRandomEntities(int limit);

    /**
     * Search entities by display name or full name (from profiles)
     * @param searchTerm - Search keyword
     * @return List of matching entities with full details
     */
    List<EntityDetailResponse> searchEntitiesByName(String searchTerm);
}
