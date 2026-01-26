package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.ActionItemDto;
import com.codmer.turepulseai.service.ActionItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/action-items")
@RequiredArgsConstructor
public class ActionItemController {

    private final ActionItemService actionItemService;

    @PostMapping
    public ResponseEntity<ActionItemDto> create(@RequestBody ActionItemDto dto) {
        ActionItemDto created = actionItemService.create(dto);
        return ResponseEntity.created(URI.create("/api/action-items/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ActionItemDto>> list() {
        return ResponseEntity.ok(actionItemService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActionItemDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(actionItemService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActionItemDto> update(@PathVariable Long id, @RequestBody ActionItemDto dto) {
        return ResponseEntity.ok(actionItemService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        actionItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

