package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackVoteResponse {
    private long likes;
    private long dislikes;
    private String userVote;
}
