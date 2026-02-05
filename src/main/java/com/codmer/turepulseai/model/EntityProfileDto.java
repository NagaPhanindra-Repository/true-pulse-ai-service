package com.codmer.turepulseai.model;

import com.codmer.turepulseai.entity.EntityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityProfileDto {
    private Long id;
    private EntityType type;
    private String displayName;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

