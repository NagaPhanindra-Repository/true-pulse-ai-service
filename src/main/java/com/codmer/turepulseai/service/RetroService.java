package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.RetroDto;
import com.codmer.turepulseai.model.RetroDetailDto;

import java.util.List;

public interface RetroService {
    RetroDto create(RetroDto dto);
    RetroDto getById(Long id);
    List<RetroDto> getAll();
    RetroDto update(Long id, RetroDto dto);
    void delete(Long id);

    /**
     * Get complete retro details including all feedback points, discussions,
     * action items, and questions - used for shared retro link view
     */
    RetroDetailDto getRetroDetails(Long retroId);
}

