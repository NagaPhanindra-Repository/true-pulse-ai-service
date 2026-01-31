package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for AI analysis of a feedback point.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackPointAnalysisResponse {
    private Long retroId;
    private Long feedbackPointId;
    private String feedbackType;
    private String analysis;
}

