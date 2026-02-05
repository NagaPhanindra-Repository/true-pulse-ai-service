package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.DocumentSearchRequest;
import com.codmer.turepulseai.model.DocumentSearchResponse;
import com.codmer.turepulseai.model.DocumentUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface BusinessDocumentService {
    DocumentUploadResponse uploadDocument(MultipartFile file, Long entityId, String displayName);
    DocumentSearchResponse searchDocuments(DocumentSearchRequest request);
}
