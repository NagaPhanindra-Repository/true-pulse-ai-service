package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "business_documents", indexes = {
        @Index(name = "idx_business_documents_business_id", columnList = "business_id"),
        @Index(name = "idx_business_documents_user_id", columnList = "user_id")
})
public class BusinessDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String title;

    private String fileName;

    private String fileType;

    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String storagePath;

    @Column(nullable = false)
    private String status;

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
