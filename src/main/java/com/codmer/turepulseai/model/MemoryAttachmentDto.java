package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoryAttachmentDto {
    private UUID id;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSizeBytes;
    private LocalDateTime uploadedAt;
}

