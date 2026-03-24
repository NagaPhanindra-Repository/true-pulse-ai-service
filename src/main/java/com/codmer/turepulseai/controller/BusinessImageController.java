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

    @PostMapping("/generate")
    public ResponseEntity<BusinessImageGenerateResponse> generate(@Valid @RequestBody BusinessImageGenerateRequest request) {
        log.info("Business image generation request received for entityId={}, displayName={}",
                request.getEntityId(), request.getDisplayName());

        BusinessImageGenerateResponse response = businessImageService.generate(request);
        return ResponseEntity.ok(response);
    }
}

