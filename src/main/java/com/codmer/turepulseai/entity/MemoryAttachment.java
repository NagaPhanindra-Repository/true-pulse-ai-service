package com.codmer.turepulseai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "memory_attachments", indexes = {
    @Index(name = "idx_memory_attachments_feature", columnList = "feature_memory_id"),
    @Index(name = "idx_memory_attachments_discussion", columnList = "discussion_id")
})
public class MemoryAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_memory_id")
    private FeatureMemory featureMemory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id")
    private MemoryDiscussion discussion;

    @Column(length = 255)
    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String fileUrl;

    @Column(length = 100)
    private String fileType;

    @Column
    private Long fileSizeBytes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}

