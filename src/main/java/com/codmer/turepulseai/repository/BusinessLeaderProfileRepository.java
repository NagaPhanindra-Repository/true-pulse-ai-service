package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.BusinessLeaderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessLeaderProfileRepository extends JpaRepository<BusinessLeaderProfile, Long> {
    @Query("SELECT b FROM BusinessLeaderProfile b WHERE LOWER(b.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<BusinessLeaderProfile> searchByFullName(@Param("searchTerm") String searchTerm);
}
