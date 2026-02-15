package com.codmer.turepulseai.util;

import com.codmer.turepulseai.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Supplier;

/**
 * Utility for handling AI API rate limits and retries
 */
@Slf4j
@Component
public class RateLimitHandler {

    private static final Pattern RATE_LIMIT_PATTERN = Pattern.compile("Please try again in ([0-9.]+)s");
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1 second

    /**
     * Executes an operation with exponential backoff retry logic for rate limits
     *
     * @param operation The operation to execute
     * @param operationName Name of the operation for logging
     * @return Result of the operation
     * @throws RateLimitExceededException if rate limit is exceeded after max retries
     */
    public <T> T executeWithRateLimitRetry(Supplier<T> operation, String operationName) {
        int retryCount = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (retryCount <= MAX_RETRIES) {
            try {
                log.debug("Executing operation: {} (attempt {})", operationName, retryCount + 1);
                return operation.get();
            } catch (NonTransientAiException ex) {
                if (isRateLimitError(ex)) {
                    long retryAfterSeconds = extractRetryAfterSeconds(ex);

                    if (retryCount < MAX_RETRIES) {
                        log.warn("Rate limit hit for {}. Retrying in {} ms (attempt {}/{})",
                            operationName, backoffMs, retryCount + 1, MAX_RETRIES);

                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RateLimitExceededException(
                                "Operation interrupted during rate limit retry: " + operationName,
                                retryAfterSeconds,
                                ie
                            );
                        }

                        retryCount++;
                        backoffMs = calculateNextBackoff(backoffMs);
                    } else {
                        log.error("Max retries exceeded for {} after {} attempts", operationName, MAX_RETRIES);
                        throw new RateLimitExceededException(
                            "Rate limit exceeded for " + operationName + ". Please try again in " + retryAfterSeconds + " seconds.",
                            retryAfterSeconds,
                            ex
                        );
                    }
                } else {
                    // Not a rate limit error, rethrow
                    throw ex;
                }
            }
        }

        throw new RateLimitExceededException(
            "Failed to execute " + operationName + " after " + MAX_RETRIES + " retries",
            INITIAL_BACKOFF_MS / 1000
        );
    }

    /**
     * Checks if the exception is a rate limit error
     */
    private boolean isRateLimitError(NonTransientAiException ex) {
        String message = ex.getMessage();
        return message != null && (
            message.contains("429") ||
            message.contains("rate_limit_exceeded") ||
            message.contains("Rate limit") ||
            message.contains("rate limit")
        );
    }

    /**
     * Extracts retry-after seconds from the error message
     */
    private long extractRetryAfterSeconds(NonTransientAiException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return 2; // Default fallback
        }

        Matcher matcher = RATE_LIMIT_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                double seconds = Double.parseDouble(matcher.group(1));
                return Math.max(1, (long) Math.ceil(seconds));
            } catch (NumberFormatException e) {
                log.warn("Could not parse retry-after value from message: {}", message);
            }
        }

        return 2; // Default fallback
    }

    /**
     * Calculates next backoff with exponential backoff strategy
     */
    private long calculateNextBackoff(long currentBackoffMs) {
        long nextBackoff = Math.min(currentBackoffMs * 2, 30000); // Max 30 seconds
        // Add jitter: ±10%
        double jitter = 0.9 + (Math.random() * 0.2);
        return (long) (nextBackoff * jitter);
    }
}

