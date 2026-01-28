package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.AnswerDto;
import com.codmer.turepulseai.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping
    public ResponseEntity<AnswerDto> create(@RequestBody AnswerDto dto) {
        AnswerDto created = answerService.create(dto);
        return ResponseEntity.created(URI.create("/api/answers/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<AnswerDto>> list() {
        return ResponseEntity.ok(answerService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnswerDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(answerService.getById(id));
    }

    @GetMapping("/question/{questionId}")
    public ResponseEntity<List<AnswerDto>> getByQuestionId(@PathVariable Long questionId) {
        return ResponseEntity.ok(answerService.getByQuestionId(questionId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AnswerDto>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(answerService.getByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnswerDto> update(@PathVariable Long id, @RequestBody AnswerDto dto) {
        return ResponseEntity.ok(answerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        answerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

