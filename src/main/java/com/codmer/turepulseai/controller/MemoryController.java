package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.entity.FeatureMemory;
import com.codmer.turepulseai.entity.MemoryDiscussion;
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
@RequestMapping("/api/memories")
@CrossOrigin(origins = "${app.cors-origins:http://localhost:4200}")
public class MemoryController {

    private final FeatureMemoryService memoryService;
    private final JiraIntegrationService jiraIntegrationService;
    private final UserRepository userRepository;

    public MemoryController(
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
            MemoryDiscussion discussion = memoryService.addDiscussion(memoryId, user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertDiscussionToDto(discussion));
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

    private MemoryDiscussionDto convertDiscussionToDto(MemoryDiscussion discussion) {
        return MemoryDiscussionDto.builder()
            .id(discussion.getId())
            .discussionText(discussion.getDiscussionText())
            .decisionType(discussion.getDecisionType())
            .tags(discussion.getTags())
            .meetingDate(discussion.getMeetingDate())
            .recordedAt(discussion.getRecordedAt())
            .updatedAt(discussion.getUpdatedAt())
            .authorName(discussion.getUser().getFirstName() + " " + discussion.getUser().getLastName())
            .build();
    }
}

