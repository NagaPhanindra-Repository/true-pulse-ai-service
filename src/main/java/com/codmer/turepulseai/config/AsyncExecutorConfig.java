package com.codmer.turepulseai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async task execution.
 * Enables parallel processing of independent AI analysis tasks.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncExecutorConfig {

    /**
     * Executor for AI analysis tasks (summarization, context building, etc.)
     * Thread pool optimized for I/O-bound operations (API calls)
     */
    @Bean(name = "aiAnalysisExecutor")
    public Executor aiAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);  // Minimum threads
        executor.setMaxPoolSize(100);  // Maximum threads
        executor.setQueueCapacity(100);  // Queue for pending tasks
        executor.setThreadNamePrefix("ai-analysis-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("AIAnalysisExecutor initialized with corePoolSize=4, maxPoolSize=10");
        return executor;
    }

    /**
     * Executor for general async tasks
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }
}

