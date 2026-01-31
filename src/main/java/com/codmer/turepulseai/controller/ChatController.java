package com.codmer.turepulseai.controller;

import com.codmer.turepulseai.model.ChatRequest;
import com.codmer.turepulseai.model.ChatResponse;
import com.codmer.turepulseai.model.QuestionChatRequest;
import com.codmer.turepulseai.model.QuestionChatResponse;
import com.codmer.turepulseai.model.SpecificFeedbackRequest;
import com.codmer.turepulseai.model.SpecificFeedbackResponse;
import com.codmer.turepulseai.model.UserQuestionsAnalysisResponse;
import com.codmer.turepulseai.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class ChatController {
    private final ChatService chatService;

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

        // Delegate to service layer which handles all repository logic
        QuestionChatResponse analysisResponse = chatService.analyzeQuestionAnswersFromRequest(questionChatRequest);

        return ResponseEntity.ok(analysisResponse);
    }

    /**
     * Analyzes a specific feedback/comment in context of all answers to determine alignment
     * with majority sentiment, identifies if it's a commonly disliked aspect, or unique perspective
     *
     * Use Case: Understand how a specific follower's comment compares to overall sentiment.
     * Useful for identifying common complaints, unique insights, minority views, or strengths.
     *
     * Examples:
     * - A customer says "pricing is too high" - is this a common complaint?
     * - A fan says "new outfit is bold" - is this majority opinion or unique?
     * - A team member says "communication needs improvement" - is this widespread concern?
     *
     * @param specificFeedbackRequest - Contains question ID and specific feedback to analyze
     * @return SpecificFeedbackResponse with 3-line contextual analysis
     */
    @PostMapping("/chat/specific-feedback/analyze")
    public ResponseEntity<SpecificFeedbackResponse> analyzeSpecificFeedback(
            @RequestBody SpecificFeedbackRequest specificFeedbackRequest) {

        log.info("Received request to analyze specific feedback for question ID: {}",
                specificFeedbackRequest.getQuestionId());

        // Delegate to service layer which handles all repository logic
        SpecificFeedbackResponse analysisResponse = chatService.analyzeSpecificFeedbackFromRequest(specificFeedbackRequest);

        return ResponseEntity.ok(analysisResponse);
    }

    /**
     * Analyzes all questions created by the logged-in user with comprehensive insights
     * Provides detailed analysis for each question including:
     * - Executive summary
     * - General sentiment with satisfaction percentage
     * - Most liked aspects (strengths)
     * - Most disliked aspects (concerns)
     * - Future expectations from followers
     * - Actionable recommendations
     *
     * This endpoint is ideal for:
     * - Business owners reviewing all customer feedback questions
     * - Celebrities analyzing all fan engagement questions
     * - Politicians reviewing citizen feedback across multiple topics
     * - Team leaders analyzing all team feedback questions
     * - Restaurant managers reviewing all menu feedback questions
     *
     * @return List of UserQuestionsAnalysisResponse with comprehensive analysis per question
     */
    @GetMapping("/chat/my-questions/analyze")
    public ResponseEntity<List<UserQuestionsAnalysisResponse>> analyzeMyQuestions() {

        log.info("Received request to analyze all questions for logged-in user");

        // Delegate to service layer which handles all repository logic and user authentication
        List<UserQuestionsAnalysisResponse> analysisResults = chatService.analyzeMyQuestionsForLoggedInUser();

        return ResponseEntity.ok(analysisResults);
    }



}
