package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionDto {
    private Long id;
    private String note;
    private Long feedbackPointId;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

