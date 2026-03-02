package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.MemoryAttachment;
import com.codmer.turepulseai.entity.FeatureMemory;
import com.codmer.turepulseai.entity.MemoryDiscussion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MemoryAttachmentRepository extends JpaRepository<MemoryAttachment, UUID> {

    List<MemoryAttachment> findByFeatureMemory(FeatureMemory featureMemory);

    List<MemoryAttachment> findByDiscussion(MemoryDiscussion discussion);
}

