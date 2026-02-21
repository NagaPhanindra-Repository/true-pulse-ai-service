package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.ActionItemDto;
import com.codmer.turepulseai.model.RetroActionItemsResponse;

import java.util.List;

public interface ActionItemService {
    ActionItemDto create(ActionItemDto dto);
    ActionItemDto getById(Long id);
    List<ActionItemDto> getAll();
    ActionItemDto update(Long id, ActionItemDto dto);
    void delete(Long id);
    List<ActionItemDto> getActionItemsByRetroId(Long retroId);

    /**
     * Get action items for a retro, including pending action items from past retros
     *
     * @param retroId the current retro ID
     * @return RetroActionItemsResponse with current and past action items separated
     */
    RetroActionItemsResponse getActionItemsWithPastRetros(Long retroId);
}



