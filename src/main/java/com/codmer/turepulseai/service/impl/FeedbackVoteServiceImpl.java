package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.entity.FeedbackVote;
import com.codmer.turepulseai.entity.FeedbackPoint;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.FeedbackVoteRequest;
import com.codmer.turepulseai.model.FeedbackVoteResponse;
import com.codmer.turepulseai.repository.FeedbackVoteRepository;
import com.codmer.turepulseai.repository.FeedbackPointRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.FeedbackVoteService;
import com.codmer.turepulseai.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeedbackVoteServiceImpl implements FeedbackVoteService {
    private final FeedbackVoteRepository feedbackVoteRepository;
    private final FeedbackPointRepository feedbackPointRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;


    @Override
    @Transactional
    public FeedbackVoteResponse handleSubmitOrUpdateVote(Long feedbackPointId, FeedbackVoteRequest request, String authHeader) {
        User user = extractUserFromAuthHeader(authHeader);
        FeedbackPoint feedbackPoint = feedbackPointRepository.findById(feedbackPointId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FeedbackPoint not found"));
        String voteTypeStr = request != null ? request.getVoteType() : null;
        if (voteTypeStr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "voteType is required");
        }
        FeedbackVote.VoteType voteType;
        try {
            voteType = FeedbackVote.VoteType.valueOf(voteTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid voteType. Must be LIKE or DISLIKE");
        }
        Optional<FeedbackVote> existingVoteOpt = feedbackVoteRepository.findByFeedbackPointAndUser(feedbackPoint, user);
        FeedbackVote vote = existingVoteOpt.orElseGet(() -> {
            FeedbackVote newVote = new FeedbackVote();
            newVote.setFeedbackPoint(feedbackPoint);
            newVote.setUser(user);
            return newVote;
        });
        vote.setVoteType(voteType);
        feedbackVoteRepository.save(vote);
        return buildVoteResponse(feedbackPoint, voteType.toString());
    }

    @Override
    public FeedbackVoteResponse handleGetVotes(Long feedbackPointId, String authHeader) {
        FeedbackPoint feedbackPoint = feedbackPointRepository.findById(feedbackPointId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FeedbackPoint not found"));
        String userVote = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                User user = extractUserFromAuthHeader(authHeader);
                Optional<FeedbackVote> userVoteOpt = feedbackVoteRepository.findByFeedbackPointAndUser(feedbackPoint, user);
                userVote = userVoteOpt.map(v -> v.getVoteType().toString()).orElse(null);
            } catch (Exception e) {
            }
        }
        return buildVoteResponse(feedbackPoint, userVote);
    }

    @Override
    @Transactional
    public FeedbackVoteResponse handleRemoveVote(Long feedbackPointId, String authHeader) {
        User user = extractUserFromAuthHeader(authHeader);
        FeedbackPoint feedbackPoint = feedbackPointRepository.findById(feedbackPointId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FeedbackPoint not found"));
        feedbackVoteRepository.findByFeedbackPointAndUser(feedbackPoint, user)
                .ifPresent(feedbackVoteRepository::delete);
        return buildVoteResponse(feedbackPoint, null);
    }

    private FeedbackVoteResponse buildVoteResponse(FeedbackPoint feedbackPoint, String userVote) {
        long likes = feedbackVoteRepository.countByFeedbackPointAndVoteType(feedbackPoint, FeedbackVote.VoteType.LIKE);
        long dislikes = feedbackVoteRepository.countByFeedbackPointAndVoteType(feedbackPoint, FeedbackVote.VoteType.DISLIKE);
        return FeedbackVoteResponse.builder()
                .likes(likes)
                .dislikes(dislikes)
                .userVote(userVote)
                .build();
    }

    private User extractUserFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header missing or invalid");
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        String username = jwtTokenProvider.getUsername(token);
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

}
