package com.codmer.turepulseai.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoryDiscussionDto {
    private UUID id;
    private String discussionText;
    private String decisionType;
    private String[] tags;
    private LocalDate meetingDate;
    private LocalDateTime recordedAt;
    private LocalDateTime updatedAt;
    private String authorName;
    private List<MemoryAttachmentDto> attachments;
}

