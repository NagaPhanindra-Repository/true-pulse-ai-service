package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.entity.*;
import com.codmer.turepulseai.model.*;
import com.codmer.turepulseai.repository.*;
import com.codmer.turepulseai.service.EntityProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EntityProfileServiceImpl implements EntityProfileService {

    private final EntityProfileRepository entityProfileRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final BusinessLeaderProfileRepository businessLeaderProfileRepository;
    private final PoliticianProfileRepository politicianProfileRepository;
    private final CelebrityProfileRepository celebrityProfileRepository;
    private final UserRepository userRepository;

    @Override
    public EntityProfileDto createEntity(EntityProfileDto dto) {
        validateEntityDto(dto);
        User user = fetchUser(dto.getCreatedByUserId());

        EntityProfile entity = EntityProfile.builder()
                .type(dto.getType())
                .displayName(dto.getDisplayName())
                .createdByUserId(user.getId())
                .build();

        return toDto(entityProfileRepository.save(entity));
    }

    @Override
    public EntityCreateResponse createEntityWithDetails(EntityCreateRequest request) {
        validateCreateRequest(request);
        User user = fetchUser(request.getCreatedByUserId());

        EntityProfile entity = EntityProfile.builder()
                .type(request.getType())
                .displayName(request.getDisplayName())
                .createdByUserId(user.getId())
                .build();

        EntityProfile saved = entityProfileRepository.save(entity);

        EntityCreateResponse response = new EntityCreateResponse();
        response.setId(saved.getId());
        response.setType(saved.getType());
        response.setDisplayName(saved.getDisplayName());
        response.setCreatedByUserId(saved.getCreatedByUserId());
        response.setCreatedAt(saved.getCreatedAt());
        response.setUpdatedAt(saved.getUpdatedAt());

        switch (saved.getType()) {
            case BUSINESS -> response.setBusinessProfile(createBusiness(saved, request));
            case BUSINESS_LEADER -> response.setBusinessLeaderProfile(createBusinessLeader(saved, request));
            case POLITICIAN -> response.setPoliticianProfile(createPolitician(saved, request));
            case CELEBRITY -> response.setCelebrityProfile(createCelebrity(saved, request));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported entity type");
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public EntityProfileDto getEntity(Long id) {
        return entityProfileRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityProfileDto> getEntities(EntityType type) {
        if (type == null) {
            return entityProfileRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
        }
        return entityProfileRepository.findByType(type).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityProfileDto> getEntitiesByCreator(Long createdByUserId) {
        if (createdByUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdByUserId is required");
        }
        return entityProfileRepository.findByCreatedByUserId(createdByUserId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public EntityProfileDto updateEntity(Long id, EntityProfileDto dto) {
        EntityProfile entity = entityProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found"));

        if (dto.getDisplayName() != null) {
            entity.setDisplayName(dto.getDisplayName());
        }
        if (dto.getType() != null && dto.getType() != entity.getType()) {
            entity.setType(dto.getType());
        }
        return toDto(entityProfileRepository.save(entity));
    }

    @Override
    public void deleteEntity(Long id) {
        if (!entityProfileRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found");
        }
        entityProfileRepository.deleteById(id);
    }

    @Override
    public BusinessProfileDto upsertBusiness(Long entityId, BusinessProfileDto dto) {
        EntityProfile entity = requireEntity(entityId, EntityType.BUSINESS);
        BusinessProfile profile = businessProfileRepository.findById(entityId).orElseGet(BusinessProfile::new);
        profile.setEntity(entity);
        profile.setFullName(dto.getFullName());
        profile.setAddress(dto.getAddress());
        profile.setDescription(dto.getDescription());
        profile.setBusinessType(dto.getBusinessType());
        profile.setMobileNumber(dto.getMobileNumber());
        profile.setCountryCode(dto.getCountryCode());
        profile.setEmail(dto.getEmail());
        profile.setContactHours(dto.getContactHours());
        return toDto(businessProfileRepository.save(profile));
    }

    @Override
    public BusinessLeaderProfileDto upsertBusinessLeader(Long entityId, BusinessLeaderProfileDto dto) {
        EntityProfile entity = requireEntity(entityId, EntityType.BUSINESS_LEADER);
        BusinessLeaderProfile profile = businessLeaderProfileRepository.findById(entityId).orElseGet(BusinessLeaderProfile::new);
        profile.setEntity(entity);
        profile.setFullName(dto.getFullName());
        profile.setCompany(dto.getCompany());
        profile.setProjectName(dto.getProjectName());
        profile.setProjectDescription(dto.getProjectDescription());
        profile.setMobileNumber(dto.getMobileNumber());
        profile.setCountryCode(dto.getCountryCode());
        profile.setEmail(dto.getEmail());
        profile.setContactHours(dto.getContactHours());
        return toDto(businessLeaderProfileRepository.save(profile));
    }

    @Override
    public PoliticianProfileDto upsertPolitician(Long entityId, PoliticianProfileDto dto) {
        EntityProfile entity = requireEntity(entityId, EntityType.POLITICIAN);
        PoliticianProfile profile = politicianProfileRepository.findById(entityId).orElseGet(PoliticianProfile::new);
        profile.setEntity(entity);
        profile.setFullName(dto.getFullName());
        profile.setPartyName(dto.getPartyName());
        profile.setSegmentAddress(dto.getSegmentAddress());
        profile.setContestingTo(dto.getContestingTo());
        profile.setDescription(dto.getDescription());
        profile.setMobileNumber(dto.getMobileNumber());
        profile.setCountryCode(dto.getCountryCode());
        profile.setEmail(dto.getEmail());
        profile.setContactHours(dto.getContactHours());
        return toDto(politicianProfileRepository.save(profile));
    }

    @Override
    public CelebrityProfileDto upsertCelebrity(Long entityId, CelebrityProfileDto dto) {
        EntityProfile entity = requireEntity(entityId, EntityType.CELEBRITY);
        CelebrityProfile profile = celebrityProfileRepository.findById(entityId).orElseGet(CelebrityProfile::new);
        profile.setEntity(entity);
        profile.setRealName(dto.getRealName());
        profile.setArtistName(dto.getArtistName());
        profile.setArtistType(dto.getArtistType());
        profile.setDescription(dto.getDescription());
        profile.setMobileNumber(dto.getMobileNumber());
        profile.setCountryCode(dto.getCountryCode());
        profile.setEmail(dto.getEmail());
        profile.setContactHours(dto.getContactHours());
        return toDto(celebrityProfileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessProfileDto getBusiness(Long entityId) {
        BusinessProfile profile = businessProfileRepository.findById(entityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business profile not found"));
        return toDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessLeaderProfileDto getBusinessLeader(Long entityId) {
        BusinessLeaderProfile profile = businessLeaderProfileRepository.findById(entityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business leader profile not found"));
        return toDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PoliticianProfileDto getPolitician(Long entityId) {
        PoliticianProfile profile = politicianProfileRepository.findById(entityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Politician profile not found"));
        return toDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public CelebrityProfileDto getCelebrity(Long entityId) {
        CelebrityProfile profile = celebrityProfileRepository.findById(entityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Celebrity profile not found"));
        return toDto(profile);
    }

    @Override
    public void deleteBusiness(Long entityId) {
        if (!businessProfileRepository.existsById(entityId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business profile not found");
        }
        businessProfileRepository.deleteById(entityId);
    }

    @Override
    public void deleteBusinessLeader(Long entityId) {
        if (!businessLeaderProfileRepository.existsById(entityId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business leader profile not found");
        }
        businessLeaderProfileRepository.deleteById(entityId);
    }

    @Override
    public void deletePolitician(Long entityId) {
        if (!politicianProfileRepository.existsById(entityId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Politician profile not found");
        }
        politicianProfileRepository.deleteById(entityId);
    }

    @Override
    public void deleteCelebrity(Long entityId) {
        if (!celebrityProfileRepository.existsById(entityId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Celebrity profile not found");
        }
        celebrityProfileRepository.deleteById(entityId);
    }

    @Override
    public List<EntityProfileDto> getMyEntities(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return getEntitiesByCreator(user.getId());
    }

    @Override
    public List<EntityDetailResponse> getMyEntitiesWithDetails(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Get all entities created by this user
        List<EntityProfile> entities = entityProfileRepository.findByCreatedByUserId(user.getId());

        // Convert to detailed response DTOs
        return entities.stream()
                .map(this::toDetailedResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityDetailResponse> getRandomEntities(int limit) {
        if (limit <= 0) {
            limit = 10; // Default to 10
        }
        if (limit > 50) {
            limit = 50; // Max limit of 50 to prevent performance issues
        }

        List<EntityProfile> entities = entityProfileRepository.findRandomEntities(limit);

        return entities.stream()
                .map(this::toDetailedResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityDetailResponse> searchEntitiesByName(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search term is required");
        }

        String trimmedSearchTerm = searchTerm.trim();

        // Search in entity display names
        List<EntityProfile> entitiesByDisplayName = entityProfileRepository.searchByDisplayName(trimmedSearchTerm);

        // Search in profile full names and collect entity IDs
        Set<Long> matchingEntityIds = new HashSet<>();

        // Search in business profiles
        List<BusinessProfile> businessMatches = businessProfileRepository.searchByFullName(trimmedSearchTerm);
        businessMatches.forEach(b -> matchingEntityIds.add(b.getId()));

        // Search in business leader profiles
        List<BusinessLeaderProfile> leaderMatches = businessLeaderProfileRepository.searchByFullName(trimmedSearchTerm);
        leaderMatches.forEach(l -> matchingEntityIds.add(l.getId()));

        // Search in politician profiles
        List<PoliticianProfile> politicianMatches = politicianProfileRepository.searchByFullName(trimmedSearchTerm);
        politicianMatches.forEach(p -> matchingEntityIds.add(p.getId()));

        // Search in celebrity profiles
        List<CelebrityProfile> celebrityMatches = celebrityProfileRepository.searchByName(trimmedSearchTerm);
        celebrityMatches.forEach(c -> matchingEntityIds.add(c.getId()));

        // Combine results from display name search and profile searches
        Set<Long> allMatchingIds = new HashSet<>();
        entitiesByDisplayName.forEach(e -> allMatchingIds.add(e.getId()));
        allMatchingIds.addAll(matchingEntityIds);

        // Fetch all matching entities
        List<EntityProfile> matchingEntities = new ArrayList<>();
        for (Long entityId : allMatchingIds) {
            entityProfileRepository.findById(entityId).ifPresent(matchingEntities::add);
        }

        // Convert to detailed responses
        return matchingEntities.stream()
                .map(this::toDetailedResponse)
                .collect(Collectors.toList());
    }

    private EntityProfile requireEntity(Long entityId, EntityType expectedType) {
        EntityProfile entity = entityProfileRepository.findById(entityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found"));
        if (entity.getType() != expectedType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entity type mismatch");
        }
        return entity;
    }

    private void validateEntityDto(EntityProfileDto dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entity payload is required");
        }
        if (dto.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
        }
        if (dto.getDisplayName() == null || dto.getDisplayName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }
        if (dto.getCreatedByUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdByUserId is required");
        }
    }

    private void validateCreateRequest(EntityCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entity payload is required");
        }
        if (request.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
        }
        if (request.getDisplayName() == null || request.getDisplayName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }
        if (request.getCreatedByUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdByUserId is required");
        }

        switch (request.getType()) {
            case BUSINESS -> validateBusinessRequest(request);
            case BUSINESS_LEADER -> validateBusinessLeaderRequest(request);
            case POLITICIAN -> validatePoliticianRequest(request);
            case CELEBRITY -> validateCelebrityRequest(request);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported entity type");
        }
    }

    private void validateBusinessRequest(EntityCreateRequest request) {
        require(request.getBusinessFullName(), "businessFullName is required");
        require(request.getBusinessAddress(), "businessAddress is required");
        require(request.getBusinessType(), "businessType is required");
        require(request.getBusinessMobileNumber(), "businessMobileNumber is required");
        require(request.getBusinessCountryCode(), "businessCountryCode is required");
        require(request.getBusinessEmail(), "businessEmail is required");
        require(request.getBusinessContactHours(), "businessContactHours is required");
    }

    private void validateBusinessLeaderRequest(EntityCreateRequest request) {
        require(request.getLeaderFullName(), "leaderFullName is required");
        require(request.getLeaderCompany(), "leaderCompany is required");
        require(request.getLeaderProjectName(), "leaderProjectName is required");
        require(request.getLeaderMobileNumber(), "leaderMobileNumber is required");
        require(request.getLeaderCountryCode(), "leaderCountryCode is required");
        require(request.getLeaderEmail(), "leaderEmail is required");
        require(request.getLeaderContactHours(), "leaderContactHours is required");
    }

    private void validatePoliticianRequest(EntityCreateRequest request) {
        require(request.getPoliticianFullName(), "politicianFullName is required");
        require(request.getPoliticianPartyName(), "politicianPartyName is required");
        require(request.getPoliticianSegmentAddress(), "politicianSegmentAddress is required");
        require(request.getPoliticianContestingTo(), "politicianContestingTo is required");
        require(request.getPoliticianMobileNumber(), "politicianMobileNumber is required");
        require(request.getPoliticianCountryCode(), "politicianCountryCode is required");
        require(request.getPoliticianEmail(), "politicianEmail is required");
        require(request.getPoliticianContactHours(), "politicianContactHours is required");
    }

    private void validateCelebrityRequest(EntityCreateRequest request) {
        require(request.getCelebrityRealName(), "celebrityRealName is required");
        require(request.getCelebrityArtistName(), "celebrityArtistName is required");
        require(request.getCelebrityArtistType(), "celebrityArtistType is required");
        require(request.getCelebrityMobileNumber(), "celebrityMobileNumber is required");
        require(request.getCelebrityCountryCode(), "celebrityCountryCode is required");
        require(request.getCelebrityEmail(), "celebrityEmail is required");
        require(request.getCelebrityContactHours(), "celebrityContactHours is required");
    }

    private void require(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private BusinessProfileDto createBusiness(EntityProfile entity, EntityCreateRequest request) {
        BusinessProfile profile = new BusinessProfile();
        profile.setEntity(entity);
        profile.setFullName(request.getBusinessFullName());
        profile.setAddress(request.getBusinessAddress());
        profile.setDescription(request.getBusinessDescription());
        profile.setBusinessType(request.getBusinessType());
        profile.setMobileNumber(request.getBusinessMobileNumber());
        profile.setCountryCode(request.getBusinessCountryCode());
        profile.setEmail(request.getBusinessEmail());
        profile.setContactHours(request.getBusinessContactHours());
        return toDto(businessProfileRepository.save(profile));
    }

    private BusinessLeaderProfileDto createBusinessLeader(EntityProfile entity, EntityCreateRequest request) {
        BusinessLeaderProfile profile = new BusinessLeaderProfile();
        profile.setEntity(entity);
        profile.setFullName(request.getLeaderFullName());
        profile.setCompany(request.getLeaderCompany());
        profile.setProjectName(request.getLeaderProjectName());
        profile.setProjectDescription(request.getLeaderProjectDescription());
        profile.setMobileNumber(request.getLeaderMobileNumber());
        profile.setCountryCode(request.getLeaderCountryCode());
        profile.setEmail(request.getLeaderEmail());
        profile.setContactHours(request.getLeaderContactHours());
        return toDto(businessLeaderProfileRepository.save(profile));
    }

    private PoliticianProfileDto createPolitician(EntityProfile entity, EntityCreateRequest request) {
        PoliticianProfile profile = new PoliticianProfile();
        profile.setEntity(entity);
        profile.setFullName(request.getPoliticianFullName());
        profile.setPartyName(request.getPoliticianPartyName());
        profile.setSegmentAddress(request.getPoliticianSegmentAddress());
        profile.setContestingTo(request.getPoliticianContestingTo());
        profile.setDescription(request.getPoliticianDescription());
        profile.setMobileNumber(request.getPoliticianMobileNumber());
        profile.setCountryCode(request.getPoliticianCountryCode());
        profile.setEmail(request.getPoliticianEmail());
        profile.setContactHours(request.getPoliticianContactHours());
        return toDto(politicianProfileRepository.save(profile));
    }

    private CelebrityProfileDto createCelebrity(EntityProfile entity, EntityCreateRequest request) {
        CelebrityProfile profile = new CelebrityProfile();
        profile.setEntity(entity);
        profile.setRealName(request.getCelebrityRealName());
        profile.setArtistName(request.getCelebrityArtistName());
        profile.setArtistType(request.getCelebrityArtistType());
        profile.setDescription(request.getCelebrityDescription());
        profile.setMobileNumber(request.getCelebrityMobileNumber());
        profile.setCountryCode(request.getCelebrityCountryCode());
        profile.setEmail(request.getCelebrityEmail());
        profile.setContactHours(request.getCelebrityContactHours());
        return toDto(celebrityProfileRepository.save(profile));
    }

    private User fetchUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
    }

    private EntityProfileDto toDto(EntityProfile entity) {
        return new EntityProfileDto(
                entity.getId(),
                entity.getType(),
                entity.getDisplayName(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BusinessProfileDto toDto(BusinessProfile profile) {
        return new BusinessProfileDto(profile.getId(), profile.getFullName(), profile.getAddress(),
                profile.getDescription(), profile.getBusinessType(), profile.getMobileNumber(),
                profile.getCountryCode(), profile.getEmail(), profile.getContactHours());
    }

    private BusinessLeaderProfileDto toDto(BusinessLeaderProfile profile) {
        return new BusinessLeaderProfileDto(profile.getId(), profile.getFullName(), profile.getCompany(),
                profile.getProjectName(), profile.getProjectDescription(), profile.getMobileNumber(),
                profile.getCountryCode(), profile.getEmail(), profile.getContactHours());
    }

    private PoliticianProfileDto toDto(PoliticianProfile profile) {
        return new PoliticianProfileDto(profile.getId(), profile.getFullName(), profile.getPartyName(),
                profile.getSegmentAddress(), profile.getContestingTo(), profile.getDescription(),
                profile.getMobileNumber(), profile.getCountryCode(), profile.getEmail(), profile.getContactHours());
    }

    private CelebrityProfileDto toDto(CelebrityProfile profile) {
        return new CelebrityProfileDto(profile.getId(), profile.getRealName(), profile.getArtistName(),
                profile.getArtistType(), profile.getDescription(), profile.getMobileNumber(),
                profile.getCountryCode(), profile.getEmail(), profile.getContactHours());
    }

    private EntityDetailResponse toDetailedResponse(EntityProfile entity) {
        EntityDetailResponse response = new EntityDetailResponse();
        response.setId(entity.getId());
        response.setType(entity.getType());
        response.setDisplayName(entity.getDisplayName());
        response.setCreatedByUserId(entity.getCreatedByUserId());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        // Load the appropriate profile based on entity type
        switch (entity.getType()) {
            case BUSINESS -> {
                var profile = businessProfileRepository.findById(entity.getId());
                profile.ifPresent(p -> response.setBusinessProfile(toDto(p)));
            }
            case BUSINESS_LEADER -> {
                var profile = businessLeaderProfileRepository.findById(entity.getId());
                profile.ifPresent(p -> response.setBusinessLeaderProfile(toDto(p)));
            }
            case POLITICIAN -> {
                var profile = politicianProfileRepository.findById(entity.getId());
                profile.ifPresent(p -> response.setPoliticianProfile(toDto(p)));
            }
            case CELEBRITY -> {
                var profile = celebrityProfileRepository.findById(entity.getId());
                profile.ifPresent(p -> response.setCelebrityProfile(toDto(p)));
            }
        }

        return response;
    }
}
