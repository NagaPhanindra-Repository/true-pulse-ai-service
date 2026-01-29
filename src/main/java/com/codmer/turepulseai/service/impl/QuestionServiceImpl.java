package com.codmer.turepulseai.service.impl;

import com.codmer.turepulseai.model.FollowedUserQuestionResponse;
import com.codmer.turepulseai.model.QuestionDto;
import com.codmer.turepulseai.entity.Answer;
import com.codmer.turepulseai.entity.Question;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.entity.UserFollower;
import com.codmer.turepulseai.repository.AnswerRepository;
import com.codmer.turepulseai.repository.QuestionRepository;
import com.codmer.turepulseai.repository.UserFollowerRepository;
import com.codmer.turepulseai.repository.UserRepository;
import com.codmer.turepulseai.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final UserFollowerRepository userFollowerRepository;
    private final AnswerRepository answerRepository;

    @Override
    public QuestionDto create(QuestionDto dto) {
        User user = fetchUser();
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
            User user = fetchUser();
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

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDto> getQuestionsByUserId() {
        User user = fetchUser();
        return questionRepository.findByUserId(user.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowedUserQuestionResponse> getQuestionsFromFollowedUsers() {
        log.info("Fetching questions from followed users for logged-in user");

        // Get logged-in user
        User loggedInUser = fetchUser();
        String loggedInUsername = loggedInUser.getUserName();
        Long loggedInUserId = loggedInUser.getId();

        log.debug("Logged-in user: {} (ID: {})", loggedInUsername, loggedInUserId);

        // Get all users that logged-in user is following
        List<UserFollower> following = userFollowerRepository.findByFollowerUsername(loggedInUsername);

        log.debug("User {} follows {} users", loggedInUsername, following.size());

        if (following.isEmpty()) {
            log.info("User {} is not following anyone", loggedInUsername);
            return List.of();
        }

        // Extract user IDs being followed
        List<Long> followedUserIds = following.stream()
                .map(uf -> {
                    // Get the user ID from the username
                    User followedUser = userRepository.findByUserName(uf.getUserUsername())
                            .orElse(null);
                    return followedUser != null ? followedUser.getId() : null;
                })
                .filter(id -> id != null)
                .collect(Collectors.toList());

        log.debug("Followed user IDs: {}", followedUserIds);

        if (followedUserIds.isEmpty()) {
            log.info("No valid followed users found for {}", loggedInUsername);
            return List.of();
        }

        // Get all questions posted by followed users (latest first)
        List<Question> questions = questionRepository.findByUserIdInOrderByCreatedAtDesc(followedUserIds);

        log.debug("Found {} questions from followed users", questions.size());

        // Build response DTOs with logged-in user's answers
        return questions.stream()
                .map(question -> buildFollowedUserQuestionResponse(question, loggedInUserId))
                .collect(Collectors.toList());
    }

    /**
     * Build the response DTO for a question from a followed user
     * Includes the logged-in user's answer if they answered the question
     *
     * @param question - The question entity
     * @param loggedInUserId - The logged-in user's ID
     * @return FollowedUserQuestionResponse with question and user's answer (if exists)
     */
    private FollowedUserQuestionResponse buildFollowedUserQuestionResponse(Question question, Long loggedInUserId) {
        log.debug("Building response for question ID: {}", question.getId());

        // Get the question creator
        User questionCreator = question.getUser();

        // Try to find logged-in user's answer to this question
        Optional<Answer> userAnswerOpt = answerRepository.findByQuestionIdAndUserId(question.getId(), loggedInUserId);

        // Build logged-in user's answer DTO (only if they answered)
        FollowedUserQuestionResponse.LoggedInUserAnswerDto userAnswerDto = null;
        if (userAnswerOpt.isPresent()) {
            Answer userAnswer = userAnswerOpt.get();
            userAnswerDto = FollowedUserQuestionResponse.LoggedInUserAnswerDto.builder()
                    .answerId(userAnswer.getId())
                    .answerContent(userAnswer.getContent())
                    .answerCreatedAt(userAnswer.getCreatedAt())
                    .answerUpdatedAt(userAnswer.getUpdatedAt())
                    .build();
            log.debug("Found user's answer to question ID: {}", question.getId());
        } else {
            log.debug("User has not answered question ID: {}", question.getId());
        }

        // Get total answers count for the question
        long totalAnswersCount = answerRepository.countByQuestionId(question.getId());

        // Build and return response
        return FollowedUserQuestionResponse.builder()
                .questionId(question.getId())
                .questionTitle(question.getTitle())
                .questionDescription(question.getDescription())
                .questionCreatorUserId(questionCreator.getId())
                .questionCreatorUsername(questionCreator.getUserName())
                .questionCreatorFirstName(questionCreator.getFirstName())
                .questionCreatorLastName(questionCreator.getLastName())
                .questionCreatedAt(question.getCreatedAt())
                .loggedInUserAnswer(userAnswerDto) // NULL if user hasn't answered
                .totalAnswersCount(totalAnswersCount)
                .build();
    }

    private User fetchUser() {
        // Get the username from the security context
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // Find the user by username
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));


        if (user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return userRepository.findById(user.getId())
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

