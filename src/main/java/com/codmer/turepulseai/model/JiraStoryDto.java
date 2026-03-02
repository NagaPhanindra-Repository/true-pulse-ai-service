package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraStoryDto {
    private String id;
    private String key;
    private String summary;
    private String description;
    private String type;
    private String issueType;
    private String status;
    private String project;
    private String assignee;
    private String url;
    private List<String> labels;
    private LocalDateTime created;
    private LocalDateTime updated;
}

