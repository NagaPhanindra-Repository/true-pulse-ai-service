package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.RetroDto;
import com.codmer.turepulseai.model.RetroDetailDto;
import com.codmer.turepulseai.service.RetroService;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/retros")
@RequiredArgsConstructor
public class RetroController {
    private final RetroService retroService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<RetroDto> create(@RequestBody RetroDto dto) {
        RetroDto created = retroService.create(dto);
        return ResponseEntity.created(URI.create("/api/retros/" + created.getId())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<RetroDto>> list() {
        return ResponseEntity.ok(retroService.getAll());
    }

    /**
     * Get all retros created by the logged-in user
     * Uses the authorization token to identify the user
     *
     * @return List of RetroDto objects created by the logged-in user
     */
    @GetMapping("/my-retros")
    public ResponseEntity<List<RetroDto>> getMyRetros() {
        // Get the username from the security context
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // Find the user by username
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Get retros created by this user
        List<RetroDto> myRetros = retroService.getRetrosByUserId(user.getId());

        return ResponseEntity.ok(myRetros);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RetroDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(retroService.getById(id));
    }

    /**
     * Get complete retro details including all feedback points, discussions,
     * action items, and questions - optimized for shared retro link view
     *
     * @param id - Retro ID
     * @return RetroDetailDto with all nested data
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<RetroDetailDto> getRetroDetails(@PathVariable Long id) {
        RetroDetailDto details = retroService.getRetroDetails(id);
        return ResponseEntity.ok(details);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RetroDto> update(@PathVariable Long id, @RequestBody RetroDto dto) {
        return ResponseEntity.ok(retroService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        retroService.delete(id);
        return ResponseEntity.noContent().build();
    }
}



