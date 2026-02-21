package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    List<ActionItem> findByRetroId(Long retroId);

    /**
     * Find OPEN and IN_PROGRESS action items from past retros created by the same user
     * Excludes action items from the specified current retro
     *
     * @param retroIds list of retro IDs to include
     * @param statuses list of statuses to filter by (OPEN, IN_PROGRESS)
     * @return List of action items from past retros
     */
    @Query("SELECT ai FROM ActionItem ai " +
           "JOIN ai.retro r " +
           "WHERE r.id IN :retroIds " +
           "AND ai.status IN :statuses " +
           "ORDER BY ai.dueDate ASC, ai.createdAt DESC")
    List<ActionItem> findPastRetrosActionItemsByUserIdAndStatuses(
        @Param("retroIds") List<Long> retroIds,
        @Param("statuses") List<ActionItem.ActionItemStatus> statuses
    );
}
