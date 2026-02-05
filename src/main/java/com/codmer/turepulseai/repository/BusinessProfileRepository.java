package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    @Query("SELECT b FROM BusinessProfile b WHERE LOWER(b.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<BusinessProfile> searchByFullName(@Param("searchTerm") String searchTerm);
}
