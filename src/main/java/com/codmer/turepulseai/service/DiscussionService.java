package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.DiscussionDto;

import java.util.List;

public interface DiscussionService {
    DiscussionDto create(DiscussionDto dto);
    DiscussionDto getById(Long id);
    List<DiscussionDto> getAll();
    DiscussionDto update(Long id, DiscussionDto dto);
    void delete(Long id);
}

