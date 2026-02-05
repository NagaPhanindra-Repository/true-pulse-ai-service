package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.CelebrityProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CelebrityProfileRepository extends JpaRepository<CelebrityProfile, Long> {
    @Query("SELECT c FROM CelebrityProfile c WHERE LOWER(c.realName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(c.artistName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<CelebrityProfile> searchByName(@Param("searchTerm") String searchTerm);
}
