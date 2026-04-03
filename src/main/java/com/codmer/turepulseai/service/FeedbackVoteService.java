package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.FeedbackVoteRequest;
import com.codmer.turepulseai.model.FeedbackVoteResponse;


public interface FeedbackVoteService {
    FeedbackVoteResponse handleSubmitOrUpdateVote(Long feedbackPointId, FeedbackVoteRequest request, String authHeader);
    FeedbackVoteResponse handleGetVotes(Long feedbackPointId, String authHeader);
    FeedbackVoteResponse handleRemoveVote(Long feedbackPointId, String authHeader);
}
