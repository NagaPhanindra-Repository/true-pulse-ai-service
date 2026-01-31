package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.Retro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RetroRepository extends JpaRepository<Retro, Long> {
    List<Retro> findByUserId(Long userId);
    List<Retro> findByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(Long userId, java.time.LocalDateTime createdAt);
}
