package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.FeedbackPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackPointRepository extends JpaRepository<FeedbackPoint, Long> {
}

