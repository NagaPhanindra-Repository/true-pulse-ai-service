package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.ChatRequest;
import com.codmer.turepulseai.model.ChatResponse;
import com.codmer.turepulseai.model.QuestionChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

    private final ChatClient chatClient;

    public String getChatResponse(String question){
        log.info("Received question: {}", question);
        return chatClient.prompt(question).call().content();
    }

    public ChatResponse chatResponse(ChatRequest request){
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
     * @param questionId - The ID of the question
     * @param questionTitle - The title of the question
     * @param questionDescription - The description/context of the question
     * @param allAnswers - List of answer contents from followers/beneficiaries
     * @param userMessage - Optional message from the question creator for contextual guidance
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
}
