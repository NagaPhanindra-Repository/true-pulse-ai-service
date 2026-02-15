package com.codmer.turepulseai.exception;

/**
 * Exception thrown when rate limit is exceeded
 */
public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String message, long retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

