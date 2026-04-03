package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.FeedbackVote;
import com.codmer.turepulseai.entity.FeedbackPoint;
import com.codmer.turepulseai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface FeedbackVoteRepository extends JpaRepository<FeedbackVote, Long> {
    Optional<FeedbackVote> findByFeedbackPointAndUser(FeedbackPoint feedbackPoint, User user);
    List<FeedbackVote> findByFeedbackPoint(FeedbackPoint feedbackPoint);
    long countByFeedbackPointAndVoteType(FeedbackPoint feedbackPoint, FeedbackVote.VoteType voteType);
}
