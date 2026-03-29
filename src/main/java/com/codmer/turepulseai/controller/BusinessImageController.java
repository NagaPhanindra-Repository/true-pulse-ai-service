package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.BusinessImageGenerateRequest;
import com.codmer.turepulseai.model.BusinessImageGenerateResponse;
import com.codmer.turepulseai.service.BusinessImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/business-images")
@RequiredArgsConstructor
public class BusinessImageController {

    private final BusinessImageService businessImageService;

    /**
     * Generate a new business image based on the prompt and business documents.
     * Returns only the image and overlay data (no text embedded in image).
     *
     * @param request BusinessImageGenerateRequest with prompt, entityId, displayName, etc.
     * @return BusinessImageGenerateResponse with image and overlay.
     */
    @PostMapping("/generate")
    public ResponseEntity<BusinessImageGenerateResponse> generate(@Valid @RequestBody BusinessImageGenerateRequest request) {
        log.info("Business image generation request received for entityId={}, displayName={}",
                request.getEntityId(), request.getDisplayName());
        // Validate prompt length (example: max 4000 chars)
        if (request.getPrompt() != null && request.getPrompt().length() > 4000) {
            throw new IllegalArgumentException("Prompt too long. Maximum allowed is 4000 characters.");
        }
        BusinessImageGenerateResponse response = businessImageService.generate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Re-generate a business image using a previously generated image as the base, with new suggestions.
     * The request must include the baseImage (from previous response) and a new prompt.
     * Returns only the updated image and overlay data (no text embedded in image).
     *
     * @param request BusinessImageGenerateRequest with baseImage, prompt, entityId, displayName, etc.
     * @return BusinessImageGenerateResponse with updated image and overlay.
     */
    @PostMapping("/regenerate")
    public ResponseEntity<BusinessImageGenerateResponse> regenerate(@Valid @RequestBody BusinessImageGenerateRequest request) {
        log.info("Business image re-generation request received for entityId={}, displayName={} (hasBaseImage={})",
                request.getEntityId(), request.getDisplayName(), request.getBaseImage() != null);
        // Validate base image presence
        if (request.getBaseImage() == null || request.getBaseImage().isEmpty()) {
            throw new IllegalArgumentException("Base image is required for re-generation.");
        }
        // Validate prompt length
        if (request.getPrompt() != null && request.getPrompt().length() > 4000) {
            throw new IllegalArgumentException("Prompt too long. Maximum allowed is 4000 characters.");
        }
        BusinessImageGenerateResponse response = businessImageService.regenerate(request);
        return ResponseEntity.ok(response);
    }
}
