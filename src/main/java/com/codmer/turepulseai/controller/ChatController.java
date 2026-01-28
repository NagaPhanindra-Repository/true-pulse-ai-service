package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.entity.Answer;
import com.codmer.turepulseai.entity.Question;
import com.codmer.turepulseai.model.ChatRequest;
import com.codmer.turepulseai.model.ChatResponse;
import com.codmer.turepulseai.model.QuestionChatRequest;
import com.codmer.turepulseai.model.QuestionChatResponse;
import com.codmer.turepulseai.repository.AnswerRepository;
import com.codmer.turepulseai.repository.QuestionRepository;
import com.codmer.turepulseai.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class ChatController {
    private final ChatService chatService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @GetMapping("/chat/{question}")
    public String getChatResponse(@PathVariable String question){
        return chatService.getChatResponse(question);
    }

    @PostMapping("/chat")
    public ChatResponse chatResponse(@RequestBody ChatRequest request){
        return chatService.chatResponse(request);
    }

    /**
     * Analyzes a question and all its answers using Spring AI
     * Generates intelligent insights about what followers think, expect, and criticize
     *
     * This is ideal for:
     * - Restaurants analyzing feedback on new menu items
     * - Businesses analyzing customer reviews on new products
     * - Leaders/Managers analyzing team feedback on policies or reviews
     * - Celebrities analyzing fan feedback on new content, outfit, speech
     * - Politicians analyzing citizen feedback on new policies
     * - Bureaucrats analyzing stakeholder feedback on initiatives
     *
     * @param questionChatRequest - Contains question ID and optional guidance message
     * @return QuestionChatResponse with comprehensive analysis and 3-line summary
     */
    @PostMapping("/chat/question/analyze")
    public ResponseEntity<QuestionChatResponse> analyzeQuestionAnswers(
            @RequestBody QuestionChatRequest questionChatRequest) {

        log.info("Received request to analyze question ID: {}", questionChatRequest.getQuestionId());

        // Validate question ID
        if (questionChatRequest.getQuestionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question ID is required");
        }

        // Fetch the question from database
        Question question = questionRepository.findById(questionChatRequest.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        // Fetch all answers for this question, ordered by creation date (most recent first)
        List<Answer> answers = answerRepository.findByQuestionIdOrderByCreatedAtDesc(question.getId());

        log.info("Found {} answers for question ID: {}", answers.size(), question.getId());

        // If no answers, still provide response but with appropriate message
        if (answers.isEmpty()) {
            log.warn("No answers found for question ID: {}", question.getId());
            QuestionChatResponse emptyResponse = new QuestionChatResponse();
            emptyResponse.setQuestionId(question.getId());
            emptyResponse.setQuestionDetails(question.getTitle() + " - " + question.getDescription());
            emptyResponse.setAnalysis("No answers received yet. Check back soon for feedback from followers.");
            return ResponseEntity.ok(emptyResponse);
        }

        // Extract answer contents for AI analysis
        List<String> answerContents = answers.stream()
                .map(Answer::getContent)
                .collect(Collectors.toList());

        // Call ChatService to perform AI analysis
        QuestionChatResponse analysisResponse = chatService.analyzeQuestionAnswers(
                question.getId(),                question.getTitle(),
                question.getDescription(),
                answerContents,
                questionChatRequest.getMessage()
        );

        return ResponseEntity.ok(analysisResponse);
    }
}
