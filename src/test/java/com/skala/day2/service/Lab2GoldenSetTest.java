package com.skala.day2.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.day2.domain.AnswerDto;

/**
 * Step 4 — 골든 세트로 측정
 *
 * 실제 모델을 호출하므로 기본 ./gradlew test 에서는 제외되고,
 * ./gradlew test -Peval 로 실행한다.
 *
 * golden.json 10문항의 정답 키워드와 출처를 확인하고
 * 통과 개수를 측정한다.
 */
@SpringBootTest
class Lab2GoldenSetTest {

    private static final Logger log = LoggerFactory.getLogger(Lab2GoldenSetTest.class);

    @Autowired
    private Lab2QnaService qnaService;

    @Autowired
    private Lab2IngestService ingestService;

    @Test
    void 골든_세트_평가() throws IOException {

        // 테스트용 VectorStore는 새로 생성되므로
        // 평가 전에 사내 문서를 먼저 인제스트한다.
        ingestService.ingestAll();

        ObjectMapper mapper = new ObjectMapper()
                .configure(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false);

        List<Golden> golden = mapper.readValue(
                new ClassPathResource("golden.json").getInputStream(),
                mapper.getTypeFactory()
                        .constructCollectionType(
                                List.class,
                                Golden.class));

        int pass = 0;

        for (Golden g : golden) {

            AnswerDto a = qnaService.ask(g.q());

            // 정답에 반드시 포함되어야 하는 키워드 확인
            boolean hit = g.must()
                    .stream()
                    .allMatch(k -> a.answer() != null
                            && a.answer().contains(k));

            // 기대 출처가 있다면 실제 sources에 포함됐는지 확인
            boolean cite = g.src() == null
                    || a.sources()
                            .stream()
                            .anyMatch(s -> s.contains(g.src()));

            if (hit && cite) {

                pass++;

                log.info(
                        "통과: {}\n 답변: {}\n 출처: {}",
                        g.q(),
                        a.answer(),
                        a.sources());

            } else {

                log.warn(
                        "실패: {}\n 답변: {}\n 출처: {}",
                        g.q(),
                        a.answer(),
                        a.sources());
            }
        }

        log.info(
                "통과 {}/{}",
                pass,
                golden.size());

        // 완료 기준: 10문항 중 8문항 이상
        assertThat(pass)
                .isGreaterThanOrEqualTo(8);
    }
}
