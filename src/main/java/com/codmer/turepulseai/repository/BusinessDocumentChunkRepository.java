package com.codmer.turepulseai.repository;

import com.codmer.turepulseai.entity.BusinessDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessDocumentChunkRepository extends JpaRepository<BusinessDocumentChunk, Long> {

    List<BusinessDocumentChunk> findByDocumentId(Long documentId);

    List<BusinessDocumentChunk> findByBusinessId(String businessId);

    List<BusinessDocumentChunk> findByDocumentIdAndBusinessId(Long documentId, String businessId);

    @Query(value = "SELECT * FROM business_document_chunks " +
            "WHERE business_id = :businessId " +
            "ORDER BY embedding <-> CAST(:embedding AS vector) " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarChunks(@Param("businessId") String businessId,
                                     @Param("embedding") String embedding,
                                     @Param("limit") int limit);

    @Query(value = "SELECT * FROM business_document_chunks " +
            "WHERE entity_id = :entityId AND display_name = :displayName " +
            "ORDER BY embedding <-> CAST(:embedding AS vector) " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarChunksByEntity(@Param("entityId") Long entityId,
                                             @Param("displayName") String displayName,
                                             @Param("embedding") String embedding,
                                             @Param("limit") int limit);

    @Query(value = "SELECT * FROM business_document_chunks " +
            "WHERE business_id = :businessId AND entity_id = :entityId AND display_name = :displayName " +
            "ORDER BY embedding <-> CAST(:embedding AS vector) " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarChunksByEntityAndBusiness(@Param("businessId") String businessId,
                                                        @Param("entityId") Long entityId,
                                                        @Param("displayName") String displayName,
                                                        @Param("embedding") String embedding,
                                                        @Param("limit") int limit);
}
