package com.skala.day2.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.day2.domain.AnswerDto;
import com.skala.day2.domain.Chunk;
import com.skala.day2.domain.IngestResult;
import com.skala.day2.service.Lab2IngestService;
import com.skala.day2.service.Lab2QnaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Day 2 메인 실습 — 3개 엔드포인트. 이 컨트롤러는 {@code ChatClient}·{@code VectorStore}를 모른다
 * (03_agent-context.md 하드 규칙) — 전부 {@link Lab2IngestService}·{@link Lab2QnaService}에 위임한다.
 *
 * <p>구현은 이 파일이 아니라 두 서비스에 채운다. 이 뼈대는 손대지 않아도 된다.
 */
@RestController
@Tag(name = "Day 2 메인 실습 · 사내 문서 Q&A")
public class Lab2Controller {

    private final Lab2IngestService ingestService;
    private final Lab2QnaService qnaService;

    public Lab2Controller(Lab2IngestService ingestService, Lab2QnaService qnaService) {
        this.ingestService = ingestService;
        this.qnaService = qnaService;
    }

    /** 두 번 실행해 조각 수를 비교한다 — 같아야 정상(재색인이 걸려 있다). 완료 기준 1·8번. */
    @PostMapping("/lab2/ingest")
    @Operation(summary = "lab2-docs 전체 인제스트", description = "두 번 실행해도 조각 수가 늘지 않아야 한다.")
    public List<IngestResult> ingest() {
        return ingestService.ingestAll();
    }

    /** 답변보다 먼저 만든다 — 검색이 보이지 않으면 어디를 고칠지 알 수 없다. 완료 기준 2번. */
    @GetMapping("/lab2/retrieve")
    @Operation(summary = "검색 결과만 본다", description = "출처와 점수를 그대로 노출한다.")
    public List<Chunk> retrieve(@RequestParam String q,
                                 @RequestParam(defaultValue = "4") int topK) {
        return qnaService.retrieve(q, topK);
    }

    /** 근거가 없으면 모델을 부르지 않는다. 완료 기준 3·4·5번. */
    @PostMapping("/lab2/ask")
    @Operation(summary = "근거를 붙여 답한다", description = "근거가 없으면 \"확인되지 않습니다\"로 거절한다.")
    public AnswerDto ask(@RequestParam String q) {
        return qnaService.ask(q);
    }
}
