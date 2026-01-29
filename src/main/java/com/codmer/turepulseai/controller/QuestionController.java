package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.FollowedUserQuestionResponse;
import com.codmer.turepulseai.model.QuestionDto;
import com.codmer.turepulseai.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionDto> create(@RequestBody QuestionDto dto) {
        log.info("Creating question: {}", dto.getTitle());
        QuestionDto created = questionService.create(dto);
        return ResponseEntity.created(URI.create("/api/questions/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<QuestionDto>> list() {
        log.info("Getting all questions");
        return ResponseEntity.ok(questionService.getAll());
    }

    /**
     * Get all questions created by the logged-in user
     * Uses the authorization token to identify the user
     *
     * @return List of QuestionDto objects created by the logged-in user
     */
    @GetMapping("/my-questions")
    public ResponseEntity<List<QuestionDto>> getMyQuestions() {
        log.info("Getting questions created by logged-in user");

        // Get questions created by this user
        List<QuestionDto> myQuestions = questionService.getQuestionsByUserId();

        return ResponseEntity.ok(myQuestions);
    }

    /**
     * Get questions posted by users that the logged-in user is following
     * Includes the logged-in user's answer to each question (if they answered)
     *
     * This endpoint is perfect for:
     * - Feed/timeline showing questions from followed users
     * - Discovering questions to answer
     * - Seeing if you've already answered a question
     * - Understanding who posted the question and when
     *
     * Request: GET /api/questions/feed/followed-users
     * Authorization: Bearer <JWT_TOKEN>
     * No request body, params, or path variables needed
     *
     * Response: List of FollowedUserQuestionResponse
     * - Each response includes:
     *   - Question details (ID, title, description, creator info, creation date)
     *   - Logged-in user's answer (if they answered the question)
     *   - Total answer count
     *   - Results ordered by latest questions first
     *
     * @return List of FollowedUserQuestionResponse with questions and user's answers (null if not answered)
     */
    @GetMapping("/feed/followed-users")
    public ResponseEntity<List<FollowedUserQuestionResponse>> getQuestionsFromFollowedUsers() {
        log.info("Getting questions feed from followed users for logged-in user");

        List<FollowedUserQuestionResponse> questionsFromFollowedUsers =
                questionService.getQuestionsFromFollowedUsers();

        log.info("Found {} questions from followed users", questionsFromFollowedUsers.size());

        return ResponseEntity.ok(questionsFromFollowedUsers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDto> get(@PathVariable Long id) {
        log.info("Getting question by ID: {}", id);
        return ResponseEntity.ok(questionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionDto> update(@PathVariable Long id, @RequestBody QuestionDto dto) {
        log.info("Updating question ID: {}", id);
        return ResponseEntity.ok(questionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting question ID: {}", id);
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

