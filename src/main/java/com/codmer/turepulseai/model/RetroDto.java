package com.codmer.turepulseai.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RetroDto {

    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private Long userId;
    private String description;
    private String title;
    private Long id;
}





