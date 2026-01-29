package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByUserId(Long userId);

    /**
     * Get all questions created by a list of users
     * Ordered by creation date (latest first)
     *
     * @param userIds - List of user IDs whose questions to fetch
     * @return List of questions ordered by latest created date
     */
    @Query("SELECT q FROM Question q WHERE q.user.id IN :userIds ORDER BY q.createdAt DESC")
    List<Question> findByUserIdInOrderByCreatedAtDesc(@Param("userIds") List<Long> userIds);
}

