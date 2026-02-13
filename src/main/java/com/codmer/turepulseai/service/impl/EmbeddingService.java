package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.entity.BusinessDocumentChunk;
import com.codmer.turepulseai.repository.BusinessDocumentChunkRepository;
import com.codmer.turepulseai.service.EmbeddingCacheService;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final BusinessDocumentChunkRepository chunkRepository;
    private final EmbeddingCacheService embeddingCacheService;

    /**
     * Generate embedding and store chunk in database
     */
    @Transactional
    public BusinessDocumentChunk embedAndStoreChunk(BusinessDocumentChunk chunk) {
        try {
            // Directly get PGvector without intermediate float array conversion
            PGvector embeddingVector = generateEmbedding(chunk.getContent());
            chunk.setEmbeddingDimension(embeddingVector.toArray().length);
            chunk.setEmbedding(embeddingVector);

            // Save chunk with embedding
            BusinessDocumentChunk savedChunk = chunkRepository.save(chunk);

            log.info("Chunk {} embedded and stored successfully", savedChunk.getId());
            return savedChunk;
        } catch (Exception e) {
            log.error("Error embedding and storing chunk", e);
            throw new RuntimeException("Failed to embed and store chunk", e);
        }
    }

    /**
     * Generate embedding for given text directly as PGvector
     */
    private PGvector generateEmbedding(String text) {
        return embeddingCacheService.embedAsVector(text);
    }
}
