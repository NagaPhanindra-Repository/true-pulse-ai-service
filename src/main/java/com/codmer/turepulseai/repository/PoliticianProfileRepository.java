package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.PoliticianProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoliticianProfileRepository extends JpaRepository<PoliticianProfile, Long> {
    @Query("SELECT p FROM PoliticianProfile p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<PoliticianProfile> searchByFullName(@Param("searchTerm") String searchTerm);
}
