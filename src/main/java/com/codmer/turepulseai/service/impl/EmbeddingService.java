package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.entity.BusinessDocumentChunk;
import com.codmer.turepulseai.repository.BusinessDocumentChunkRepository;
import com.pgvector.PGvector;
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
            // Generate embedding from content
            float[] embeddingVector = generateEmbedding(chunk.getContent());

            // Convert float array to PGvector format
            PGvector vector = convertToPGvector(embeddingVector);
            chunk.setEmbedding(vector);
            chunk.setEmbeddingDimension(embeddingVector.length);

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
     * Convert float array to pgvector PGvector
     */
    private PGvector convertToPGvector(float[] embedding) {
        return new PGvector(embedding);
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
     * Search for similar chunks using vector similarity
     */
    @Transactional(readOnly = true)
    public java.util.List<BusinessDocumentChunk> searchSimilarChunks(String businessId, String queryText, int limit) {
        try {
            // Generate embedding for query text
            float[] queryEmbedding = generateEmbedding(queryText);
            PGvector queryVector = convertToPGvector(queryEmbedding);

            // Perform similarity search using pgvector
            String sql = "SELECT * FROM business_document_chunks " +
                    "WHERE business_id = ? " +
                    "ORDER BY embedding <-> CAST(? AS vector) " +
                    "LIMIT ?";

            return jdbcTemplate.query(sql, new Object[]{businessId, queryVector.getValue(), limit},
                    (rs, rowNum) -> mapRowToChunk(rs));
        } catch (Exception e) {
            log.error("Error searching similar chunks", e);
            throw new RuntimeException("Failed to search similar chunks", e);
        }
    }

    private BusinessDocumentChunk mapRowToChunk(java.sql.ResultSet rs) throws java.sql.SQLException {
        BusinessDocumentChunk chunk = new BusinessDocumentChunk();
        chunk.setId(rs.getLong("id"));
        chunk.setDocumentId(rs.getLong("document_id"));
        chunk.setBusinessId(rs.getString("business_id"));
        chunk.setChunkIndex(rs.getInt("chunk_index"));
        chunk.setContent(rs.getString("content"));
        chunk.setPrevContent(rs.getString("prev_content"));
        chunk.setNextContent(rs.getString("next_content"));
        chunk.setEmbeddingDimension(rs.getInt("embedding_dimension"));
        chunk.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("updated_at") != null) {
            chunk.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return chunk;
    }
}

