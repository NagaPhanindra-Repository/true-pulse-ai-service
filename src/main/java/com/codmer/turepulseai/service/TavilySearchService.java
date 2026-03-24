package com.codmer.turepulseai.service;

import com.codmer.turepulseai.config.TavilyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TavilySearchService {

    private final TavilyProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isEnabledAndConfigured() {
        return properties.isEnabled() && StringUtils.hasText(properties.getApiKey());
    }

    public String searchAsContext(String query) {
        if (!isEnabledAndConfigured()) {
            return "";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());

            Map<String, Object> payload = Map.of(
                    "query", query,
                    "search_depth", "basic",
                    "max_results", properties.getMaxResults(),
                    "include_answer", false,
                    "include_raw_content", false
            );

            String endpoint = properties.getBaseUrl() + "/search";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "";
            }

            Object resultsObj = response.getBody().get("results");
            if (!(resultsObj instanceof List<?> results) || results.isEmpty()) {
                return "";
            }

            List<String> lines = new ArrayList<>();
            int index = 1;
            for (Object item : results) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }

                String title = stringValue(map.get("title"));
                String url = stringValue(map.get("url"));
                String content = stringValue(map.get("content"));

                if (!StringUtils.hasText(title) && !StringUtils.hasText(content)) {
                    continue;
                }

                String snippet = truncate(content, 320);
                lines.add(index + ") " + (StringUtils.hasText(title) ? title : "Result")
                        + (StringUtils.hasText(url) ? " - " + url : "")
                        + (StringUtils.hasText(snippet) ? "\n   " + snippet : ""));
                index++;
            }

            return String.join("\n", lines);
        } catch (Exception ex) {
            log.warn("Tavily search failed: {}", ex.getMessage());
            return "";
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String truncate(String value, int max) {
        if (!StringUtils.hasText(value) || value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}

