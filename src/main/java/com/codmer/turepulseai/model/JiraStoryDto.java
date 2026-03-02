package com.codmer.turepulseai.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraStoryDto {
    private String id;
    private String key;
    private String summary;
    private String description;
    private String issueType;
    private String status;
    private String assignee;
    private String created;
    private String updated;
}

