package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for analyzing specific feedback/comment in context of all answers
 * Takes a question ID and a specific feedback/comment to analyze
 *
 * Use Cases:
 * - Check if a comment aligns with most followers' sentiments
 * - Determine if feedback is a common dislike
 * - Analyze minority vs majority opinions
 * - Understand context of specific feedback within all responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecificFeedbackRequest {

    /**
     * The ID of the question being discussed
     * Required field - must exist in the database
     */
    private Long questionId;

    /**
     * The specific feedback/comment/question from a follower to analyze
     * This will be analyzed in context of all answers to see if it represents
     * common sentiment, a minority view, or a unique perspective
     * Required field
     */
    private String specificFeedback;
}

