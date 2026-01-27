package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    List<ActionItem> findByRetroId(Long retroId);
}

