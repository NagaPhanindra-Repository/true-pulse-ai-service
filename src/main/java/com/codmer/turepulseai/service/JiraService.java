package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.ConnectionTestResult;
import com.codmer.turepulseai.model.JiraProject;
import com.codmer.turepulseai.model.JiraStoryDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class JiraService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JiraService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Test connection to Jira and fetch available projects
     */
    public ConnectionTestResult testConnection(String jiraUrl, String jiraEmail, String apiToken) {
        try {
            String baseUrl = normalizeJiraUrl(jiraUrl);
            log.info("Testing Jira connection to: {}", baseUrl);
            log.info("Using email: {}", jiraEmail);

            String response = makeJiraRequest(baseUrl, "/rest/api/3/project/search", jiraEmail, apiToken, "GET");

            log.info("Received response from Jira: {}", response);
            JsonNode rootNode = objectMapper.readTree(response);
            List<JiraProject> projects = new ArrayList<>();

            if (rootNode.has("values")) {
                rootNode.get("values").forEach(project -> {
                    JiraProject jiraProject = JiraProject.builder()
                        .key(project.path("key").asText())
                        .name(project.path("name").asText())
                        .projectTypeKey(project.path("projectTypeKey").asText())
                        .build();
                    projects.add(jiraProject);
                });
            }

            log.info("Successfully connected to Jira. Found {} projects", projects.size());
            if (!projects.isEmpty()) {
                log.info("First project: {}", projects.getFirst().getKey());
            }

            return ConnectionTestResult.builder()
                .success(true)
                .availableProjects(projects)
                .build();

        } catch (Exception e) {
            log.error("Failed to test Jira connection to URL: {}, Email: {}", jiraUrl, jiraEmail, e);

            String errorMessage = e.getMessage();

            // Extract more specific error messages
            if (errorMessage != null) {
                if (errorMessage.contains("401")) {
                    errorMessage = "Authentication failed: Invalid email or API token (401 Unauthorized)";
                } else if (errorMessage.contains("403")) {
                    errorMessage = "Access forbidden: Check your Jira permissions (403 Forbidden)";
                } else if (errorMessage.contains("404")) {
                    errorMessage = "Jira instance not found: Check your Jira URL (404 Not Found)";
                } else if (errorMessage.contains("Connection refused") || errorMessage.contains("Unknown host")) {
                    errorMessage = "Cannot connect to Jira: Check your Jira URL";
                }
            }

            return ConnectionTestResult.builder()
                .success(false)
                .error(errorMessage)
                .build();
        }
    }

    /**
     * Fetch story details from Jira
     */
    public JiraStoryDto fetchStoryDetails(String jiraUrl, String jiraEmail, String apiToken, String storyKey) {
        try {
            String baseUrl = normalizeJiraUrl(jiraUrl);
            String response = makeJiraRequest(baseUrl, "/rest/api/3/issue/" + storyKey, jiraEmail, apiToken, "GET");

            JsonNode rootNode = objectMapper.readTree(response);

            // Parse dates with proper timezone handling
            LocalDateTime created = parseJiraDateTime(rootNode.path("fields").path("created").asText());
            LocalDateTime updated = parseJiraDateTime(rootNode.path("fields").path("updated").asText());

            return JiraStoryDto.builder()
                .id(rootNode.get("id").asText())
                .key(rootNode.get("key").asText())
                .summary(rootNode.path("fields").path("summary").asText())
                .description(rootNode.path("fields").path("description").path("content").elements().hasNext() ?
                    rootNode.path("fields").path("description").path("content").get(0).path("content").get(0).asText() : "")
                .issueType(rootNode.path("fields").path("issuetype").path("name").asText())
                .status(rootNode.path("fields").path("status").path("name").asText())
                .assignee(rootNode.path("fields").path("assignee").path("displayName").asText(""))
                .created(created)
                .updated(updated)
                .build();

        } catch (Exception e) {
            log.error("Failed to fetch story details from Jira", e);
            return null;
        }
    }

    /**
     * Parse Jira datetime string with timezone offset
     * Jira returns format: 2026-03-01T20:22:06.366-0600 (without colon in timezone)
     * ZonedDateTime expects: 2026-03-01T20:22:06.366-06:00 (with colon in timezone)
     */
    private LocalDateTime parseJiraDateTime(String dateTimeString) {
        try {
            // Normalize timezone format: -0600 → -06:00
            String normalized = dateTimeString.replaceAll("([+-]\\d{2})(\\d{2})$", "$1:$2");
            log.debug("Parsing Jira datetime. Original: {}, Normalized: {}", dateTimeString, normalized);

            ZonedDateTime zonedDateTime = ZonedDateTime.parse(normalized);
            return zonedDateTime.toLocalDateTime();
        } catch (Exception e) {
            log.error("Failed to parse Jira datetime: {}", dateTimeString, e);
            // Return current time as fallback
            return LocalDateTime.now();
        }
    }

    /**
     * Make HTTP request to Jira REST API with Basic Auth
     */
    private String makeJiraRequest(String baseUrl, String endpoint, String email, String apiToken, String method) throws Exception {
        String url = baseUrl + endpoint;

        log.info("Making {} request to: {}", method, url);
        log.debug("Using email: {}", email);

        String auth = email + ":" + apiToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .method(method, HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Jira API response status: {}", response.statusCode());

        if (response.statusCode() >= 400) {
            String errorBody = response.body();
            log.error("Jira API error response: {}", errorBody);
            throw new Exception("Jira API error: " + response.statusCode() + " - " + errorBody);
        }

        return response.body();
    }

    /**
     * Normalize Jira URL - extract base URL and remove query parameters
     */
    private String normalizeJiraUrl(String jiraUrl) {
        String url = jiraUrl.trim();

        // Remove query parameters and fragments (e.g., ?continue=... or #fragment)
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.contains("#")) {
            url = url.substring(0, url.indexOf("#"));
        }

        // Add https:// if no protocol specified
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        // Remove trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        log.debug("Normalized Jira URL from '{}' to '{}'", jiraUrl, url);
        return url;
    }
}

