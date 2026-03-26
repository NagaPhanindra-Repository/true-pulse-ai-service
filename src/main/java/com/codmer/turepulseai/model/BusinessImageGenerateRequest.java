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
     * Rendering mode hint for clients.
     * <p>
     * The backend now always generates a clean, text-free image and returns structured overlay
     * metadata so that the frontend can render all readable text. This field is kept only as a
     * hint or for future evolution of the client experience; it no longer changes how the
     * backend constructs the image.
     */
    private String renderingMode;
}
