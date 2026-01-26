package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprehensive Retro Details DTO
 * Used for shared retro link - fetches all related data in one call
 * Includes: Retro → FeedbackPoints → Discussions → Users
 *           → ActionItems → Users
 *           → Questions → Users
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetroDetailDto {

    // Retro Information
    private Long id;
    private String title;
    private String description;
    private Long userId;
    private String createdBy; // Username of who created
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Nested Collections
    private List<FeedbackPointDetailDto> feedbackPoints;
    private List<ActionItemDetailDto> actionItems;
    private List<QuestionDetailDto> questions;

    /**
     * Feedback Point with nested discussions
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackPointDetailDto {
        private Long id;
        private String type; // LIKED, LEARNED, LACKED, LONGED_FOR
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // Nested discussions
        private List<DiscussionDetailDto> discussions;
    }

    /**
     * Discussion with user details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscussionDetailDto {
        private Long id;
        private String note;
        private Long userId;
        private String userName; // Username of discussion author
        private String userFirstName;
        private String userLastName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Action Item with assignee details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItemDetailDto {
        private Long id;
        private String description;
        private java.time.LocalDate dueDate;
        private boolean completed;
        private String status; // OPEN, IN_PROGRESS, COMPLETED, CANCELLED
        private LocalDateTime completedAt;

        // Assignee details
        private Long assignedUserId;
        private String assignedUserName;
        private String assignedUserFirstName;
        private String assignedUserLastName;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Question with user details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDetailDto {
        private Long id;
        private String title;
        private String description;
        private Long userId;
        private String userName; // Username of question creator
        private String userFirstName;
        private String userLastName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}

