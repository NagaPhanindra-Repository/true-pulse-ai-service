package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for analyzing question and its answers using AI
 * Takes a question ID and optional user message for contextual analysis
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionChatRequest {

    /**
     * The ID of the question to analyze
     * Required field - must exist in the database
     */
    private Long questionId;

    /**
     * Optional message from the question creator for specific guidance
     * Can be used to focus the analysis on particular aspects
     */
    private String message;
}


