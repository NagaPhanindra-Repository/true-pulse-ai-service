package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.EntityProfile;
import com.codmer.turepulseai.entity.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntityProfileRepository extends JpaRepository<EntityProfile, Long> {
    List<EntityProfile> findByType(EntityType type);
    List<EntityProfile> findByCreatedByUserId(Long createdByUserId);

    @Query(value = "SELECT * FROM entity_profiles ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<EntityProfile> findRandomEntities(@Param("limit") int limit);

    @Query("SELECT e FROM EntityProfile e WHERE LOWER(e.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<EntityProfile> searchByDisplayName(@Param("searchTerm") String searchTerm);
}
