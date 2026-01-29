package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for questions posted by followed users
 * Includes the question details and the logged-in user's answer (if they answered)
 *
 * This DTO is designed for the "Feed from followed users" feature
 * Shows questions from users that the logged-in user follows
 * and displays the logged-in user's answer to each question (if exists)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowedUserQuestionResponse {

    /**
     * Unique identifier of the question
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
     * ID of the user who created the question
     */
    private Long questionCreatorUserId;

    /**
     * Username of the user who created the question
     */
    private String questionCreatorUsername;

    /**
     * First name of the user who created the question
     */
    private String questionCreatorFirstName;

    /**
     * Last name of the user who created the question
     */
    private String questionCreatorLastName;

    /**
     * When the question was created (for sorting)
     */
    private LocalDateTime questionCreatedAt;

    /**
     * The logged-in user's answer to this question (if they answered)
     * NULL if logged-in user hasn't answered this question
     */
    private LoggedInUserAnswerDto loggedInUserAnswer;

    /**
     * Total count of answers to this question
     */
    private Long totalAnswersCount;

    /**
     * Nested DTO for logged-in user's answer
     * Contains only the answer created by logged-in user
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoggedInUserAnswerDto {

        /**
         * ID of the answer
         */
        private Long answerId;

        /**
         * Content of the answer
         */
        private String answerContent;

        /**
         * When the answer was created
         */
        private LocalDateTime answerCreatedAt;

        /**
         * When the answer was last updated
         */
        private LocalDateTime answerUpdatedAt;
    }
}

