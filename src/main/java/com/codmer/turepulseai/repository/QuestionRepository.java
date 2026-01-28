package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByUserId(Long userId);
}

