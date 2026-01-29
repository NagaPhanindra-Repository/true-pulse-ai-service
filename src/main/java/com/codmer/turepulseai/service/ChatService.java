package com.codmer.turepulseai.service;

import com.codmer.turepulseai.entity.Answer;
import com.codmer.turepulseai.entity.Question;
import com.codmer.turepulseai.entity.User;
import com.codmer.turepulseai.model.ChatRequest;
import com.codmer.turepulseai.model.ChatResponse;
import com.codmer.turepulseai.model.QuestionChatRequest;
import com.codmer.turepulseai.model.QuestionChatResponse;
import com.codmer.turepulseai.model.SpecificFeedbackRequest;
import com.codmer.turepulseai.model.SpecificFeedbackResponse;
import com.codmer.turepulseai.model.UserQuestionsAnalysisResponse;
import com.codmer.turepulseai.repository.AnswerRepository;
import com.codmer.turepulseai.repository.QuestionRepository;
import com.codmer.turepulseai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    public String getChatResponse(String question) {
        log.info("Received question: {}", question);
        return chatClient.prompt(question).call().content();
    }

    public ChatResponse chatResponse(ChatRequest request) {
        log.info("Received question: {}", request);

        List<Message> messages = new ArrayList<>();

        // Optional: system prompt to control behavior
        messages.add(new SystemMessage(
                "You are a helpful assistant in a web chat application. " +
                        "Keep responses concise and clear."
        ));


        messages.add(new UserMessage(request.getMessage()));

        var prompt = new Prompt(messages);
        var response = chatClient.prompt(prompt).call();

        String reply = response.content();


        return new ChatResponse(
                reply,
                "model",
                Instant.now().getEpochSecond()
        );
    }

    /**
     * Analyzes a question and its answers using Spring AI
     * Generates a concise 2-3 line summary combining sentiment, likes, dislikes, expectations, and recommendations
     *
     * @param questionId          - The ID of the question
     * @param questionTitle       - The title of the question
     * @param questionDescription - The description/context of the question
     * @param allAnswers          - List of answer contents from followers/beneficiaries
     * @param userMessage         - Optional message from the question creator for contextual guidance
     * @return QuestionChatResponse with concise 2-3 line analysis
     */
    public QuestionChatResponse analyzeQuestionAnswers(Long questionId, String questionTitle,
                                                       String questionDescription,
                                                       List<String> allAnswers,
                                                       String userMessage) {
        log.info("Analyzing question ID: {} with {} answers", questionId, allAnswers.size());

        // Build the analysis prompt with all answers
        StringBuilder answersContent = new StringBuilder();
        for (int i = 0; i < allAnswers.size(); i++) {
            answersContent.append("Answer ").append(i + 1).append(": ").append(allAnswers.get(i)).append("\n\n");
        }

        // System prompt for concise 2-3 line analysis
        String systemPrompt = """
                You are an expert AI analyst specializing in sentiment analysis and feedback interpretation.
                
                Your task: Analyze feedback from followers, customers, beneficiaries, or team members and provide a CONCISE 2-3 LINE SUMMARY.
                
                The summary should synthesize WITHOUT REPETITION:
                - What followers/customers think (general sentiment)
                - What they like most
                - What they dislike or criticize
                - What they expect in future
                - Actionable recommendations
                
                Keep it brief, impactful, and non-repetitive. Use natural flowing sentences. NO numbered sections.
                """;

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        // Build user message with question context and all answers
        StringBuilder userContent = new StringBuilder();
        userContent.append("Question: ").append(questionTitle).append("\n");
        userContent.append("Context: ").append(questionDescription).append("\n\n");
        userContent.append("=== FEEDBACK ===\n\n");
        userContent.append(answersContent);

        if (userMessage != null && !userMessage.trim().isEmpty()) {
            userContent.append("\nFocus: ").append(userMessage).append("\n");
        }

        userContent.append("\nProvide 2-3 lines of analysis that combines sentiment, likes, dislikes, expectations, and recommendations without repetition.");

        messages.add(new UserMessage(userContent.toString()));

        // Call the AI model
        var prompt = new Prompt(messages);
        var response = chatClient.prompt(prompt).call();
        String analysisContent = response.content();

        log.info("Analysis generated successfully for question ID: {}", questionId);

        // Create and return the response object with simplified fields
        QuestionChatResponse qcResponse = new QuestionChatResponse();
        qcResponse.setQuestionId(questionId);
        qcResponse.setQuestionDetails(questionTitle + " - " + questionDescription);
        qcResponse.setAnalysis(analysisContent != null ? analysisContent.trim() : "No analysis available");

        return qcResponse;
    }

    /**
     * Analyzes a specific feedback/comment in context of all answers to a question
     * Determines if the feedback aligns with majority sentiment, is a minority view,
     * or represents a unique perspective. Identifies if it's a commonly disliked aspect.
     * <p>
     * Use Case: When a follower leaves specific feedback, understand how it compares
     * to overall sentiment and whether it's a common complaint or unique viewpoint.
     *
     * @param questionId          - The ID of the question
     * @param questionTitle       - The title of the question
     * @param questionDescription - The description/context
     * @param specificFeedback    - The specific feedback/comment to analyze
     * @param allAnswers          - List of all answer contents for context
     * @return SpecificFeedbackResponse with 3-line analysis
     */
    public SpecificFeedbackResponse analyzeSpecificFeedback(Long questionId, String questionTitle,
                                                            String questionDescription,
                                                            String specificFeedback,
                                                            List<String> allAnswers) {
        log.info("Analyzing specific feedback for question ID: {} with {} total answers", questionId, allAnswers.size());

        // Build all answers context
        StringBuilder answersContext = new StringBuilder();
        for (int i = 0; i < allAnswers.size(); i++) {
            answersContext.append("Feedback ").append(i + 1).append(": ").append(allAnswers.get(i)).append("\n\n");
        }

        // System prompt specifically designed for analyzing specific feedback in context
        String systemPrompt = """
                You are an expert AI analyst specializing in sentiment analysis and comparative feedback analysis.
                
                Your task: Analyze a SPECIFIC piece of feedback in the context of ALL OTHER FEEDBACK received.
                
                Determine:
                1. Does this feedback align with MAJORITY sentiment or is it MINORITY/UNIQUE opinion?
                2. What do MOST followers/customers think about this specific aspect?
                3. Is this a COMMONLY DISLIKED thing, a STRENGTH, or a UNIQUE PERSPECTIVE?
                
                Provide a CREATIVE 3-LINE SUMMARY that:
                - States whether feedback is common, minority, or unique
                - Explains what majority sentiment is about this aspect
                - Identifies if it's a top dislike, strength, or edge case
                
                Be concise, insightful, and avoid repetition. Use natural flowing language.
                """;

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        // Build comprehensive user message
        StringBuilder userContent = new StringBuilder();
        userContent.append("Question: ").append(questionTitle).append("\n");
        userContent.append("Context: ").append(questionDescription).append("\n\n");
        userContent.append("=== SPECIFIC FEEDBACK TO ANALYZE ===\n");
        userContent.append(specificFeedback).append("\n\n");
        userContent.append("=== ALL OTHER FEEDBACK FOR CONTEXT ===\n");
        userContent.append(answersContext);
        userContent.append("\nAnalyze the specific feedback above in context of all other feedback. ");
        userContent.append("Is it a common sentiment, minority view, or unique? Is it a dislike or strength? ");
        userContent.append("Provide 3 lines of creative analysis.");

        messages.add(new UserMessage(userContent.toString()));

        // Call the AI model
        var prompt = new Prompt(messages);
        var response = chatClient.prompt(prompt).call();
        String analysisContent = response.content();

        log.info("Specific feedback analysis generated successfully for question ID: {}", questionId);

        // Create and return the response object
        SpecificFeedbackResponse sfResponse = new SpecificFeedbackResponse();
        sfResponse.setQuestionId(questionId);
        sfResponse.setSpecificFeedback(specificFeedback);
        sfResponse.setAnalysis(analysisContent != null ? analysisContent.trim() : "No analysis available");

        return sfResponse;
    }

    /**
     * Analyzes all questions created by a user with comprehensive insights
     * Generates detailed analysis for each question including:
     * - Executive summary
     * - General sentiment
     * - Most liked/disliked aspects
     * - Future expectations
     * - Actionable recommendations
     *
     * @param questionId          - The ID of the question to analyze
     * @param questionTitle       - The title of the question
     * @param questionDescription - The description/context
     * @param allAnswers          - List of all answer contents
     * @return UserQuestionsAnalysisResponse with comprehensive analysis
     */
    public UserQuestionsAnalysisResponse analyzeUserQuestion(Long questionId, String questionTitle,
                                                             String questionDescription,
                                                             List<String> allAnswers) {
        log.info("Analyzing user question ID: {} with {} answers", questionId, allAnswers.size());

        // Build all answers context
        StringBuilder answersContext = new StringBuilder();
        for (int i = 0; i < allAnswers.size(); i++) {
            answersContext.append("Answer ").append(i + 1).append(": ").append(allAnswers.get(i)).append("\n\n");
        }

        // System prompt for comprehensive question analysis
        String systemPrompt = """
                You are an expert AI analyst specializing in comprehensive feedback analysis and business intelligence.
                
                Your task: Analyze ALL answers/feedback to a question and provide DETAILED insights across multiple dimensions.
                
                Provide analysis in these specific categories:
                1. EXECUTIVE SUMMARY (3 lines): High-level overview of key findings
                2. GENERAL SENTIMENT: Overall sentiment with satisfaction %, excitement level, competitive position
                3. MOST LIKED ASPECTS: What followers appreciate most - strengths and positive highlights
                4. MOST DISLIKED ASPECTS: Common complaints, concerns, areas needing improvement
                5. FUTURE EXPECTATIONS: What followers hope to see, expect in future iterations
                6. RECOMMENDATIONS: Strategic actionable recommendations based on feedback
                
                Be specific, insightful, data-driven where possible (mention percentages/counts), and actionable.
                Use clear, professional language suitable for executives and decision-makers.
                """;

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        // Build comprehensive user message
        StringBuilder userContent = new StringBuilder();
        userContent.append("Question: ").append(questionTitle).append("\n");
        userContent.append("Context: ").append(questionDescription).append("\n");
        userContent.append("Total Answers Received: ").append(allAnswers.size()).append("\n\n");
        userContent.append("=== ALL FEEDBACK/ANSWERS ===\n\n");
        userContent.append(answersContext);
        userContent.append("\nProvide comprehensive analysis covering:\n");
        userContent.append("1. Executive Summary (3 lines)\n");
        userContent.append("2. General Sentiment (with satisfaction % if determinable)\n");
        userContent.append("3. Most Liked Aspects\n");
        userContent.append("4. Most Disliked Aspects\n");
        userContent.append("5. Future Expectations\n");
        userContent.append("6. Recommendations\n\n");
        userContent.append("Format each section clearly with the section name followed by analysis.");

        messages.add(new UserMessage(userContent.toString()));

        // Call the AI model
        var prompt = new Prompt(messages);
        var response = chatClient.prompt(prompt).call();
        String analysisContent = response.content();

        log.info("Comprehensive analysis generated for question ID: {}", questionId);

        // Parse the AI response into structured sections
        UserQuestionsAnalysisResponse analysisResponse = parseComprehensiveAnalysis(
                analysisContent, questionId, questionTitle, questionDescription, allAnswers.size());

        return analysisResponse;
    }

    /**
     * Parses AI-generated comprehensive analysis into structured response
     * Extracts different sections and formats them appropriately
     */
    private UserQuestionsAnalysisResponse parseComprehensiveAnalysis(String analysisContent,
                                                                     Long questionId,
                                                                     String questionTitle,
                                                                     String questionDescription,
                                                                     int totalAnswers) {
        UserQuestionsAnalysisResponse response = new UserQuestionsAnalysisResponse();
        response.setQuestionId(questionId);
        response.setQuestionTitle(questionTitle);
        response.setQuestionDescription(questionDescription);
        response.setTotalAnswers(totalAnswers);
        response.setModel("Spring AI");
        response.setCreatedAt(Instant.now().getEpochSecond());

        // Parse sections from AI response
        String executiveSummary = extractSection(analysisContent, "Executive Summary", "General Sentiment");
        String generalSentiment = extractSection(analysisContent, "General Sentiment", "Most Liked Aspects");
        String mostLiked = extractSection(analysisContent, "Most Liked Aspects", "Most Disliked Aspects");
        String mostDisliked = extractSection(analysisContent, "Most Disliked Aspects", "Future Expectations");
        String futureExpectations = extractSection(analysisContent, "Future Expectations", "Recommendations");
        String recommendations = extractSection(analysisContent, "Recommendations", null);

        // Set all fields with fallbacks
        response.setExecutiveSummary(executiveSummary != null ? executiveSummary :
                "Comprehensive analysis of feedback received.");
        response.setGeneralSentiment(generalSentiment != null ? generalSentiment :
                "Overall sentiment analysis based on " + totalAnswers + " responses.");
        response.setMostLikedAspects(mostLiked != null ? mostLiked :
                "Positive aspects highlighted by respondents.");
        response.setMostDislikedAspects(mostDisliked != null ? mostDisliked :
                "Areas of concern mentioned in feedback.");
        response.setFutureExpectations(futureExpectations != null ? futureExpectations :
                "Expectations for future improvements.");
        response.setRecommendations(recommendations != null ? recommendations :
                "Strategic recommendations based on analysis.");

        return response;
    }

    /**
     * Extracts a specific section from AI response
     * Looks for section markers and extracts content between them
     */
    private String extractSection(String content, String sectionName, String nextSectionName) {
        try {
            // Look for various possible section markers
            String[] possibleMarkers = {
                    sectionName + ":",
                    sectionName.toUpperCase() + ":",
                    "**" + sectionName + "**",
                    sectionName,
                    sectionName.toLowerCase()
            };

            int startIndex = -1;
            for (String marker : possibleMarkers) {
                startIndex = content.indexOf(marker);
                if (startIndex != -1) {
                    startIndex = startIndex + marker.length();
                    break;
                }
            }

            if (startIndex == -1) {
                return null;
            }

            int endIndex;
            if (nextSectionName != null) {
                // Find the next section
                String[] nextMarkers = {
                        nextSectionName + ":",
                        nextSectionName.toUpperCase() + ":",
                        "**" + nextSectionName + "**",
                        "\n" + nextSectionName,
                        nextSectionName.toLowerCase()
                };

                endIndex = content.length();
                for (String marker : nextMarkers) {
                    int tempIndex = content.indexOf(marker, startIndex);
                    if (tempIndex != -1 && tempIndex < endIndex) {
                        endIndex = tempIndex;
                    }
                }
            } else {
                endIndex = content.length();
            }

            String extracted = content.substring(startIndex, endIndex).trim();
            // Remove markdown formatting and clean up
            extracted = extracted.replaceAll("\\*\\*", "").replaceAll("\\n\\n+", "\n").trim();
            return extracted;

        } catch (Exception e) {
            log.warn("Failed to extract section: {}", sectionName, e);
            return null;
        }
    }

    /**
     * Analyzes a question and its answers using Spring AI (with repository logic)
     * Fetches question and answers from database, then performs AI analysis
     *
     * @param questionChatRequest - Contains question ID and optional guidance message
     * @return QuestionChatResponse with comprehensive analysis
     */
    public QuestionChatResponse analyzeQuestionAnswersFromRequest(QuestionChatRequest questionChatRequest) {
        log.info("Analyzing question from request - Question ID: {}", questionChatRequest.getQuestionId());

        // Validate question ID
        if (questionChatRequest.getQuestionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question ID is required");
        }

        // Fetch the question from database
        Question question = questionRepository.findById(questionChatRequest.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        // Fetch all answers for this question
        List<Answer> answers = answerRepository.findByQuestionIdOrderByCreatedAtDesc(question.getId());

        log.info("Found {} answers for question ID: {}", answers.size(), question.getId());

        // If no answers, return appropriate message
        if (answers.isEmpty()) {
            log.warn("No answers found for question ID: {}", question.getId());
            QuestionChatResponse emptyResponse = new QuestionChatResponse();
            emptyResponse.setQuestionId(question.getId());
            emptyResponse.setQuestionDetails(question.getTitle() + " - " + question.getDescription());
            emptyResponse.setAnalysis("No answers received yet. Check back soon for feedback from followers.");
            return emptyResponse;
        }

        // Extract answer contents
        List<String> answerContents = answers.stream()
                .map(Answer::getContent)
                .collect(Collectors.toList());

        // Call existing AI analysis method
        return analyzeQuestionAnswers(
                question.getId(),
                question.getTitle(),
                question.getDescription(),
                answerContents,
                questionChatRequest.getMessage()
        );
    }

    /**
     * Analyzes specific feedback in context of all answers (with repository logic)
     * Fetches question and answers from database, then performs comparative analysis
     *
     * @param specificFeedbackRequest - Contains question ID and specific feedback
     * @return SpecificFeedbackResponse with contextual analysis
     */
    public SpecificFeedbackResponse analyzeSpecificFeedbackFromRequest(SpecificFeedbackRequest specificFeedbackRequest) {
        log.info("Analyzing specific feedback from request - Question ID: {}", specificFeedbackRequest.getQuestionId());

        // Validate inputs
        if (specificFeedbackRequest.getQuestionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question ID is required");
        }
        if (specificFeedbackRequest.getSpecificFeedback() == null ||
                specificFeedbackRequest.getSpecificFeedback().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specific feedback is required");
        }

        // Fetch the question from database
        Question question = questionRepository.findById(specificFeedbackRequest.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        // Fetch all answers for context
        List<Answer> answers = answerRepository.findByQuestionIdOrderByCreatedAtDesc(question.getId());

        log.info("Found {} answers for question ID: {}", answers.size(), question.getId());

        // If no answers, return appropriate message
        if (answers.isEmpty()) {
            log.warn("No answers found for question ID: {}", question.getId());
            SpecificFeedbackResponse emptyResponse = new SpecificFeedbackResponse();
            emptyResponse.setQuestionId(question.getId());
            emptyResponse.setSpecificFeedback(specificFeedbackRequest.getSpecificFeedback());
            emptyResponse.setAnalysis("Cannot determine if this feedback aligns with majority sentiment " +
                    "as no other answers/feedback have been received yet. This appears to be an initial or " +
                    "unique perspective. Await more feedback to contextualize this comment.");
            return emptyResponse;
        }

        // Extract answer contents
        List<String> answerContents = answers.stream()
                .map(Answer::getContent)
                .collect(Collectors.toList());

        // Call existing AI analysis method
        return analyzeSpecificFeedback(
                question.getId(),
                question.getTitle(),
                question.getDescription(),
                specificFeedbackRequest.getSpecificFeedback(),
                answerContents
        );
    }

    /**
     * Analyzes all questions created by logged-in user (with repository logic)
     * Fetches user from security context, gets their questions and answers, performs comprehensive analysis
     *
     * @return List of UserQuestionsAnalysisResponse with comprehensive analysis per question
     */
    public List<UserQuestionsAnalysisResponse> analyzeMyQuestionsForLoggedInUser() {
        log.info("Analyzing all questions for logged-in user");

        // Get the username from security context
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // Find the user by username
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        log.info("Analyzing questions for user: {} (ID: {})", username, user.getId());

        // Fetch all questions created by this user
        List<Question> userQuestions = questionRepository.findByUserId(user.getId());

        log.info("Found {} questions created by user ID: {}", userQuestions.size(), user.getId());

        // If no questions, return empty list
        if (userQuestions.isEmpty()) {
            log.info("No questions found for user ID: {}", user.getId());
            return new ArrayList<>();
        }

        // Analyze each question
        List<UserQuestionsAnalysisResponse> analysisResults = new ArrayList<>();

        for (Question question : userQuestions) {
            log.info("Analyzing question ID: {} - '{}'", question.getId(), question.getTitle());

            // Fetch all answers for this question
            List<Answer> answers = answerRepository.findByQuestionIdOrderByCreatedAtDesc(question.getId());

            log.info("Found {} answers for question ID: {}", answers.size(), question.getId());

            // Handle questions with no answers
            if (answers.isEmpty()) {
                log.warn("No answers found for question ID: {} - creating placeholder response", question.getId());
                UserQuestionsAnalysisResponse placeholderResponse = createPlaceholderResponse(question);
                analysisResults.add(placeholderResponse);
                continue;
            }

            // Extract answer contents
            List<String> answerContents = answers.stream()
                    .map(Answer::getContent)
                    .collect(Collectors.toList());

            // Perform AI analysis
            try {
                UserQuestionsAnalysisResponse analysisResponse = analyzeUserQuestion(
                        question.getId(),
                        question.getTitle(),
                        question.getDescription(),
                        answerContents
                );

                analysisResults.add(analysisResponse);
                log.info("Successfully analyzed question ID: {}", question.getId());

            } catch (Exception e) {
                log.error("Error analyzing question ID: {}", question.getId(), e);
                UserQuestionsAnalysisResponse errorResponse = createErrorResponse(question, answers.size());
                analysisResults.add(errorResponse);
            }
        }

        log.info("Completed analysis of {} questions for user ID: {}", analysisResults.size(), user.getId());

        return analysisResults;
    }

    /**
     * Creates a placeholder response for questions with no answers
     */
    private UserQuestionsAnalysisResponse createPlaceholderResponse(Question question) {
        UserQuestionsAnalysisResponse placeholderResponse = new UserQuestionsAnalysisResponse();
        placeholderResponse.setQuestionId(question.getId());
        placeholderResponse.setQuestionTitle(question.getTitle());
        placeholderResponse.setQuestionDescription(question.getDescription());
        placeholderResponse.setTotalAnswers(0);
        placeholderResponse.setExecutiveSummary("No answers received yet for this question.\nCheck back soon as followers start providing feedback.\nConsider sharing the question with your audience to gather insights.");
        placeholderResponse.setGeneralSentiment("No feedback available yet to determine sentiment.");
        placeholderResponse.setMostLikedAspects("Not applicable - no answers received yet.");
        placeholderResponse.setMostDislikedAspects("Not applicable - no answers received yet.");
        placeholderResponse.setFutureExpectations("Not applicable - no answers received yet.");
        placeholderResponse.setRecommendations("Promote this question to your followers to gather feedback. Consider sharing via social media or email campaigns.");
        placeholderResponse.setModel("Spring AI");
        placeholderResponse.setCreatedAt(Instant.now().getEpochSecond());
        return placeholderResponse;
    }

    /**
     * Creates an error response when analysis fails
     */
    private UserQuestionsAnalysisResponse createErrorResponse(Question question, int answerCount) {
        UserQuestionsAnalysisResponse errorResponse = new UserQuestionsAnalysisResponse();
        errorResponse.setQuestionId(question.getId());
        errorResponse.setQuestionTitle(question.getTitle());
        errorResponse.setQuestionDescription(question.getDescription());
        errorResponse.setTotalAnswers(answerCount);
        errorResponse.setExecutiveSummary("Analysis temporarily unavailable due to processing error.");
        errorResponse.setGeneralSentiment("Unable to generate sentiment analysis at this time.");
        errorResponse.setMostLikedAspects("Analysis unavailable.");
        errorResponse.setMostDislikedAspects("Analysis unavailable.");
        errorResponse.setFutureExpectations("Analysis unavailable.");
        errorResponse.setRecommendations("Please try again later or contact support.");
        errorResponse.setModel("Spring AI");
        errorResponse.setCreatedAt(Instant.now().getEpochSecond());
        return errorResponse;
    }
}
    // ...existing analyzeQuestionAnswers method...
    // ...existing analyzeSpecificFeedback method...
    // ...existing analyzeUserQuestion method...
