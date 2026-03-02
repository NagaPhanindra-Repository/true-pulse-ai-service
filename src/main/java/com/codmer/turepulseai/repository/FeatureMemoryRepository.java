package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.FeatureMemory;
import com.codmer.turepulseai.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureMemoryRepository extends JpaRepository<FeatureMemory, UUID> {

    Page<FeatureMemory> findByUser(User user, Pageable pageable);

    Page<FeatureMemory> findByUserAndStatus(User user, String status, Pageable pageable);

    @Query("SELECT fm FROM FeatureMemory fm WHERE fm.user = :user AND " +
           "(LOWER(fm.jiraStoryKey) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(fm.jiraStoryTitle) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<FeatureMemory> searchByUser(@Param("user") User user, @Param("search") String search, Pageable pageable);

    List<FeatureMemory> findByUserAndStatus(User user, String status);

    Optional<FeatureMemory> findByIdAndUser(UUID id, User user);

    Optional<FeatureMemory> findByUserAndJiraStoryKey(User user, String jiraStoryKey);

    List<FeatureMemory> findByJiraStoryKey(String jiraStoryKey);
}

