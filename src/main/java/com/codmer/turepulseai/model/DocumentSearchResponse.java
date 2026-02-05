package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResponse {
    private String businessId;
    private Long entityId;
    private String displayName;
    private String query;
    private List<MatchedChunk> matches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedChunk {
        private Long documentId;
        private Integer chunkIndex;
        private String content;
        private String prevContent;
        private String nextContent;
        private Double similarity;
    }
}
