package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;
import com.pgvector.PGvector;
import com.codmer.turepulseai.config.VectorType;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "business_document_chunks", indexes = {
        @Index(name = "idx_doc_chunks_document_id", columnList = "document_id"),
        @Index(name = "idx_doc_chunks_business_id", columnList = "business_id")
})
public class BusinessDocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String prevContent;

    @Column(columnDefinition = "TEXT")
    private String nextContent;

    @Column(nullable = false)
    private Integer embeddingDimension;

    @Type(VectorType.class)
    @Column(columnDefinition = "vector(1536)", name = "embedding")
    private PGvector embedding;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
