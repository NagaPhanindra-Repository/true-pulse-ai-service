package com.codmer.turepulseai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.tavily")
public class TavilyProperties {
    private boolean enabled = true;
    private String apiKey;
    private String baseUrl = "https://api.tavily.com";
    private int timeoutMs = 2000;
    private int maxResults = 5;
}

