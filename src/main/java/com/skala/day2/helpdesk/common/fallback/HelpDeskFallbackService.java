package com.skala.day2.helpdesk.common.fallback;

import java.util.List;

import org.springframework.stereotype.Service;

import com.skala.day2.helpdesk.domain.ChatRequest;
import com.skala.day2.helpdesk.domain.ChatResponse;

@Service
public class HelpDeskFallbackService {

    public ChatResponse fallback(ChatRequest request) {

        String conversationId =
                request.userId() + ":" + request.sessionId();

        String answer = switch (request.domain()) {

            case SEMICONDUCTOR ->
                    "현재 AI 응답 서비스를 일시적으로 사용할 수 없습니다. "
                    + "장비 상태 변경 없이 잠시 후 다시 조회해 주세요.";

            case FINANCE ->
                    "현재 AI 응답 서비스를 일시적으로 사용할 수 없습니다. "
                    + "거래 상태 변경 없이 잠시 후 다시 조회해 주세요.";
        };

        return new ChatResponse(
                request.domain(),
                answer,
                conversationId,
                List.of());
    }
}
