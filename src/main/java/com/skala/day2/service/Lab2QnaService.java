package com.skala.day2.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.skala.day2.domain.AnswerDto;
import com.skala.day2.domain.Chunk;

/**
 * TODO: Step 2~3 — 검색과 답변 (교안 p.223–224)
 *
 * <p>이 프로젝트는 <b>직접 조립형</b>으로 통일한다 — {@code QuestionAnswerAdvisor} 같은 어드바이저는
 * 확장 과제("어드바이저 전환")를 고를 때만 쓴다. 참고: `02_lab-guide.md` Step 2·3 코드 예시,
 * 강사 샘플 `08_위키QnA/WikiRag.java`의 {@code 찾기()}·{@code 묻기()}(단, 아래 두 가지는 그대로
 * 베끼지 않는다 — ①거절 지시 없이 프롬프트에 사용자 입력을 직접 이어 붙임 ②컨트롤러 직접 호출).
 */
@Service
public class Lab2QnaService {

    private final VectorStore vectorStore;
    private final ChatClient answerChat;

    public Lab2QnaService(VectorStore vectorStore, ChatClient answerChatClient) {
        this.vectorStore = vectorStore;
        this.answerChat = answerChatClient;
    }

    /**
     * TODO: Step 2 — 검색만 (교안 p.223). 답변보다 먼저 만든다.
     *
     * <ol>
     *   <li>{@code vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(topK)
     *       .similarityThreshold(0.5).build())}로 검색한다.</li>
     *   <li>결과를 {@link Chunk}(source, score, snippet 120자)로 매핑해 그대로 돌려준다 — 점수를
     *       감추지 않는다. "검색 결과를 눈으로 봤는가?"가 RAG 디버깅의 시작이다(01_study-guide.md §29).</li>
     * </ol>
     *
     * <p>세 질문으로 검증한다: ①"반품 기한" → return-policy 상위 ②"물건 돌려보내려면 며칠 안에?" →
     * 표현이 달라도 같은 문서를 찾는가 ③"우주 배송" → 점수가 전부 낮아 근거 없음으로 보이는가.
     */
    public List<Chunk> retrieve(String question, int topK) {
        throw new UnsupportedOperationException(
                "TODO: Step 2 — 교안 p.223. similaritySearch로 top-k 근거를 찾아 점수와 함께 돌려준다.");
    }

    /**
     * TODO: Step 3 — 근거로 답하기 (교안 p.224).
     *
     * <ol>
     *   <li>{@link #retrieve}로 근거를 찾는다. <b>비어 있으면 모델을 부르지 않고 {@link AnswerDto#unknown()}을
     *       반환한다</b> — 완료 기준 4번(거절), 오늘의 진짜 학습 지점(p.229).</li>
     *   <li>근거가 있으면 {@code answerChat.prompt().user(u -> u.text("[근거]\n{context}\n\n[질문]
     *       {question}").param("context", ...).param("question", question)).call().entity(AnswerDto.class)}
     *       로 구조화 출력을 받는다. ⚠️ 사용자 입력을 문자열에 직접 이어 붙이지 않는다 — {@code {placeholder}}
     *       + {@code .param()} 바인딩(03_agent-context.md 하드 규칙).</li>
     *   <li>{@code finishReason=length}(응답 잘림)를 정상 응답으로 취급해 그대로 반환하지 않는다 —
     *       Day 1 `OrderSummaryService.callModel()`의 처리 방식을 참고한다.</li>
     * </ol>
     */
    public AnswerDto ask(String question) {
        throw new UnsupportedOperationException(
                "TODO: Step 3 — 교안 p.224. 근거가 없으면 모델을 부르지 않고 unknown()을 반환한다.");
    }
}
