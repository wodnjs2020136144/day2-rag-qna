package com.skala.day2.lab3.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.skala.day2.lab3.tool.ApprovalTools;
import com.skala.day2.lab3.tool.OrderTools;

@Service
public class Lab3ChatService {

    private final ChatClient chatClient;
    private final OrderTools orderTools;
    private final ApprovalTools approvalTools;

    public Lab3ChatService(
            @Qualifier("lab3AssistantClient") ChatClient chatClient,
            OrderTools orderTools,
            ApprovalTools approvalTools) {

        this.chatClient = chatClient;
        this.orderTools = orderTools;
        this.approvalTools = approvalTools;
    }

    public Map<String, String> chat(
            String question,
            String userId,
            String sessionId) {

        // 대화 ID 생성 규칙은 한 곳에서만 관리한다.
        String conversationId = userId + ":" + sessionId;

        String answer = chatClient.prompt()
                .user(question)

                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        conversationId))

                .tools(orderTools, approvalTools)

                .toolContext(Map.of(
                        "userId", userId,
                        "userMessage", question))

                .call()
                .content();

        return Map.of(
                "answer", answer,
                "conversationId", conversationId);
    }
}
