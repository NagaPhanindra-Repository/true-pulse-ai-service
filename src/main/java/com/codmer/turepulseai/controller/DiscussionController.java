package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.DiscussionDto;
import com.codmer.turepulseai.service.DiscussionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/public/discussions")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;

    @PostMapping
    public ResponseEntity<DiscussionDto> create(@RequestBody DiscussionDto dto) {
        DiscussionDto created = discussionService.create(dto);
        return ResponseEntity.created(URI.create("/api/discussions/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<DiscussionDto>> list() {
        return ResponseEntity.ok(discussionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscussionDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(discussionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscussionDto> update(@PathVariable Long id, @RequestBody DiscussionDto dto) {
        return ResponseEntity.ok(discussionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        discussionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

