package com.skala.day2.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Day 2 메인 실습 — 답변 전용 {@link ChatClient} 빈 하나.
 *
 * <p>이 빈 자체는 배선(wiring)이라 이미 완성해 뒀다 — 실제로 채워야 할 RAG 로직(검색 → 근거 포맷팅 →
 * 거절 → 구조화 출력)은 {@code service} 패키지에 있다. 다만 아래 두 값은 실습 중 조정 대상이다:
 *
 * <ul>
 *   <li>{@code temperature} — 0.0으로 고정돼 있다. 규정 답변은 매번 같아야 하므로 올리지 않는다.</li>
 *   <li>{@code maxTokens} — 300으로 시작한다. 답변이 자꾸 잘리면({@code finishReason=length}) 올리고,
 *       반대로 너무 길면 줄인다. Day 1 `03_agent-context.md`: 잘린 응답을 정상 응답으로 취급해
 *       그대로 파싱/전달하지 않는다 — 잘림 감지는 service 계층에서 한다.</li>
 * </ul>
 *
 * <p>이 프로젝트는 spring-ai-bom 2.0.0을 쓴다 — {@code defaultOptions}는 {@code ChatOptions.Builder}를
 * 받는다(빌드 전 상태로 넘긴다). 강사 샘플(spring-ai 1.1.8, 08_위키QnA 등)의 {@code .build()} 호출과
 * 다르니 그대로 베끼지 않는다.
 */
@Configuration
public class Lab2AiConfig {

    @Bean
    ChatClient answerChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        너는 사내 규정 안내 도우미다.
                        아래 [근거]만 사용해 한국어로 답한다. 근거에 없으면 "확인되지 않습니다"라고만 답한다.
                        추측하지 않는다. 답변 끝에 실제로 사용한 근거의 출처를 [출처: 파일명] 형식으로 남긴다.
                        """)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.0)   // 규정 답변은 재현 가능해야 한다
                        .maxTokens(300))
                .build();
    }
}
