package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.DocumentSearchRequest;
import com.codmer.turepulseai.model.DocumentSearchResponse;
import com.codmer.turepulseai.model.DocumentUploadResponse;
import com.codmer.turepulseai.service.BusinessDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/business-documents")
@RequiredArgsConstructor
public class BusinessDocumentController {

    private final BusinessDocumentService businessDocumentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam("entityId") Long entityId,
            @RequestParam("displayName") String displayName) {
        log.info("Uploading document for entityId: {}, displayName: {}", entityId, displayName);
        return ResponseEntity.ok(businessDocumentService.uploadDocument(file, entityId, displayName));
    }

    @PostMapping("/search")
    public ResponseEntity<DocumentSearchResponse> searchDocuments(@RequestBody DocumentSearchRequest request) {
        log.info("Searching documents for entityId: {}, displayName: {}", request.getEntityId(), request.getDisplayName());
        return ResponseEntity.ok(businessDocumentService.searchDocuments(request));
    }
}
