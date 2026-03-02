package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.MemoryDiscussion;
import com.codmer.turepulseai.entity.FeatureMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MemoryDiscussionRepository extends JpaRepository<MemoryDiscussion, UUID> {

    List<MemoryDiscussion> findByFeatureMemoryOrderByRecordedAtAsc(FeatureMemory featureMemory);

    List<MemoryDiscussion> findByFeatureMemoryAndDecisionTypeOrderByRecordedAtAsc(
        FeatureMemory featureMemory, String decisionType);

    @Query("SELECT md FROM MemoryDiscussion md WHERE md.featureMemory = :memory " +
           "AND LOWER(md.discussionText) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY md.recordedAt ASC")
    List<MemoryDiscussion> searchInMemory(@Param("memory") FeatureMemory featureMemory,
                                          @Param("query") String query);

    long countByFeatureMemory(FeatureMemory featureMemory);
}

