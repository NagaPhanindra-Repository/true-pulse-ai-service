package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResponse {
    private String businessId;
    private Long entityId;
    private String displayName;
    private String query;
    private String answer;
}
