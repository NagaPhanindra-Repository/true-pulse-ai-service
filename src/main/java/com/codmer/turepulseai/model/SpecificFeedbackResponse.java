package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for specific feedback analysis
 * Contains analysis of whether specific feedback aligns with overall sentiment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecificFeedbackResponse {

    /**
     * The question ID being analyzed
     */
    private Long questionId;

    /**
     * The specific feedback that was analyzed
     */
    private String specificFeedback;

    /**
     * AI-generated analysis of the specific feedback in context of all answers
     * Synthesized into 3-line summary covering:
     * 1. Whether this feedback aligns with overall sentiment (common/minority/unique)
     * 2. What most followers think about this aspect
     * 3. Is this a commonly disliked thing or if it's a strength
     *
     * Example:
     * "This feedback about high pricing aligns with majority sentiment - 75% of
     *  followers mentioned cost concerns. Most expect competitive pricing strategy.
     *  This is identified as the top disliked aspect across all feedback."
     */
    private String analysis;
}

