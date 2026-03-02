package com.codmer.turepulseai.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionTestResult {
    private Boolean success;
    private List<JiraProject> availableProjects;
    private String error;
}