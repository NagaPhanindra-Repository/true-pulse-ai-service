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
     * Optional: base image to refine or adjust. Expected as a data URL (e.g. "data:image/png;base64,....")
     * or raw Base64 image string. When present, regenerate API will treat this as the visual baseline
     * and adjust composition/colors according to the new prompt, still enforcing text-free output.
     */
    private String baseImage;

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
