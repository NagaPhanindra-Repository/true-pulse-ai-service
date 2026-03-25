package com.codmer.turepulseai.model;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Builder
@Data
public class BusinessImageGenerateResponse {
    private String error;
    private String displayName;
    private Long entityId;
    private String businessId;
    private String revisedPrompt;
    private String imageBase64;
    private String mimeType;
    private boolean success;

    // New: structured overlay metadata so clients can render text themselves
    private List<OverlaySpec> overlays;

    @Data
    @Builder
    public static class OverlaySpec {
        private int slot;
        private String role;      // brand, offer, headline, details, contact, etc.
        private String text;      // exact phrase, already normalized
        private String zone;      // top center, bottom bar, upper middle, bottom right, etc.
        private String size;      // small, medium, large
    }
}
