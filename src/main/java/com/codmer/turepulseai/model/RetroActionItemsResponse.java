package com.codmer.turepulseai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO that separates current retro action items from past retro action items
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetroActionItemsResponse {
    /**
     * Action items for the current retro
     */
    private List<ActionItemDto> currentRetroActionItems;

    /**
     * Open and in-progress action items from past retros created by the same user
     */
    private List<ActionItemDto> pastRetroActionItems;
}

