package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for comprehensive analysis of a user's question
 * Provides detailed insights including sentiment, liked/disliked aspects,
 * expectations, and recommendations based on all answers received
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestionsAnalysisResponse {

    /**
     * The question ID being analyzed
     */
    private Long questionId;

    /**
     * Title of the question
     */
    private String questionTitle;

    /**
     * Description/context of the question
     */
    private String questionDescription;

    /**
     * Total number of answers received for this question
     */
    private Integer totalAnswers;

    /**
     * Executive summary - 3-line high-level overview
     * Summarizes key findings at a glance
     */
    private String executiveSummary;

    /**
     * General sentiment analysis
     * Overall sentiment with percentage satisfaction rate,
     * excitement level, and competitive comparison
     */
    private String generalSentiment;

    /**
     * Most liked aspects
     * What followers/customers appreciate most,
     * including strengths and positive highlights
     */
    private String mostLikedAspects;

    /**
     * Most disliked aspects
     * Common complaints, concerns, and areas needing improvement
     */
    private String mostDislikedAspects;

    /**
     * Future expectations
     * What followers hope to see, expect in future iterations,
     * and desired improvements
     */
    private String futureExpectations;

    /**
     * Actionable recommendations
     * Strategic recommendations based on feedback analysis
     * for product/service improvement
     */
    private String recommendations;

    /**
     * AI model used for analysis
     */
    private String model;

    /**
     * Timestamp when analysis was created (Unix epoch seconds)
     */
    private Long createdAt;
}

