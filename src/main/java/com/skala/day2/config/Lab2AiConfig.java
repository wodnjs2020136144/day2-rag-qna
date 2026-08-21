package com.skala.day2.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Lab2AiConfig {

    @Bean
    ChatClient answerChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        너는 사내 규정 안내 도우미다.

                        - 제공된 [근거]만 사용해 답한다.
                        - 근거에 직접 답이 있으면 반드시 그 근거를 사용한다.
                        - 근거에 없는 내용은 추측하지 않는다.
                        - 답할 수 없을 때만 "확인되지 않습니다."라고 한다.
                        - 출처는 구조화 응답의 sources 필드에 넣는다.
                        """)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.0)
                        .maxTokens(300))
                .build();
    }
}
