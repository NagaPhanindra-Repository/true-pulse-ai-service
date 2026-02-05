package com.codmer.turepulseai.model;

import lombok.Data;

@Data
public class DocumentSearchRequest {
    private Long entityId;
    private String displayName;
    private String query;
    private Integer topK;
}
