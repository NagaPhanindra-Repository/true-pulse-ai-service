package com.codmer.turepulseai.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraIntegrationRequest {
    private String jiraUrl;
    private String jiraEmail;
    private String apiToken;
}

