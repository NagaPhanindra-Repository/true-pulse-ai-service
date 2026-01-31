package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.FeedbackPointAnalysisRequest;
import com.codmer.turepulseai.model.FeedbackPointAnalysisResponse;
import com.codmer.turepulseai.model.FeedbackPointDto;
import com.codmer.turepulseai.service.FeedbackPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/feedback-points")
@RequiredArgsConstructor
public class FeedbackPointController {

    private final FeedbackPointService feedbackPointService;

    @PostMapping
    public ResponseEntity<FeedbackPointDto> create(@RequestBody FeedbackPointDto dto) {
        FeedbackPointDto created = feedbackPointService.create(dto);
        return ResponseEntity.created(URI.create("/api/feedback-points/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<FeedbackPointDto>> list() {
        return ResponseEntity.ok(feedbackPointService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackPointDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackPointService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeedbackPointDto> update(@PathVariable Long id, @RequestBody FeedbackPointDto dto) {
        return ResponseEntity.ok(feedbackPointService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feedbackPointService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/analysis")
    public ResponseEntity<FeedbackPointAnalysisResponse> analyze(@RequestBody FeedbackPointAnalysisRequest request) {
        return ResponseEntity.ok(feedbackPointService.analyzeFeedbackPoint(request));
    }
}
