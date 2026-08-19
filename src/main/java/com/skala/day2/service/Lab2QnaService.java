package com.skala.day2.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.skala.day2.domain.AnswerDto;
import com.skala.day2.domain.Chunk;

/**
 * Step 2~3 — 검색과 답변 (교안 p.223–224). 직접 조립형으로 통일한다 — {@code similaritySearch} →
 * 근거 포맷팅 → 수동 프롬프트 → {@code .entity(AnswerDto.class)}.
 */
@Service
public class Lab2QnaService {

    private static final int ASK_TOP_K = 4;
    // 실측(text-embedding-3-small, 섹션 단위 청크 재조정 후): golden.json 10문항 기준 정답 청크는
    // 최저 0.22~0.25대에서도 나오는데, 무관한 질문("우주 배송")의 최고 오탐 점수는 0.45로 오히려
    // 더 높다 — "배송"이라는 단어 자체가 겹쳐서 벡터 유사도만으로는 절대 분리되지 않는다.
    // threshold는 완전 무관한(점수 자체가 바닥인) 질문만 거르는 1차 방어선으로 낮게 두고, 진짜 거절은
    // 시스템 프롬프트("근거에 없으면 확인되지 않습니다")의 grounded 판단에 맡긴다.
    private static final double SIMILARITY_THRESHOLD = 0.2;
    private static final int SNIPPET_LENGTH = 120;

    private final VectorStore vectorStore;
    private final ChatClient answerChat;

    public Lab2QnaService(VectorStore vectorStore, ChatClient answerChatClient) {
        this.vectorStore = vectorStore;
        this.answerChat = answerChatClient;
    }

    /** Step 2 — 검색만 (교안 p.223). 점수를 감추지 않고 그대로 돌려준다. */
    public List<Chunk> retrieve(String question, int topK) {
        return search(question, topK).stream()
                .map(doc -> new Chunk(sourceOf(doc), scoreOf(doc), snippetOf(doc)))
                .toList();
    }

    /**
     * Step 3 — 근거로 답하기 (교안 p.224). 근거가 비면 모델을 부르지 않고 거절한다(완료 기준 4번).
     */
    public AnswerDto ask(String question) {
        List<Document> evidence = search(question, ASK_TOP_K);
        if (evidence.isEmpty()) {
            return AnswerDto.unknown();
        }

        String context = evidence.stream()
                .map(doc -> "출처: %s%n%s".formatted(sourceOf(doc), doc.getText()))
                .collect(Collectors.joining("\n\n"));

        ResponseEntity<ChatResponse, AnswerDto> response = answerChat.prompt()
                .user(u -> u.text("[근거]\n{context}\n\n[질문] {question}")
                        .param("context", context)
                        .param("question", question))
                .call()
                .responseEntity(AnswerDto.class);

        ChatResponse chatResponse = response.response();
        if (chatResponse != null && chatResponse.hasFinishReasons(Set.of("LENGTH"))) {
            // 잘린 응답(finishReason=length)을 정상 응답으로 취급하지 않는다 —
            // Lab2ExceptionHandler가 503으로 안전하게 처리한다. maxTokens를 늘리는 게 근본 해법이다.
            throw new IllegalStateException("답변이 최대 토큰 길이에서 잘렸습니다(finishReason=length).");
        }

        AnswerDto answer = response.entity();
        return answer != null ? sanitizeSources(answer) : AnswerDto.unknown();
    }

    private List<Document> search(String question, int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build());
    }

    private String sourceOf(Document doc) {
        Object source = doc.getMetadata().get("source");
        return source != null ? source.toString() : "unknown";
    }

    private double scoreOf(Document doc) {
        Double score = doc.getScore();
        return score != null ? score : 0.0;
    }

    private String snippetOf(Document doc) {
        String text = doc.getText();
        if (text == null) {
            return "";
        }
        return text.length() <= SNIPPET_LENGTH ? text : text.substring(0, SNIPPET_LENGTH);
    }

    /** 모델이 근거 포맷("출처: 파일명")의 접두어·대괄호를 그대로 베껴 오는 경우를 방어적으로 정리한다. */
    private AnswerDto sanitizeSources(AnswerDto answer) {
        List<String> cleaned = answer.sources().stream()
                .map(s -> s.replaceAll("^\\[|\\]$", "").replaceFirst("^출처:\\s*", "").trim())
                .filter(s -> !s.isEmpty())
                .toList();
        return new AnswerDto(answer.answer(), cleaned, answer.grounded());
    }
}
