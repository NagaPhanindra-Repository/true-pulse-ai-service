package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.AnswerDto;
import com.codmer.turepulseai.entity.Answer;
import com.codmer.turepulseai.entity.Question;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.repository.AnswerRepository;
import com.codmer.turepulseai.repository.QuestionRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.AnswerService;
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
public class AnswerServiceImpl implements AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Override
    public AnswerDto create(AnswerDto dto) {
        if (dto.getQuestionId() == null || dto.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questionId and userId are required");
        }

        Question question = questionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question not found"));

        User user = userRepository.findByUserName(dto.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        Answer answer = new Answer();
        answer.setContent(dto.getContent());
        answer.setQuestion(question);
        answer.setUser(user);

        Answer saved = answerRepository.save(answer);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AnswerDto getById(Long id) {
        return answerRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnswerDto> getAll() {
        return answerRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnswerDto> getByQuestionId(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        return answerRepository.findByQuestionIdOrderByCreatedAtDesc(questionId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnswerDto> getByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return answerRepository.findByUserId(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public AnswerDto update(Long id, AnswerDto dto) {
        // Validate that path ID matches dto ID if provided
        if (dto.getId() != null && !id.equals(dto.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path ID and DTO ID do not match");
        }

        Answer answer = answerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));

        if (dto.getContent() != null) {
            answer.setContent(dto.getContent());
        }

        if (dto.getQuestionId() != null && (answer.getQuestion() == null || !dto.getQuestionId().equals(answer.getQuestion().getId()))) {
            Question question = questionRepository.findById(dto.getQuestionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question not found"));
            answer.setQuestion(question);
        }

        if (dto.getUserId() != null && (answer.getUser() == null || !dto.getUserId().equals(answer.getUser().getId()))) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            answer.setUser(user);
        }

        Answer updated = answerRepository.save(answer);
        return toDto(updated);
    }

    @Override
    public void delete(Long id) {
        if (!answerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found");
        }
        answerRepository.deleteById(id);
    }

    private AnswerDto toDto(Answer answer) {
        Long questionId = answer.getQuestion() != null ? answer.getQuestion().getId() : null;
        Long userId = answer.getUser() != null ? answer.getUser().getId() : null;
        return new AnswerDto(
                answer.getId(),
                answer.getContent(),
                questionId,
                userId,
                answer.getCreatedAt(),
                answer.getUpdatedAt(),
                answer.getUser() != null ? answer.getUser().getUserName() : null
        );
    }
}

