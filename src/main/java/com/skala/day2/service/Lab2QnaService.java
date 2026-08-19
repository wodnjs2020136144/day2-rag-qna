package com.skala.day2.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.skala.day2.domain.AnswerDto;
import com.skala.day2.domain.Chunk;

@Service
public class Lab2QnaService {

    private final VectorStore vectorStore;
    private final ChatClient answerChat;

    public Lab2QnaService(
            VectorStore vectorStore,
            ChatClient answerChatClient) {

        this.vectorStore = vectorStore;
        this.answerChat = answerChatClient;
    }

    /**
     * 공통 검색 로직
     */
    private List<Document> searchDocuments(String question, int topK) {

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThresholdAll()
                        .build());
    }

    /**
     * Step 2 — 검색 결과 확인
     */
    public List<Chunk> retrieve(String question, int topK) {

        var documents = searchDocuments(question, topK);

        return documents.stream()
                .map(document -> {

                    String source = String.valueOf(
                            document.getMetadata().get("source"));

                    double score = document.getScore() == null
                            ? 0.0
                            : document.getScore();

                    String text = document.getText() == null
                            ? ""
                            : document.getText();

                    String snippet = text.length() <= 120
                            ? text
                            : text.substring(0, 120);

                    return new Chunk(
                            source,
                            score,
                            snippet);
                })
                .toList();
    }

    /**
     * Step 3 — 검색된 근거만 이용하여 답변
     */
    public AnswerDto ask(String question) {

        var documents = searchDocuments(question, 4);

        // 검색 결과가 정말 하나도 없으면 모델 호출 자체를 하지 않는다.
        if (documents.isEmpty()) {
            return AnswerDto.unknown();
        }

        // AI에게는 snippet이 아니라 검색된 문서의 전체 내용을 전달한다.
        String context = documents.stream()
                .map(document -> {
                    String source = String.valueOf(
                            document.getMetadata().get("source"));

                    String text = document.getText() == null
                            ? ""
                            : document.getText();

                    return "[출처: " + source + "]\n" + text;
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        AnswerDto result = answerChat.prompt()
                .user(user -> user
                        .text("""
                                [근거]
                                {context}

                                [질문]
                                {question}

                                다음 규칙을 반드시 지켜라.
                                - 질문의 답이 위 근거에 직접 존재할 때만 답한다.
                                - 근거로 확인할 수 없다면 추측하지 않는다.
                                - 확인할 수 없는 경우 answer는 정확히 "확인되지 않습니다."로 한다.
                                - 확인할 수 없는 경우 sources는 빈 배열, grounded는 false로 한다.
                                - 답할 수 있는 경우 실제 사용한 파일명만 sources에 넣는다.
                                - 근거를 사용해 답한 경우 grounded는 true로 한다.
                                """)
                        .param("context", context)
                        .param("question", question))
                .call()
                .entity(AnswerDto.class);

        if (result == null) {
            return AnswerDto.unknown();
        }

        // 모델이 거절했다면 응답 형식을 확실하게 통일한다.
        if (result.answer() != null
                && result.answer().contains("확인되지")) {
            return AnswerDto.unknown();
        }

        return result;
    }
}
