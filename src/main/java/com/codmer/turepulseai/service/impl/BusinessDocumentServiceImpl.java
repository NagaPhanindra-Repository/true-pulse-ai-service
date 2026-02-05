package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.entity.BusinessDocument;
import com.codmer.turepulseai.entity.BusinessDocumentChunk;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.DocumentSearchRequest;
import com.codmer.turepulseai.model.DocumentSearchResponse;
import com.codmer.turepulseai.model.DocumentUploadResponse;
import com.codmer.turepulseai.repository.BusinessDocumentChunkRepository;
import com.codmer.turepulseai.repository.BusinessDocumentRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.BusinessDocumentService;
import com.codmer.turepulseai.util.DocumentChunker;
import com.codmer.turepulseai.util.DocumentTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BusinessDocumentServiceImpl implements BusinessDocumentService {

    private static final int DEFAULT_TOP_K = 5;

    private final BusinessDocumentRepository businessDocumentRepository;
    private final BusinessDocumentChunkRepository businessDocumentChunkRepository;
    private final UserRepository userRepository;
    private final DocumentTextExtractor documentTextExtractor;
    private final DocumentChunker documentChunker;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingService embeddingService;

    @Override
    public DocumentUploadResponse uploadDocument(MultipartFile file, Long entityId, String displayName) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "document file is required");
        }
        if (entityId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityId is required");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }

        User user = fetchUser();

        // Use username as businessId
        String businessId = user.getUserName();

        BusinessDocument document = new BusinessDocument();
        document.setBusinessId(businessId);
        document.setEntityId(entityId);
        document.setDisplayName(displayName.trim());
        document.setUserId(user.getId());
        document.setTitle(file.getOriginalFilename());
        document.setFileName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStatus("PROCESSING");

        BusinessDocument saved = businessDocumentRepository.save(document);

        String text = documentTextExtractor.extractText(file);
        List<DocumentChunker.Chunk> chunks = documentChunker.chunk(text);

        if (chunks.isEmpty()) {
            saved.setStatus("EMPTY");
            businessDocumentRepository.save(saved);
            return DocumentUploadResponse.builder()
                    .documentId(saved.getId())
                    .businessId(saved.getBusinessId())
                    .entityId(saved.getEntityId())
                    .displayName(saved.getDisplayName())
                    .title(saved.getTitle())
                    .status(saved.getStatus())
                    .message("No text content found in document")
                    .build();
        }

        int dimension = 1536; // Default dimension for OpenAI embeddings, fallback to this

        try {
            dimension = embeddingModel.dimensions();
        } catch (Exception e) {
            log.warn("Could not determine embedding dimensions. Using default dimension: " + dimension, e);
        }

        final int finalDimension = dimension;
        for (DocumentChunker.Chunk chunk : chunks) {
            BusinessDocumentChunk entity = new BusinessDocumentChunk();
            entity.setDocumentId(saved.getId());
            entity.setBusinessId(saved.getBusinessId());
            entity.setEntityId(saved.getEntityId());
            entity.setDisplayName(saved.getDisplayName());
            entity.setChunkIndex(chunk.getIndex());
            entity.setContent(chunk.getContent());
            entity.setPrevContent(chunk.getPrevContent());
            entity.setNextContent(chunk.getNextContent());
            entity.setEmbeddingDimension(finalDimension);

            // Use EmbeddingService to generate embedding and store chunk
            try {
                embeddingService.embedAndStoreChunk(entity);
            } catch (Exception e) {
                log.warn("Failed to generate embedding for chunk " + chunk.getIndex() +
                        ". Check your OpenAI API quota and key. Saving chunk without embedding...", e);
                // Save chunk without embedding
                businessDocumentChunkRepository.save(entity);
            }
        }

        saved.setStatus("READY");
        businessDocumentRepository.save(saved);

        return DocumentUploadResponse.builder()
                .documentId(saved.getId())
                .businessId(saved.getBusinessId())
                .entityId(saved.getEntityId())
                .displayName(saved.getDisplayName())
                .title(saved.getTitle())
                .status(saved.getStatus())
                .message("Document processed and indexed")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentSearchResponse searchDocuments(DocumentSearchRequest request) {
        if (request == null || request.getEntityId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityId is required");
        }
        if (request.getDisplayName() == null || request.getDisplayName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }

        User user = fetchUser();
        String businessId = user.getUserName();

        int topK = request.getTopK() != null && request.getTopK() > 0 ? request.getTopK() : DEFAULT_TOP_K;
        float[] embedding = embeddingModel.embed(request.getQuery());
        String embeddingLiteral = formatEmbedding(embedding);

        List<Object[]> rows = businessDocumentChunkRepository.findSimilarChunksByEntityAndBusiness(
                businessId, request.getEntityId(), request.getDisplayName().trim(), embeddingLiteral, topK);

        List<DocumentSearchResponse.MatchedChunk> matches = new ArrayList<>();
        for (Object[] row : rows) {
            DocumentSearchResponse.MatchedChunk match = DocumentSearchResponse.MatchedChunk.builder()
                    .documentId(((Number) row[1]).longValue())
                    .chunkIndex(((Number) row[3]).intValue())
                    .content((String) row[4])
                    .prevContent((String) row[5])
                    .nextContent((String) row[6])
                    .similarity(row[11] != null ? ((Number) row[11]).doubleValue() : null)
                    .build();
            matches.add(match);
        }

        return DocumentSearchResponse.builder()
                .businessId(businessId)
                .entityId(request.getEntityId())
                .displayName(request.getDisplayName().trim())
                .query(request.getQuery())
                .matches(matches)
                .build();
    }


    private String formatEmbedding(float[] embedding) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(embedding[i]);
        }
        builder.append("]");
        return builder.toString();
    }

    private User fetchUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
