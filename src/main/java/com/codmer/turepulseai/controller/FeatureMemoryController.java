package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.entity.FeatureMemory;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.*;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.FeatureMemoryService;
import com.codmer.turepulseai.service.JiraIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/feature-memories")
@CrossOrigin(origins = "${app.cors-origins:http://localhost:4200}")
public class FeatureMemoryController {

    private final FeatureMemoryService memoryService;
    private final JiraIntegrationService jiraIntegrationService;
    private final UserRepository userRepository;

    public FeatureMemoryController(
        FeatureMemoryService memoryService,
        JiraIntegrationService jiraIntegrationService,
        UserRepository userRepository) {
        this.userRepository = userRepository;
        this.memoryService = memoryService;
        this.jiraIntegrationService = jiraIntegrationService;
    }

    /**
     * Create a new feature memory
     */
    @PostMapping
    public ResponseEntity<FeatureMemoryDto> createMemory(@RequestBody CreateMemoryRequest request) {
        try {
            User user = getAuthenticatedUser();

            JiraStoryDto jiraStory = null;
            if (request.getJiraIntegrationId() != null) {
                jiraStory = jiraIntegrationService.fetchStory(
                    request.getJiraIntegrationId(),
                    user,
                    request.getJiraStoryKey()
                );
            }

            FeatureMemory memory = memoryService.createMemory(request, user, jiraStory);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(memory));
        } catch (Exception e) {
            log.error("Error creating feature memory", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get paginated memories for the authenticated user
     */
    @GetMapping
    public ResponseEntity<Page<FeatureMemoryDto>> getMemories(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search,
        Pageable pageable) {
        try {
            User user = getAuthenticatedUser();
            Page<FeatureMemoryDto> memories = memoryService.getUserMemories(user, status, search, pageable);
            return ResponseEntity.ok(memories);
        } catch (Exception e) {
            log.error("Error fetching feature memories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get memory details with discussions and branches
     */
    @GetMapping("/{memoryId}")
    public ResponseEntity<FeatureMemoryDetailDto> getMemory(@PathVariable UUID memoryId) {
        try {
            User user = getAuthenticatedUser();
            FeatureMemoryDetailDto memory = memoryService.getMemoryDetail(memoryId, user);
            return ResponseEntity.ok(memory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching feature memory", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update a feature memory
     */
    @PutMapping("/{memoryId}")
    public ResponseEntity<FeatureMemoryDto> updateMemory(
        @PathVariable UUID memoryId,
        @RequestBody FeatureMemory updates) {
        try {
            User user = getAuthenticatedUser();
            FeatureMemory memory = memoryService.updateMemory(memoryId, user, updates);
            return ResponseEntity.ok(convertToDto(memory));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating feature memory", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Complete a feature memory
     */
    @PostMapping("/{memoryId}/complete")
    public ResponseEntity<Void> completeMemory(@PathVariable UUID memoryId) {
        try {
            User user = getAuthenticatedUser();
            memoryService.completeMemory(memoryId, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error completing feature memory", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Archive a feature memory
     */
    @PostMapping("/{memoryId}/archive")
    public ResponseEntity<Void> archiveMemory(@PathVariable UUID memoryId) {
        try {
            User user = getAuthenticatedUser();
            memoryService.archiveMemory(memoryId, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error archiving feature memory", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a feature memory
     */
    @DeleteMapping("/{memoryId}")
    public ResponseEntity<Void> deleteMemory(@PathVariable UUID memoryId) {
        try {
            User user = getAuthenticatedUser();
            memoryService.deleteMemory(memoryId, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting feature memory", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Add a discussion note
     */
    @PostMapping("/{memoryId}/discussions")
    public ResponseEntity<MemoryDiscussionDto> addDiscussion(
        @PathVariable UUID memoryId,
        @RequestBody AddDiscussionRequest request) {
        try {
            User user = getAuthenticatedUser();
            memoryService.addDiscussion(memoryId, user, request);

            // Fetch updated discussions to return latest
            List<MemoryDiscussionDto> discussions = memoryService.getDiscussions(memoryId, user, null);
            if (!discussions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(discussions.get(discussions.size() - 1));
            }
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error adding discussion", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get discussions for a memory
     */
    @GetMapping("/{memoryId}/discussions")
    public ResponseEntity<List<MemoryDiscussionDto>> getDiscussions(
        @PathVariable UUID memoryId,
        @RequestParam(required = false) String type) {
        try {
            User user = getAuthenticatedUser();
            List<MemoryDiscussionDto> discussions = memoryService.getDiscussions(memoryId, user, type);
            return ResponseEntity.ok(discussions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching discussions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get discussions by type (requirements only)
     */
    @GetMapping("/{memoryId}/discussions/requirements")
    public ResponseEntity<List<MemoryDiscussionDto>> getRequirements(@PathVariable UUID memoryId) {
        try {
            User user = getAuthenticatedUser();
            List<MemoryDiscussionDto> requirements = memoryService.getDiscussions(memoryId, user, "requirement");
            return ResponseEntity.ok(requirements);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching requirements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get discussions by type (edge cases only)
     */
    @GetMapping("/{memoryId}/discussions/edge-cases")
    public ResponseEntity<List<MemoryDiscussionDto>> getEdgeCases(@PathVariable UUID memoryId) {
        try {
            User user = getAuthenticatedUser();
            List<MemoryDiscussionDto> edgeCases = memoryService.getDiscussions(memoryId, user, "edge-case");
            return ResponseEntity.ok(edgeCases);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching edge cases", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get discussions by type (decisions/changes only)
     */
    @GetMapping("/{memoryId}/discussions/decisions")
    public ResponseEntity<List<MemoryDiscussionDto>> getDecisions(@PathVariable UUID memoryId) {
        try {
            User user = getAuthenticatedUser();
            List<MemoryDiscussionDto> decisions = memoryService.getDiscussions(memoryId, user, "change");
            return ResponseEntity.ok(decisions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching decisions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

    }

    private FeatureMemoryDto convertToDto(FeatureMemory memory) {
        return FeatureMemoryDto.builder()
            .id(memory.getId())
            .jiraStoryKey(memory.getJiraStoryKey())
            .jiraStoryTitle(memory.getJiraStoryTitle())
            .jiraStoryDescription(memory.getJiraStoryDescription())
            .status(memory.getStatus())
            .createdAt(memory.getCreatedAt())
            .updatedAt(memory.getUpdatedAt())
            .build();
    }
}

