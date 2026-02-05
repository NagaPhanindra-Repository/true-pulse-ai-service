package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.BusinessDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessDocumentRepository extends JpaRepository<BusinessDocument, Long> {
    List<BusinessDocument> findByBusinessId(String businessId);
    List<BusinessDocument> findByUserId(Long userId);
}

