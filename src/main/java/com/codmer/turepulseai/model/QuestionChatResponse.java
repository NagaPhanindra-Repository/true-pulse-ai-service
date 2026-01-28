package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for question analysis using AI
 * Contains concise analysis of answers to a question
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionChatResponse {

    /**
     * The question ID that was analyzed
     */
    private Long questionId;

    /**
     * Question details (title and description combined)
     */
    private String questionDetails;

    /**
     * Comprehensive AI-generated analysis summary
     * Includes: what followers think, what they like/dislike,
     * future expectations, and actionable recommendations
     * Synthesized into 2-3 lines without repetition
     */
    private String analysis;
}

