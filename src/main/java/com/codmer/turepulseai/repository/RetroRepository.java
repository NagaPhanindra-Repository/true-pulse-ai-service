package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.Retro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetroRepository extends JpaRepository<Retro, Long> {
}

