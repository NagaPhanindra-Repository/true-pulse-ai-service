package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.QuestionDto;

import java.util.List;

public interface QuestionService {
    QuestionDto create(QuestionDto dto);
    QuestionDto getById(Long id);
    List<QuestionDto> getAll();
    QuestionDto update(Long id, QuestionDto dto);
    void delete(Long id);
}

