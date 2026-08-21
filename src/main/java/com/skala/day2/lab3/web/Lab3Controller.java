package com.skala.day2.lab3.web;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.day2.lab3.service.Lab3ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Day 3 · Tool Calling")
public class Lab3Controller {

    private final Lab3ChatService chatService;

    public Lab3Controller(Lab3ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/lab3/chat")
    @Operation(
            summary = "멀티턴 주문 상담",
            description = "userId + sessionId를 대화 ID로 사용하고 userId는 ToolContext로 전달한다.")
    public Map<String, String> chat(
            @RequestParam String q,
            @RequestParam(defaultValue = "user1") String userId,
            @RequestParam(defaultValue = "session1") String sessionId) {

        return chatService.chat(q, userId, sessionId);
    }
}
