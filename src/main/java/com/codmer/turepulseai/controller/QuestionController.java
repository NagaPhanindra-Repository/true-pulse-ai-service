package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.QuestionDto;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<QuestionDto> create(@RequestBody QuestionDto dto) {
        QuestionDto created = questionService.create(dto);
        return ResponseEntity.created(URI.create("/api/questions/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<QuestionDto>> list() {
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

        // Get questions created by this user
        List<QuestionDto> myQuestions = questionService.getQuestionsByUserId();

        return ResponseEntity.ok(myQuestions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionDto> update(@PathVariable Long id, @RequestBody QuestionDto dto) {
        return ResponseEntity.ok(questionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

