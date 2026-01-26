package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.QuestionDto;
import com.codmer.turepulseai.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionDto> create(@RequestBody QuestionDto dto) {
        QuestionDto created = questionService.create(dto);
        return ResponseEntity.created(URI.create("/api/questions/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<QuestionDto>> list() {
        return ResponseEntity.ok(questionService.getAll());
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

