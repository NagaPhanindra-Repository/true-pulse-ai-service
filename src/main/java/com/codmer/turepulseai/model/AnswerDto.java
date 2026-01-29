package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDto {
    private Long id;
    private String content;
    private Long questionId;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String username;
}

