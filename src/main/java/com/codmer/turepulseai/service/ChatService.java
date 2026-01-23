package com.codmer.turepulseai.service;

import com.codmer.turepulseai.model.ChatRequest;
import com.codmer.turepulseai.model.ChatResponse;
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
}
