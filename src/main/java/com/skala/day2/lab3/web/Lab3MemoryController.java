package com.skala.day2.lab3.web;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Day 3 · Memory Debug")
public class Lab3MemoryController {

    private final ChatMemory chatMemory;

    public Lab3MemoryController(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @GetMapping("/lab3/memory")
    @Operation(summary = "대화 메모리 직접 확인")
    public List<Map<String, String>> memory(
            @RequestParam String userId,
            @RequestParam String sessionId) {

        String conversationId = userId + ":" + sessionId;

        return chatMemory.get(conversationId).stream()
                .map(message -> Map.of(
                        "type", message.getMessageType().name(),
                        "text", message.getText()))
                .toList();
    }
}
