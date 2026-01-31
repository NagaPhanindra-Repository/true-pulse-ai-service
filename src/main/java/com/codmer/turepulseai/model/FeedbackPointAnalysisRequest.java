package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for AI analysis of a feedback point within a retro.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackPointAnalysisRequest {
    private Long retroId;
    private Long feedbackPointId;
}

