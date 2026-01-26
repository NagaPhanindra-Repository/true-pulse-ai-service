package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.QuestionDto;
import com.codmer.turepulseai.entity.Question;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.repository.QuestionRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Override
    public QuestionDto create(QuestionDto dto) {
        User user = fetchUser(dto.getUserId());
        Question q = new Question();
        q.setTitle(dto.getTitle());
        q.setDescription(dto.getDescription());
        q.setUser(user);
        Question saved = questionRepository.save(q);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDto getById(Long id) {
        return questionRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDto> getAll() {
        return questionRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public QuestionDto update(Long id, QuestionDto dto) {
        // Validate that path ID matches dto ID if provided
        if (dto.getId() != null && !id.equals(dto.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path ID and DTO ID do not match");
        }

        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        if (dto.getTitle() != null) q.setTitle(dto.getTitle());
        if (dto.getDescription() != null) q.setDescription(dto.getDescription());

        if (dto.getUserId() != null && (q.getUser() == null || !dto.getUserId().equals(q.getUser().getId()))) {
            User user = fetchUser(dto.getUserId());
            q.setUser(user);
        }

        Question updated = questionRepository.save(q);
        return toDto(updated);
    }

    @Override
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        questionRepository.deleteById(id);
    }

    private User fetchUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
    }

    private QuestionDto toDto(Question q) {
        Long userId = q.getUser() != null ? q.getUser().getId() : null;
        return new QuestionDto(
                q.getId(),
                q.getTitle(),
                q.getDescription(),
                userId,
                q.getCreatedAt(),
                q.getUpdatedAt()
        );
    }
}

