package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.entity.JiraIntegration;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.ConnectionTestResult;
import com.codmer.turepulseai.model.JiraIntegrationDto;
import com.codmer.turepulseai.model.JiraIntegrationRequest;
import com.codmer.turepulseai.model.JiraStoryDto;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.JiraIntegrationService;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/jira")
@CrossOrigin(origins = "${app.cors-origins:http://localhost:4200}")
public class JiraIntegrationController {

    private final JiraIntegrationService jiraIntegrationService;
    private final UserRepository userRepository;

    public JiraIntegrationController(JiraIntegrationService jiraIntegrationService
    , UserRepository userRepository) {
        this.userRepository = userRepository;
        this.jiraIntegrationService = jiraIntegrationService;
    }

    /**
     * Test Jira connection without creating integration
     * This endpoint validates credentials before saving
     */
    @PostMapping("/integrations/test-connection")
    public ResponseEntity<ConnectionTestResult> testConnectionBeforeCreate(@RequestBody JiraIntegrationRequest request) {
        try {
            log.info("Testing Jira connection for URL: {}", request.getJiraUrl());

            // Validate required fields
            if (request.getJiraUrl() == null || request.getJiraUrl().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ConnectionTestResult.builder()
                        .success(false)
                        .error("Jira URL is required")
                        .build()
                );
            }

            if (request.getJiraEmail() == null || request.getJiraEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ConnectionTestResult.builder()
                        .success(false)
                        .error("Email address is required")
                        .build()
                );
            }

            if (request.getApiToken() == null || request.getApiToken().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ConnectionTestResult.builder()
                        .success(false)
                        .error("API token is required")
                        .build()
                );
            }

            // Test connection using JiraService directly (no encryption, no DB save)
            ConnectionTestResult result = jiraIntegrationService.testConnectionWithoutSaving(
                request.getJiraUrl(),
                request.getJiraEmail(),
                request.getApiToken()
            );

            if (result.getSuccess()) {
                log.info("Jira connection test successful for: {}", request.getJiraUrl());
                return ResponseEntity.ok(result);
            } else {
                log.warn("Jira connection test failed for: {}. Error: {}", request.getJiraUrl(), result.getError());
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error testing Jira connection", e);
            return ResponseEntity.badRequest().body(
                ConnectionTestResult.builder()
                    .success(false)
                    .error("Connection test failed: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Create a new Jira integration
     */
    @PostMapping("/integrations")
    public ResponseEntity<JiraIntegrationDto> createIntegration(@RequestBody JiraIntegrationRequest request) {
        try {
            User user = getAuthenticatedUser();
            JiraIntegration integration = jiraIntegrationService.createIntegration(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(integration));
        } catch (Exception e) {
            log.error("Error creating Jira integration", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all integrations for the authenticated user
     */
    @GetMapping("/integrations")
    public ResponseEntity<List<JiraIntegrationDto>> getIntegrations() {
        try {
            User user = getAuthenticatedUser();
            List<JiraIntegrationDto> integrations = jiraIntegrationService.getUserIntegrations(user);
            return ResponseEntity.ok(integrations);
        } catch (Exception e) {
            log.error("Error fetching Jira integrations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active integrations for the authenticated user
     */
    @GetMapping("/integrations/active")
    public ResponseEntity<List<JiraIntegrationDto>> getActiveIntegrations() {
        try {
            User user = getAuthenticatedUser();
            List<JiraIntegrationDto> integrations = jiraIntegrationService.getActiveIntegrations(user);
            return ResponseEntity.ok(integrations);
        } catch (Exception e) {
            log.error("Error fetching active Jira integrations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Test connection to Jira
     */
    @PostMapping("/integrations/{integrationId}/test")
    public ResponseEntity<ConnectionTestResult> testConnection(@PathVariable UUID integrationId) {
        try {
            User user = getAuthenticatedUser();
            ConnectionTestResult result = jiraIntegrationService.testConnection(integrationId, user);
            if (result.getSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error testing Jira connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Fetch a story from Jira
     */
    @GetMapping("/integrations/{integrationId}/story/{storyKey}")
    public ResponseEntity<JiraStoryDto> fetchStory(
        @PathVariable UUID integrationId,
        @PathVariable String storyKey) {
        try {
            User user = getAuthenticatedUser();
            JiraStoryDto story = jiraIntegrationService.fetchStory(integrationId, user, storyKey);
            if (story != null) {
                return ResponseEntity.ok(story);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error fetching story from Jira", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an integration
     */
    @PutMapping("/integrations/{integrationId}")
    public ResponseEntity<JiraIntegrationDto> updateIntegration(
        @PathVariable UUID integrationId,
        @RequestBody JiraIntegrationRequest request) {
        try {
            User user = getAuthenticatedUser();
            JiraIntegration integration = jiraIntegrationService.updateIntegration(integrationId, user, request);
            return ResponseEntity.ok(convertToDto(integration));
        } catch (Exception e) {
            log.error("Error updating Jira integration", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deactivate an integration
     */
    @PostMapping("/integrations/{integrationId}/deactivate")
    public ResponseEntity<Void> deactivateIntegration(@PathVariable UUID integrationId) {
        try {
            User user = getAuthenticatedUser();
            jiraIntegrationService.deactivateIntegration(integrationId, user);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deactivating Jira integration", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete an integration
     */
    @DeleteMapping("/integrations/{integrationId}")
    public ResponseEntity<Void> deleteIntegration(@PathVariable UUID integrationId) {
        try {
            User user = getAuthenticatedUser();
            jiraIntegrationService.deleteIntegration(integrationId, user);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting Jira integration", e);
            return ResponseEntity.badRequest().build();
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

    }

    private JiraIntegrationDto convertToDto(JiraIntegration integration) {
        return JiraIntegrationDto.builder()
            .id(integration.getId())
            .jiraUrl(integration.getJiraUrl())
            .jiraEmail(integration.getJiraEmail())
            .projectKeys(integration.getProjectKeys())
            .isActive(integration.getIsActive())
            .lastSyncAt(integration.getLastSyncAt())
            .createdAt(integration.getCreatedAt())
            .build();
    }
}

