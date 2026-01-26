package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackPointDto {
    private Long id;
    private String type; // LIKED, LEARNED, LACKED, LONGED_FOR
    private String description;
    private Long retroId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

