package com.codmer.turepulseai.service;

import com.codmer.turepulseai.entity.JiraIntegration;
import com.codmer.turepulseai.entity.JiraProject;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.ConnectionTestResult;
import com.codmer.turepulseai.model.JiraIntegrationDto;
import com.codmer.turepulseai.model.JiraIntegrationRequest;
import com.codmer.turepulseai.model.JiraStoryDto;
import com.codmer.turepulseai.repository.JiraIntegrationRepository;
import com.codmer.turepulseai.repository.JiraProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class JiraIntegrationService {

    private final JiraIntegrationRepository jiraIntegrationRepository;
    private final JiraProjectRepository jiraProjectRepository;
    private final JiraService jiraService;
    private final EncryptionService encryptionService;

    public JiraIntegrationService(
        JiraIntegrationRepository jiraIntegrationRepository,
        JiraProjectRepository jiraProjectRepository,
        JiraService jiraService,
        EncryptionService encryptionService) {
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.jiraProjectRepository = jiraProjectRepository;
        this.jiraService = jiraService;
        this.encryptionService = encryptionService;
    }

    /**
     * Create a new Jira integration
     */
    public JiraIntegration createIntegration(User user, JiraIntegrationRequest request) {
        Optional<JiraIntegration> existing = jiraIntegrationRepository.findByUserAndJiraUrl(user, request.getJiraUrl());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Jira integration already exists for this URL");
        }

        String encryptedToken;
        try {
            encryptedToken = encryptionService.encrypt(request.getApiToken());
        } catch (Exception e) {
            log.error("Failed to encrypt API token", e);
            throw new RuntimeException("Failed to encrypt credentials", e);
        }

        // Extract base URL (without query parameters)
        String baseUrl = normalizeBaseUrl(request.getJiraUrl());

        JiraIntegration integration = JiraIntegration.builder()
            .user(user)
            .name(request.getName())
            .jiraUrl(request.getJiraUrl())
            .baseUrl(baseUrl)
            .jiraEmail(request.getJiraEmail())
            .encryptedApiToken(encryptedToken)
            .isActive(true)
            .build();

        log.info("Creating Jira integration for user: {} with Jira URL: {}", user.getId(), request.getJiraUrl());

        // Save integration first
        JiraIntegration savedIntegration = jiraIntegrationRepository.save(integration);

        // Before saving projects, get the actual projects from Jira using test-connection
        log.info("Testing connection to get actual projects from Jira");
        ConnectionTestResult testResult = jiraService.testConnection(
            request.getJiraUrl(),
            request.getJiraEmail(),
            request.getApiToken()
        );

        if (!testResult.getSuccess()) {
            log.warn("Connection test failed when creating integration: {}", testResult.getError());
            // Delete the integration we just created since connection failed
            jiraIntegrationRepository.delete(savedIntegration);
            throw new RuntimeException("Failed to verify connection with Jira: " + testResult.getError());
        }

        // Use actual projects from Jira test-connection response
        List<com.codmer.turepulseai.model.JiraProject> jiraProjects = testResult.getAvailableProjects();

        // Extract projectKeys from Jira response
        List<String> projectKeys = jiraProjects.stream()
            .map(project -> project.getKey())
            .collect(Collectors.toList());

        // Save projectKeys to integration
        savedIntegration.setProjectKeys(projectKeys);
        log.info("Found {} projects from Jira: {}", projectKeys.size(), projectKeys);

        // Save individual project details from Jira response
        if (jiraProjects != null && !jiraProjects.isEmpty()) {
            List<JiraProject> projectEntities = jiraProjects.stream()
                .map(project -> JiraProject.builder()
                    .jiraIntegration(savedIntegration)
                    .projectKey(project.getKey())
                    .projectName(project.getName())
                    .projectTypeKey(project.getProjectTypeKey())
                    .build())
                .collect(Collectors.toList());

            List<JiraProject> savedProjects = jiraProjectRepository.saveAll(projectEntities);
            savedIntegration.setProjects(savedProjects);
            log.info("Saved {} project details for integration: {}", savedProjects.size(), savedIntegration.getId());
        }

        // Save updated integration with projectKeys and projects
        return jiraIntegrationRepository.save(savedIntegration);
    }

    /**
     * Normalize base URL - extract just the domain
     */
    private String normalizeBaseUrl(String jiraUrl) {
        String url = jiraUrl.trim();
        // Remove query parameters and fragments
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.contains("#")) {
            url = url.substring(0, url.indexOf("#"));
        }
        // Remove trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Get all integrations for a user
     */
    @Transactional(readOnly = true)
    public List<JiraIntegrationDto> getUserIntegrations(User user) {
        return jiraIntegrationRepository.findByUser(user).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get active integrations for a user
     */
    @Transactional(readOnly = true)
    public List<JiraIntegrationDto> getActiveIntegrations(User user) {
        return jiraIntegrationRepository.findByUserAndIsActiveTrue(user).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific integration
     */
    @Transactional(readOnly = true)
    public JiraIntegration getIntegration(UUID integrationId, User user) {
        return jiraIntegrationRepository.findByIdAndUser(integrationId, user)
            .orElseThrow(() -> new IllegalArgumentException("Jira integration not found"));
    }

    /**
     * Test the connection to Jira
     */
    public ConnectionTestResult testConnection(UUID integrationId, User user) {
        JiraIntegration integration = getIntegration(integrationId, user);

        String decryptedToken;
        try {
            decryptedToken = encryptionService.decrypt(integration.getEncryptedApiToken());
        } catch (Exception e) {
            log.error("Failed to decrypt API token", e);
            return ConnectionTestResult.builder()
                .success(false)
                .error("Failed to decrypt credentials")
                .build();
        }

        return jiraService.testConnection(integration.getJiraUrl(), integration.getJiraEmail(), decryptedToken);
    }

    /**
     * Test Jira connection without saving (for pre-validation)
     * This method does NOT encrypt the token or save to database
     */
    public ConnectionTestResult testConnectionWithoutSaving(String jiraUrl, String jiraEmail, String apiToken) {
        log.info("Testing Jira connection without saving for URL: {}", jiraUrl);
        try {
            // Directly test connection without encryption or database save
            return jiraService.testConnection(jiraUrl, jiraEmail, apiToken);
        } catch (Exception e) {
            log.error("Error testing Jira connection", e);
            return ConnectionTestResult.builder()
                .success(false)
                .error("Connection test failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Fetch story details from Jira
     */
    @Transactional(readOnly = true)
    public JiraStoryDto fetchStory(UUID integrationId, User user, String storyKey) {
        JiraIntegration integration = getIntegration(integrationId, user);

        String decryptedToken;
        try {
            decryptedToken = encryptionService.decrypt(integration.getEncryptedApiToken());
        } catch (Exception e) {
            log.error("Failed to decrypt API token", e);
            return null;
        }

        return jiraService.fetchStoryDetails(integration.getJiraUrl(), integration.getJiraEmail(), decryptedToken, storyKey);
    }

    /**
     * Update integration
     */
    public JiraIntegration updateIntegration(UUID integrationId, User user, JiraIntegrationRequest request) {
        JiraIntegration integration = getIntegration(integrationId, user);

        integration.setJiraUrl(request.getJiraUrl());
        integration.setJiraEmail(request.getJiraEmail());

        if (request.getApiToken() != null && !request.getApiToken().isEmpty()) {
            try {
                integration.setEncryptedApiToken(encryptionService.encrypt(request.getApiToken()));
            } catch (Exception e) {
                log.error("Failed to encrypt API token", e);
                throw new RuntimeException("Failed to encrypt credentials", e);
            }
        }

        return jiraIntegrationRepository.save(integration);
    }

    /**
     * Deactivate an integration
     */
    public void deactivateIntegration(UUID integrationId, User user) {
        JiraIntegration integration = getIntegration(integrationId, user);
        integration.setIsActive(false);
        jiraIntegrationRepository.save(integration);
    }

    /**
     * Delete an integration
     */
    public void deleteIntegration(UUID integrationId, User user) {
        JiraIntegration integration = getIntegration(integrationId, user);
        jiraIntegrationRepository.delete(integration);
    }

    /**
     * Convert to DTO (excludes encrypted token)
     */
    private JiraIntegrationDto convertToDto(JiraIntegration integration) {
        // Convert project entities to DTOs using model DTO class
        List<com.codmer.turepulseai.model.JiraProject> projectDtos = integration.getProjects().stream()
            .map(projectEntity -> com.codmer.turepulseai.model.JiraProject.builder()
                .key(projectEntity.getProjectKey())
                .name(projectEntity.getProjectName())
                .projectTypeKey(projectEntity.getProjectTypeKey())
                .build())
            .collect(Collectors.toList());

        return JiraIntegrationDto.builder()
            .id(integration.getId().toString())
            .userId(integration.getUser().getId().toString())
            .name(integration.getName())
            .jiraUrl(integration.getJiraUrl())
            .baseUrl(integration.getBaseUrl())
            .jiraEmail(integration.getJiraEmail())
            .projectKeys(integration.getProjectKeys())
            .projects(projectDtos)
            .isActive(integration.getIsActive())
            .lastSyncAt(integration.getLastSyncAt())
            .createdAt(integration.getCreatedAt())
            .updatedAt(integration.getUpdatedAt())
            .build();
    }
}

