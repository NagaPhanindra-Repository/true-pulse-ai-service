package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByQuestionId(Long questionId);
    List<Answer> findByUserId(Long userId);
    List<Answer> findByQuestionIdOrderByCreatedAtDesc(Long questionId);

    /**
     * Find a specific user's answer to a specific question
     * Returns Optional since user may not have answered the question
     *
     * @param questionId - The question ID
     * @param userId - The user ID
     * @return Optional containing the answer if user answered the question
     */
    Optional<Answer> findByQuestionIdAndUserId(Long questionId, Long userId);

    /**
     * Count total answers for a question
     *
     * @param questionId - The question ID
     * @return Count of answers for the question
     */
    long countByQuestionId(Long questionId);
}

