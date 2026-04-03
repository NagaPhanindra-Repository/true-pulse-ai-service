package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.FeedbackVoteRequest;
import com.codmer.turepulseai.model.FeedbackVoteResponse;
import com.codmer.turepulseai.service.FeedbackVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback-points")
@RequiredArgsConstructor
public class FeedbackVoteController {
    private final FeedbackVoteService feedbackVoteService;

    @PostMapping("/{id}/vote")
    public ResponseEntity<FeedbackVoteResponse> submitOrUpdateVote(@PathVariable Long id, @RequestBody FeedbackVoteRequest request, @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(feedbackVoteService.handleSubmitOrUpdateVote(id, request, authHeader));
    }

    @GetMapping("/{id}/votes")
    public ResponseEntity<FeedbackVoteResponse> getVotes(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(feedbackVoteService.handleGetVotes(id, authHeader));
    }

    @DeleteMapping("/{id}/vote")
    public ResponseEntity<FeedbackVoteResponse> removeVote(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(feedbackVoteService.handleRemoveVote(id, authHeader));
    }
}
