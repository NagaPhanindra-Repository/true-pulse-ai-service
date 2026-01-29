package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.FollowedUserQuestionResponse;
import com.codmer.turepulseai.model.QuestionDto;

import java.util.List;

public interface QuestionService {
    QuestionDto create(QuestionDto dto);
    QuestionDto getById(Long id);
    List<QuestionDto> getAll();
    QuestionDto update(Long id, QuestionDto dto);
    void delete(Long id);
    List<QuestionDto> getQuestionsByUserId();

    /**
     * Get all questions posted by users that the logged-in user is following
     * Includes the logged-in user's answer to each question (if they answered)
     * Results are sorted by latest first
     *
     * @return List of FollowedUserQuestionResponse with questions and user's answers
     */
    List<FollowedUserQuestionResponse> getQuestionsFromFollowedUsers();
}

