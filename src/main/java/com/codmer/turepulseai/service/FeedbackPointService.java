package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.FeedbackPointDto;

import java.util.List;

public interface FeedbackPointService {
    FeedbackPointDto create(FeedbackPointDto dto);
    FeedbackPointDto getById(Long id);
    List<FeedbackPointDto> getAll();
    FeedbackPointDto update(Long id, FeedbackPointDto dto);
    void delete(Long id);
}

