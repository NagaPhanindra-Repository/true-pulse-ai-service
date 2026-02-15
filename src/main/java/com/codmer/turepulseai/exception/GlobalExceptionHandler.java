package com.codmer.turepulseai.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

/**
 * Global exception handler for all REST API exceptions.
 * Prevents authentication from being invalidated on service errors.
 * Handles rate limit and AI service exceptions gracefully.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles rate limit exceeded exceptions from OpenAI API
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded. Retry after: {} seconds", ex.getRetryAfterSeconds());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Rate Limit Exceeded");
        body.put("message", "AI service rate limit reached. " + ex.getMessage());
        body.put("retryAfterSeconds", ex.getRetryAfterSeconds());

        return new ResponseEntity<>(body, HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * Handles non-transient AI exceptions (like rate limits from Spring AI)
     * Does NOT force logout - keeps user session active
     */
    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<Map<String, Object>> handleNonTransientAiException(NonTransientAiException ex) {
        log.error("Non-transient AI exception occurred: {}", ex.getMessage());

        String message = ex.getMessage();
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String userMessage = "AI service temporarily unavailable. Please try again in a few moments.";

        // Check if it's a rate limit error
        if (message != null && (message.contains("429") || message.contains("rate_limit_exceeded"))) {
            status = HttpStatus.TOO_MANY_REQUESTS;
            userMessage = "AI service rate limit reached. Please wait before retrying.";
            log.warn("OpenAI API rate limit reached.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", userMessage);

        return new ResponseEntity<>(body, status);
    }

    /**
     * Handles completion exceptions that wrap AI exceptions (from parallel execution)
     */
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<Map<String, Object>> handleCompletionException(CompletionException ex) {
        log.error("Completion exception occurred: {}", ex.getMessage());

        Throwable cause = ex.getCause();

        // If the cause is an AI exception, handle it as such
        if (cause instanceof NonTransientAiException) {
            return handleNonTransientAiException((NonTransientAiException) cause);
        }

        if (cause instanceof RateLimitExceededException) {
            return handleRateLimitExceeded((RateLimitExceededException) cause);
        }

        // Generic completion exception handler
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An error occurred during processing. Please try again.");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles ResponseStatusException
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.error("Response status exception: {}", ex.getReason());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatusCode().value());
        body.put("error", ex.getStatusCode().toString());
        body.put("message", ex.getReason());

        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    /**
     * Handles access denied exceptions (but doesn't invalidate authentication)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Access Denied");
        body.put("message", "You do not have permission to access this resource.");

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument exception: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        log.error("Unexpected exception occurred", ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please try again later.");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

