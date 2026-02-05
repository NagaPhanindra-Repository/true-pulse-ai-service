package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.entity.EntityType;
import com.codmer.turepulseai.model.*;
import com.codmer.turepulseai.service.EntityProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/entities")
@RequiredArgsConstructor
public class EntityProfileController {

    private final EntityProfileService entityProfileService;

    @PostMapping
    public ResponseEntity<EntityCreateResponse> create(@RequestBody EntityCreateRequest dto) {
        EntityCreateResponse created = entityProfileService.createEntityWithDetails(dto);
        return ResponseEntity.created(URI.create("/api/entities/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<EntityProfileDto>> list(
            @RequestParam(value = "type", required = false) EntityType type,
            @RequestParam(value = "createdByUserId", required = false) Long createdByUserId) {
        if (createdByUserId != null) {
            return ResponseEntity.ok(entityProfileService.getEntitiesByCreator(createdByUserId));
        }
        return ResponseEntity.ok(entityProfileService.getEntities(type));
    }

    @GetMapping("/my-entities")
    public ResponseEntity<List<EntityDetailResponse>> getMyEntities() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(entityProfileService.getMyEntitiesWithDetails(username));
    }

    @GetMapping("/random")
    public ResponseEntity<List<EntityDetailResponse>> getRandomEntities(
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(entityProfileService.getRandomEntities(limit));
    }

    @GetMapping("/search")
    public ResponseEntity<List<EntityDetailResponse>> searchEntities(
            @RequestParam("q") String searchTerm) {
        return ResponseEntity.ok(entityProfileService.searchEntitiesByName(searchTerm));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityProfileDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(entityProfileService.getEntity(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityProfileDto> update(@PathVariable Long id, @RequestBody EntityProfileDto dto) {
        return ResponseEntity.ok(entityProfileService.updateEntity(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entityProfileService.deleteEntity(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/business")
    public ResponseEntity<BusinessProfileDto> getBusiness(@PathVariable Long id) {
        return ResponseEntity.ok(entityProfileService.getBusiness(id));
    }

    @PutMapping("/{id}/business")
    public ResponseEntity<BusinessProfileDto> upsertBusiness(@PathVariable Long id, @RequestBody BusinessProfileDto dto) {
        return ResponseEntity.ok(entityProfileService.upsertBusiness(id, dto));
    }

    @DeleteMapping("/{id}/business")
    public ResponseEntity<Void> deleteBusiness(@PathVariable Long id) {
        entityProfileService.deleteBusiness(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/business-leader")
    public ResponseEntity<BusinessLeaderProfileDto> getBusinessLeader(@PathVariable Long id) {
        return ResponseEntity.ok(entityProfileService.getBusinessLeader(id));
    }

    @PutMapping("/{id}/business-leader")
    public ResponseEntity<BusinessLeaderProfileDto> upsertBusinessLeader(@PathVariable Long id, @RequestBody BusinessLeaderProfileDto dto) {
        return ResponseEntity.ok(entityProfileService.upsertBusinessLeader(id, dto));
    }

    @DeleteMapping("/{id}/business-leader")
    public ResponseEntity<Void> deleteBusinessLeader(@PathVariable Long id) {
        entityProfileService.deleteBusinessLeader(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/politician")
    public ResponseEntity<PoliticianProfileDto> getPolitician(@PathVariable Long id) {
        return ResponseEntity.ok(entityProfileService.getPolitician(id));
    }

    @PutMapping("/{id}/politician")
    public ResponseEntity<PoliticianProfileDto> upsertPolitician(@PathVariable Long id, @RequestBody PoliticianProfileDto dto) {
        return ResponseEntity.ok(entityProfileService.upsertPolitician(id, dto));
    }

    @DeleteMapping("/{id}/politician")
    public ResponseEntity<Void> deletePolitician(@PathVariable Long id) {
        entityProfileService.deletePolitician(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/celebrity")
    public ResponseEntity<CelebrityProfileDto> getCelebrity(@PathVariable Long id) {
        return ResponseEntity.ok(entityProfileService.getCelebrity(id));
    }

    @PutMapping("/{id}/celebrity")
    public ResponseEntity<CelebrityProfileDto> upsertCelebrity(@PathVariable Long id, @RequestBody CelebrityProfileDto dto) {
        return ResponseEntity.ok(entityProfileService.upsertCelebrity(id, dto));
    }

    @DeleteMapping("/{id}/celebrity")
    public ResponseEntity<Void> deleteCelebrity(@PathVariable Long id) {
        entityProfileService.deleteCelebrity(id);
        return ResponseEntity.noContent().build();
    }
}
