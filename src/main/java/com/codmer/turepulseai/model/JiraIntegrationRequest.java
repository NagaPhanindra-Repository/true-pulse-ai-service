package com.codmer.turepulseai.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraIntegrationRequest {
    private String name;
    private String jiraUrl;
    private String jiraEmail;
    private String apiToken;
    private List<String> projectKeys;
    private List<JiraProject> projects;
}




