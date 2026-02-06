package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.entity.BusinessDocumentChunk;
import com.codmer.turepulseai.repository.BusinessDocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final BusinessDocumentChunkRepository chunkRepository;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Generate embedding and store chunk in database
     */
    @Transactional
    public BusinessDocumentChunk embedAndStoreChunk(BusinessDocumentChunk chunk) {
        try {
            float[] embeddingVector = generateEmbedding(chunk.getContent());
            chunk.setEmbeddingDimension(embeddingVector.length);

            // Save chunk without embedding first to avoid JPA PGvector binding issues.
            BusinessDocumentChunk savedChunk = chunkRepository.save(chunk);

            String vectorLiteral = formatEmbeddingForPostgres(embeddingVector);
            jdbcTemplate.update(
                    "UPDATE business_document_chunks SET embedding = CAST(? AS vector), embedding_dimension = ? WHERE id = ?",
                    vectorLiteral, embeddingVector.length, savedChunk.getId());

            log.info("Chunk {} embedded and stored successfully", savedChunk.getId());
            return savedChunk;
        } catch (Exception e) {
            log.error("Error embedding and storing chunk", e);
            throw new RuntimeException("Failed to embed and store chunk", e);
        }
    }

    /**
     * Generate embedding for given text using Spring AI
     */
    private float[] generateEmbedding(String text) {
        try {
            var embedding = embeddingModel.embed(text);
            float[] result = new float[embedding.length];
            for (int i = 0; i < embedding.length; i++) {
                result[i] = (float) embedding[i];
            }
            return result;
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    /**
     * Format embedding array as PostgreSQL vector literal.
     * Example: [0.1,0.2,0.3]
     */
    private String formatEmbeddingForPostgres(float[] embedding) {
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
}
