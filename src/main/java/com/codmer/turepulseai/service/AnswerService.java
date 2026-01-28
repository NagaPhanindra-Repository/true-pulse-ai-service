package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.AnswerDto;

import java.util.List;

public interface AnswerService {
    AnswerDto create(AnswerDto dto);
    AnswerDto getById(Long id);
    List<AnswerDto> getAll();
    List<AnswerDto> getByQuestionId(Long questionId);
    List<AnswerDto> getByUserId(Long userId);
    AnswerDto update(Long id, AnswerDto dto);
    void delete(Long id);
}

