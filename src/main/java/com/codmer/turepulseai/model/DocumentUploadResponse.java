package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {
    private Long documentId;
    private String businessId;
    private Long entityId;
    private String displayName;
    private String title;
    private String status;
    private String message;
}
