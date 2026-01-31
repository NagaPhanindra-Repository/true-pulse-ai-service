package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.RetroDto;
import com.codmer.turepulseai.model.RetroDetailDto;
import com.codmer.turepulseai.model.RetroAnalysisResponse;

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

    /**
     * Get all retros created by a specific user
     */
    List<RetroDto> getRetrosByUserId(Long userId);

    /**
     * Analyze retro using AI to provide insights and suggestions
     */
    RetroAnalysisResponse analyzeRetro(Long retroId);
}
