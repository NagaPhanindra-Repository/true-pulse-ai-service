package com.codmer.turepulseai.service;

import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingCacheService {

    private final EmbeddingModel embeddingModel;

    @Cacheable(value = "embeddings", key = "#text.hashCode()")
    public float[] embed(String text) {
        var embedding = embeddingModel.embed(text);
        float[] result = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            result[i] = (float) embedding[i];
        }
        return result;
    }

    /**
     * Directly embed text and return as PGvector without intermediate float array conversion
     */
    @Cacheable(value = "embeddings", key = "#text.hashCode()")
    public PGvector embedAsVector(String text) {
        var embedding = embeddingModel.embed(text);
        float[] result = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            result[i] = (float) embedding[i];
        }
        return new PGvector(result);
    }

    public int dimensions() {
        try {
            return embeddingModel.dimensions();
        } catch (Exception e) {
            log.warn("Could not determine embedding dimensions; defaulting to 1536", e);
            return 1536;
        }
    }
}

