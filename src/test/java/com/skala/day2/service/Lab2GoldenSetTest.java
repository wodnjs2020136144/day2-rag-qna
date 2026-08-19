package com.skala.day2.service;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.day2.domain.AnswerDto;
import com.skala.day2.domain.IngestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 4 — 골든 세트로 RAG 품질을 측정한다(교안 p.225).
 *
 * <p>실제 모델을 호출하므로 기본 {@code ./gradlew test}에서는 제외돼 있다(build.gradle 참고).
 * 실행: {@code ./gradlew test -Peval}
 *
 * <p>golden.json 10문항은 {@code main}에 고정돼 있다 — 두 사람의 통과율을 비교할 유일한 기준자이므로
 * 임의로 고치지 않는다(고칠 필요가 있으면 PR로 상의한다).
 *
 * <p>채울 것: {@link Golden#must()}가 답변에 전부 포함되는지, {@link Golden#src()}가 null이 아니면
 * 출처에 포함되는지 확인하고 통과 개수를 센다. 실패한 문항은 반드시 로그로 남긴다 — 답을 읽지 않고
 * 고치면 뭘 고쳤는지 알 수 없다(p.225). 실패를 두 종류로 나눠 결과보고서에 적는다:
 * ㉠ 근거를 못 찾았다(청킹·임베딩·top-k·질문 변환 문제) / ㉡ 찾고도 잘못 답했다(프롬프트·모델·근거 포맷 문제).
 */
@SpringBootTest
class Lab2GoldenSetTest {

    private static final Logger log = LoggerFactory.getLogger(Lab2GoldenSetTest.class);

    @Autowired
    private Lab2QnaService qnaService;

    @Autowired
    private Lab2IngestService ingestService;

    /**
     * SimpleVectorStore는 테스트가 시작될 때 비어 있으므로 평가 전에 문서 3종을 먼저 넣는다.
     *
     * <p>서버에서 실행한 {@code /lab2/ingest} 결과는 테스트 프로세스와 공유되지 않는다.
     * 테스트가 외부 실행 순서에 의존하지 않도록 여기서 직접 인제스트한다.
     */
    @BeforeEach
    void 골든_세트_평가_전_문서를_인제스트한다() {
        List<IngestResult> results = ingestService.ingestAll();

        // 공통 기준 문서인 반품·배송·멤버십 3종이 모두 들어갔는지 먼저 확인한다.
        assertThat(results)
                .as("골든 세트 평가 전 lab2-docs 문서 3종이 인제스트되어야 한다")
                .hasSize(3);

        log.info("골든 세트용 문서 인제스트 완료: {}", results);
    }

    @Test
    void 골든_세트_평가() throws IOException {
        // 1. 두 사람이 공통으로 사용하는 고정 질문 10개를 읽는다.
        // note처럼 record에 없는 필드가 있어도 읽을 수 있도록 알 수 없는 속성은 무시한다.
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Golden> golden = mapper.readValue(
                new ClassPathResource("golden.json").getInputStream(),
                mapper.getTypeFactory().constructCollectionType(List.class, Golden.class));

        assertThat(golden)
                .as("골든 세트는 비교 기준을 유지하기 위해 10문항이어야 한다")
                .hasSize(10);

        // 2. 각 질문을 실제 RAG 서비스에 보내 답변, 필수 단어, 출처, grounded를 검사한다.
        int pass = 0;
        for (Golden g : golden) {
            AnswerDto answer = qnaService.ask(g.q());

            // 구조화 응답의 일부 필드가 null이어도 평가 코드 자체가 중단되지 않게 안전하게 변환한다.
            String answerText = answer.answer() == null ? "" : answer.answer();
            List<String> sources = answer.sources() == null ? List.of() : answer.sources();

            // must에 적힌 필수 단어가 답변에 모두 포함돼야 한다.
            boolean hit = g.must().stream()
                    .allMatch(answerText::contains);

            // src가 있는 문항은 해당 출처가 있어야 하고, null이면 문서에 답이 없는 질문이다.
            boolean cite = g.src() == null
                    || sources.stream().anyMatch(source -> source.contains(g.src()));

            // 문서에 답이 있으면 grounded=true, 없으면 false여야 한다.
            boolean grounded = g.src() == null
                    ? !answer.grounded()
                    : answer.grounded();

            if (hit && cite && grounded) {
                pass++;
                log.info("통과: {} → 답변={}, 출처={}, grounded={}",
                        g.q(), answerText, sources, answer.grounded());
            } else {
                // 실패한 답을 읽어야 검색 문제인지 생성 문제인지 다음 조정 방향을 정할 수 있다.
                log.warn("""
                        실패: {}
                          필수 단어 통과: {}
                          출처 통과: {}
                          grounded 통과: {}
                          답변: {}
                          출처: {}
                        """,
                        g.q(), hit, cite, grounded, answerText, sources);
            }
        }

        // 3. 통과율을 로그에 남기고 최소 기준인 8/10을 코드로 고정한다.
        log.info("통과 {}/{}", pass, golden.size());
        assertThat(pass)
                .as("Day 2 완료 기준 6번: 골든 세트 10문항 중 8문항 이상 통과")
                .isGreaterThanOrEqualTo(8);
    }
}
