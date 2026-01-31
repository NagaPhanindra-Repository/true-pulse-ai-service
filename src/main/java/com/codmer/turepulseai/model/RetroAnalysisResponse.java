package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI-generated retrospective analysis summary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetroAnalysisResponse {
    private Long retroId;
    private String summary;
}

