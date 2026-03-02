package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.GitBranchMapping;
import com.codmer.turepulseai.entity.FeatureMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitBranchMappingRepository extends JpaRepository<GitBranchMapping, UUID> {

    List<GitBranchMapping> findByFeatureMemory(FeatureMemory featureMemory);

    Optional<GitBranchMapping> findByBranchName(String branchName);

    Optional<GitBranchMapping> findByFeatureMemoryAndBranchName(FeatureMemory featureMemory, String branchName);
}

