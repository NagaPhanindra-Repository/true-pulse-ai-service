package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
}

