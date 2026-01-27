package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.ActionItemDto;

import java.util.List;

public interface ActionItemService {
    ActionItemDto create(ActionItemDto dto);
    ActionItemDto getById(Long id);
    List<ActionItemDto> getAll();
    ActionItemDto update(Long id, ActionItemDto dto);
    void delete(Long id);
    List<ActionItemDto> getActionItemsByRetroId(Long retroId);
}

