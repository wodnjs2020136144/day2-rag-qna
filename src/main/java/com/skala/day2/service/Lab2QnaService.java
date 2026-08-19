package com.skala.day2.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
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

    /** 검색 결과 API에서 보여 줄 청크 본문의 최대 길이다. */
    private static final int SNIPPET_MAX_LENGTH = 120;

    private final VectorStore vectorStore;
    private final ChatClient answerChat;
    private final int answerTopK;
    private final double similarityThreshold;

    public Lab2QnaService(
            VectorStore vectorStore,
            ChatClient answerChatClient,
            @Value("${lab2.rag.answer-top-k:4}") int answerTopK,
            @Value("${lab2.rag.similarity-threshold:0.0}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.answerChat = answerChatClient;
        this.answerTopK = requirePositiveTopK(answerTopK);
        this.similarityThreshold = requireSimilarityThreshold(similarityThreshold);
    }

    /**
     * Step 2 — 검색만 수행한다(교안 p.223). 답변보다 먼저 검색 결과를 확인한다.
     *
     * <ol>
     *   <li>{@code vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(topK)
     *       .similarityThreshold(similarityThreshold).build())}로 검색한다. 임계값은 Step 5에서
     *       {@code LAB2_THRESHOLD}로 조정한다.</li>
     *   <li>결과를 {@link Chunk}(source, score, snippet 120자)로 매핑해 그대로 돌려준다 — 점수를
     *       감추지 않는다. "검색 결과를 눈으로 봤는가?"가 RAG 디버깅의 시작이다(01_study-guide.md §29).</li>
     * </ol>
     *
     * <p>세 질문으로 검증한다: ①"반품 기한" → return-policy 상위 ②"물건 돌려보내려면 며칠 안에?" →
     * 표현이 달라도 같은 문서를 찾는가 ③"우주 배송" → 점수가 전부 낮아 근거 없음으로 보이는가.
     */
    public List<Chunk> retrieve(String question, int topK) {
        validateSearchCondition(question, topK);

        List<Document> documents = searchDocuments(question, topK);

        // 2. VectorStore의 Document를 외부 응답용 Chunk로 변환한다.
        // 검색 품질을 눈으로 확인할 수 있도록 출처, 점수, 원문 일부를 모두 반환한다.
        return documents.stream()
                .map(document -> new Chunk(
                        sourceOf(document),
                        scoreOf(document),
                        snippetOf(document.getText())))
                .toList();
    }

    /**
     * 질문과 의미가 가까운 원본 Document를 검색한다.
     *
     * <p>{@link #retrieve}는 이 결과를 120자 미리보기로 바꾸지만, Step 3의 답변 생성에는
     * 정보가 잘리지 않은 전체 청크가 필요하므로 검색 로직을 별도 메서드로 공유한다.
     */
    private List<Document> searchDocuments(String question, int topK) {
        // 질문을 임베딩해 유사한 청크를 검색한다.
        // topK는 최대 검색 개수이고, threshold보다 낮은 결과는 제외한다.
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    /** 잘못된 검색 조건이 VectorStore까지 전달되지 않도록 미리 검사한다. */
    private void validateSearchCondition(String question, int topK) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("검색 질문은 비어 있을 수 없습니다.");
        }

        if (topK < 1) {
            throw new IllegalArgumentException("topK는 1 이상이어야 합니다.");
        }
    }

    /** Step 5에서 전달한 top-k 실험값을 애플리케이션 시작 시 검증한다. */
    private int requirePositiveTopK(int topK) {
        if (topK < 1) {
            throw new IllegalArgumentException("answer-top-k는 1 이상이어야 합니다.");
        }
        return topK;
    }

    /** Spring AI 유사도 임계값의 허용 범위인 0~1을 검사한다. */
    private double requireSimilarityThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("similarity-threshold는 0.0 이상 1.0 이하여야 합니다.");
        }
        return threshold;
    }

    /** Step 1에서 저장한 source 메타데이터를 검색 결과의 출처로 사용한다. */
    private String sourceOf(Document document) {
        Object source = document.getMetadata().get("source");
        return source == null ? "unknown" : source.toString();
    }

    /** 일반 Document에는 점수가 없을 수 있으므로 null일 때는 0으로 처리한다. */
    private double scoreOf(Document document) {
        Double score = document.getScore();
        return score == null ? 0.0 : score;
    }

    /** 줄바꿈과 연속 공백을 정리하고 원문 앞부분을 최대 120자로 제한한다. */
    private String snippetOf(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = text.replaceAll("\\s+", " ").trim();

        if (normalized.length() <= SNIPPET_MAX_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, SNIPPET_MAX_LENGTH) + "...";
    }

    /**
     * Step 3 — 검색한 근거만 사용해 답한다(교안 p.224).
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
        validateSearchCondition(question, answerTopK);

        // 1. 답변을 만들기 전에 관련 문서를 검색한다.
        // /retrieve 응답은 본문을 120자로 줄이지만, AI에는 정보가 잘리지 않은 전체 청크를 전달한다.
        List<Document> evidence = searchDocuments(question, answerTopK);

        // 2. 검색된 근거가 없으면 모델 호출 비용을 쓰지 않고 즉시 거절한다.
        if (evidence.isEmpty()) {
            return AnswerDto.unknown();
        }

        // 3. 각 청크에 출처를 붙여 모델이 본문과 파일명을 함께 구분할 수 있게 만든다.
        String context = formatContext(evidence);

        // 4. 사용자 질문을 문자열로 직접 이어 붙이지 않고 placeholder와 param으로 바인딩한다.
        // responseEntity를 사용하면 구조화 객체와 함께 finishReason도 검사할 수 있다.
        var response = answerChat.prompt()
                .user(user -> user
                        .text("""
                                [근거]
                                {context}

                                [질문]
                                {question}

                                AnswerDto 형식으로 답한다.
                                sources에는 실제 답변에 사용한 출처 파일명만 넣는다.
                                근거에서 답을 확인했으면 grounded=true, 확인하지 못했으면 grounded=false로 한다.
                                """)
                        .param("context", context)
                        .param("question", question))
                .call()
                .responseEntity(AnswerDto.class);

        // 5. 출력 한도 때문에 응답이 잘렸다면 불완전한 JSON을 정상 응답으로 취급하지 않는다.
        if (response.response() != null
                && response.response().hasFinishReasons(Set.of("length"))) {
            throw new IllegalStateException("AI 응답이 출력 토큰 한도 때문에 잘렸습니다.");
        }

        AnswerDto answer = response.entity();
        if (answer == null) {
            throw new IllegalStateException("AI 구조화 응답을 AnswerDto로 변환하지 못했습니다.");
        }

        return answer;
    }

    /** 검색된 청크의 전체 본문과 출처를 AI 프롬프트용 근거 문자열로 만든다. */
    private String formatContext(List<Document> evidence) {
        return evidence.stream()
                .map(document -> """
                        [출처: %s]
                        %s
                        """.formatted(
                                sourceOf(document),
                                document.getText() == null ? "" : document.getText().trim()))
                .collect(Collectors.joining("\n\n"));
    }
}
