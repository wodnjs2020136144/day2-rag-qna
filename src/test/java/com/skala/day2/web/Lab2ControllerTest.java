package com.skala.day2.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.skala.day2.domain.AnswerDto;
import com.skala.day2.domain.Chunk;
import com.skala.day2.domain.IngestResult;
import com.skala.day2.service.Lab2IngestService;
import com.skala.day2.service.Lab2QnaService;

/**
 * 웹 계층만 확인 — 모델을 부르지 않으므로 키 없이 돈다(Day 1 {@code OrderSummaryControllerTest}와 같은
 * 패턴). {@link Lab2IngestService}·{@link Lab2QnaService}를 Mockito로 대체해 컨트롤러의 위임·응답
 * 모양만 검증한다 — 서비스 안의 RAG 로직(TODO)이 아직 없어도 이 테스트는 통과해야 한다.
 *
 * <p>Step 1~3을 구현한 뒤에는 이 테스트를 건드리지 않아도 계속 통과한다 — 컨트롤러 계약이 바뀌지 않는 한.
 */
@WebMvcTest(Lab2Controller.class)
class Lab2ControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    Lab2IngestService ingestService;

    @MockitoBean
    Lab2QnaService qnaService;

    @Test
    void 인제스트는_문서별_결과를_돌려준다() throws Exception {
        given(ingestService.ingestAll())
                .willReturn(List.of(new IngestResult("return-policy.md", 4)));

        mvc.perform(post("/lab2/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("return-policy.md"))
                .andExpect(jsonPath("$[0].chunks").value(4));
    }

    @Test
    void 검색은_점수와_출처를_함께_돌려준다() throws Exception {
        given(qnaService.retrieve("반품 기한", 4))
                .willReturn(List.of(new Chunk("return-policy.md", 0.71, "단순 변심 반품은...")));

        mvc.perform(get("/lab2/retrieve").param("q", "반품 기한"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("return-policy.md"))
                .andExpect(jsonPath("$[0].score").value(0.71));
    }

    @Test
    void 근거가_없으면_거절_응답을_그대로_전달한다() throws Exception {
        given(qnaService.ask("우주 배송도 되나요?")).willReturn(AnswerDto.unknown());

        mvc.perform(post("/lab2/ask").param("q", "우주 배송도 되나요?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("확인되지 않습니다."))
                .andExpect(jsonPath("$.grounded").value(false));
    }
}
