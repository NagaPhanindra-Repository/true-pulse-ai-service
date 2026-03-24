package com.codmer.turepulseai.model;

import lombok.Data;

@Data
public class BusinessImageGenerateRequest {
    private String prompt;
    private Long entityId;
    private String displayName;

    // Optional: if null, service uses a safe default.
    private String size;
}

