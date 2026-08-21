package com.skala.day2.helpdesk.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.skala.day2.helpdesk.domain.ChatRequest;
import com.skala.day2.helpdesk.domain.ChatResponse;
import com.skala.day2.helpdesk.finance.tool.TransactionTools;
import com.skala.day2.helpdesk.semiconductor.tool.EquipmentTools;

@Service
public class HelpDeskService {

    private final ChatClient chatClient;
    private final EquipmentTools equipmentTools;
    private final TransactionTools transactionTools;

    public HelpDeskService(
            @Qualifier("helpDeskClient") ChatClient chatClient,
            EquipmentTools equipmentTools,
            TransactionTools transactionTools) {

        this.chatClient = chatClient;
        this.equipmentTools = equipmentTools;
        this.transactionTools = transactionTools;
    }

    public ChatResponse chat(ChatRequest request) {

        if (request.domain() == null) {
            throw new IllegalArgumentException("domain은 필수입니다.");
        }

        if (request.question() == null || request.question().isBlank()) {
            throw new IllegalArgumentException("question은 필수입니다.");
        }

        if (request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }

        if (request.sessionId() == null || request.sessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId는 필수입니다.");
        }

        // 기존 Day3와 같은 conversationId 규칙을 사용한다.
        String conversationId =
                request.userId() + ":" + request.sessionId();

        String answer;

        switch (request.domain()) {

            case SEMICONDUCTOR -> answer =
                    chatClient.prompt()
                            .user(request.question())
                            .advisors(a -> a.param(
                                    ChatMemory.CONVERSATION_ID,
                                    conversationId))
                            .tools(equipmentTools)
                            .toolContext(Map.of(
                                    "userId", request.userId(),
                                    "userMessage", request.question(),
                                    "domain", request.domain().name()))
                            .call()
                            .content();

            case FINANCE -> answer =
                    chatClient.prompt()
                            .user(request.question())
                            .advisors(a -> a.param(
                                    ChatMemory.CONVERSATION_ID,
                                    conversationId))
                            .tools(transactionTools)
                            .toolContext(Map.of(
                                    "userId", request.userId(),
                                    "userMessage", request.question(),
                                    "domain", request.domain().name()))
                            .call()
                            .content();

            default -> throw new IllegalArgumentException(
                    "지원하지 않는 domain입니다.");
        }

        return new ChatResponse(
                request.domain(),
                answer,
                conversationId);
    }
}
