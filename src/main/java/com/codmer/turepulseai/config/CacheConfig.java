package com.codmer.turepulseai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for LLM responses and database queries
 * Reduces API calls, token usage, and latency
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure cache manager with Caffeine
     * Different caches with different TTLs for various use cases
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Default cache configuration - 2 hours TTL, max 1000 entries
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .recordStats());

        // Register specific caches
        cacheManager.setCacheNames(java.util.List.of(
                "questionAnalysis",        // Cache for question analysis - 6 hours
                "specificFeedback",        // Cache for specific feedback analysis - 4 hours
                "userQuestionsAnalysis",   // Cache for user questions analysis - 1 hour
                "ragAnswers",              // Cache for RAG-based answers - 3 hours
                "embeddings",              // Cache for embeddings - 24 hours
                "questionData",            // Cache for question+answers data - 30 minutes
                "chatResponses"            // Cache for general chat responses - 1 hour
        ));

        return cacheManager;
    }

    /**
     * Bean for question analysis cache - longer TTL since analysis doesn't change frequently
     */
    @Bean
    public Caffeine<Object, Object> questionAnalysisCaffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(6, TimeUnit.HOURS)
                .recordStats();
    }

    /**
     * Bean for specific feedback cache - moderate TTL
     */
    @Bean
    public Caffeine<Object, Object> specificFeedbackCaffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(4, TimeUnit.HOURS)
                .recordStats();
    }

    /**
     * Bean for user questions analysis cache - shorter TTL as new answers may come
     */
    @Bean
    public Caffeine<Object, Object> userQuestionsAnalysisCaffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats();
    }

    /**
     * Bean for RAG answers cache - moderate TTL
     */
    @Bean
    public Caffeine<Object, Object> ragAnswersCaffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(3, TimeUnit.HOURS)
                .recordStats();
    }

    /**
     * Bean for embeddings cache - long TTL as embeddings don't change
     */
    @Bean
    public Caffeine<Object, Object> embeddingsCaffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats();
    }

    /**
     * Bean for question data cache - short TTL as answers can be added
     */
    @Bean
    public Caffeine<Object, Object> questionDataCaffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(300)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * Bean for general chat responses cache
     */
    @Bean
    public Caffeine<Object, Object> chatResponsesCaffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats();
    }
}

