package com.codmer.turepulseai.controller;


import com.codmer.turepulseai.model.ChatRequest;
import com.codmer.turepulseai.model.ChatResponse;
import com.codmer.turepulseai.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
}
