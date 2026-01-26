package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionItemDto {
    private Long id;
    private String description;
    private LocalDate dueDate;
    private boolean completed;
    private String status; // OPEN, IN_PROGRESS, COMPLETED, CANCELLED
    private Long retroId;
    private Long assignedUserId;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

