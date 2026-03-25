package com.codmer.turepulseai.model;

import lombok.Data;

@Data
public class BusinessImageGenerateRequest {
    private String prompt;
    private Long entityId;
    private String displayName;

    // Optional: if null, service uses a safe default.
    private String size;

    /**
     * Rendering mode for text.
     * <p>
     * IMAGE_ONLY  - AI generates a complete poster image with all important text baked into the image itself.
     * TEXT_OVERLAYS - AI generates an image plus structured overlay specs for clients that render text on top.
     * <p>
     * If null or unrecognized, the service will choose a default mode (currently IMAGE_ONLY).
     */
    private String renderingMode;
}
