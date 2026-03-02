package com.codmer.turepulseai.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraProject {
    private String key;
    private String name;
    private String projectTypeKey;
}

