package com.skala.day2.lab3.config;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.skala.day2.lab3.advisor.TokenMeterAdvisor;

@Configuration
public class Lab3AdvisorConfig {

    @Bean
    public ChatMemoryRepository lab3ChatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public ChatMemory lab3ChatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    @Bean("lab3AssistantClient")
    public ChatClient lab3AssistantClient(
            ChatClient.Builder builder,
            VectorStore vectorStore,
            ChatMemory chatMemory,
            TokenMeterAdvisor tokenMeter) {

        return builder
                .defaultSystem("""
                        너는 고객 상담 에이전트다.

                        - 주문 상태나 최근 주문 질문은 반드시 주문 Tool을 사용한다.
                        - 환불은 직접 실행하지 않고 승인 요청 Tool만 사용한다.
                        - 규정 관련 질문은 제공된 근거 문서를 사용한다.
                        - 근거에 없는 사실은 추측하지 않는다.
                        - 사용자 본인의 주문만 처리한다.
                        - 개인정보와 민감정보를 출력하지 않는다.
                        - 답변은 한국어로 간결하게 한다.
                        """)
                .defaultAdvisors(
                        tokenMeter,

                        SafeGuardAdvisor.builder()
                                .sensitiveWords(List.of(
                                        "주민등록번호",
                                        "카드번호",
                                        "비밀번호"))
                                .failureResponse(
                                        "죄송합니다. 민감정보가 포함된 요청은 처리할 수 없습니다.")
                                .order(100)
                                .build(),

                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .order(200)
                                .build(),

                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .topK(4)
                                                .similarityThreshold(0.4)
                                                .build())
                                .order(300)
                                .build(),

                        new SimpleLoggerAdvisor())
                .build();
    }
}
