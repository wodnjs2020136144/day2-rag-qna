package com.skala.day2.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
import com.skala.day2.config.Lab2RagProperties;
import com.skala.day2.domain.AnswerDto;
import com.skala.day2.domain.IngestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TODO: Step 4 — 골든 세트로 측정 (교안 p.225)
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

    @Autowired
    private Lab2RagProperties ragProperties;

    private List<IngestResult> ingestResults;

    // 이 테스트는 @SpringBootTest 자체 컨텍스트에서 뜬 별도의 인메모리 VectorStore를 쓴다 —
    // bootRun으로 띄운 서버에 인제스트해 둔 데이터와는 무관하다. 매번 새로 채워야 근거가 잡힌다.
    @BeforeEach
    void 인제스트() {
        ingestResults = ingestService.ingestAll();
        // Step 5 실험표(A~F)는 같은 테스트를 파라미터만 바꿔 여러 번 돌린다 — 어느 조합의 결과인지
        // 로그만 보고 알 수 있게 활성 파라미터와 문서별 청크 수를 남긴다.
        log.info("[Step5] 파라미터: {} / 청크 수: {}", ragProperties, ingestResults);
    }

    @Test
    void 골든_세트_평가() throws IOException {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Golden> golden = mapper.readValue(
                new ClassPathResource("golden.json").getInputStream(),
                mapper.getTypeFactory().constructCollectionType(List.class, Golden.class));

        int pass = 0;
        for (Golden g : golden) {
            AnswerDto a = qnaService.ask(g.q());
            boolean hit = g.must().stream().allMatch(k -> a.answer().contains(k));
            boolean cite = g.src() == null
                    || a.sources().stream().anyMatch(s -> s.contains(g.src()));
            if (hit && cite) {
                pass++;
            } else {
                log.warn("실패: {}\n 답변: {}\n 출처: {}", g.q(), a.answer(), a.sources());
            }
        }
        log.info("통과 {}/{}", pass, golden.size());
        기록(pass, golden.size());
        assertThat(pass).isGreaterThanOrEqualTo(8);   // 완료 기준 6번 — 기준선을 코드에 박아 둔다
    }

    /**
     * 통과 수를 단언보다 먼저 파일에 남긴다 — 나쁜 조합(threshold 0.7 등)은 단언에서 실패하는 게
     * 정상이지만, 그래도 숫자는 남아야 {@code scripts/step5-experiment.sh}가 실험표를 채울 수 있다.
     */
    private void 기록(int pass, int total) throws IOException {
        Path out = Path.of("build/step5/result.txt");
        Files.createDirectories(out.getParent());
        String line = "%s\tchunkSize=%d\tminChunkSizeChars=%d\toverlapRatio=%s\ttopK=%d\tsimilarityThreshold=%s\tpass=%d/%d%n"
                .formatted(java.time.Instant.now(), ragProperties.chunkSize(), ragProperties.minChunkSizeChars(),
                        ragProperties.overlapRatio(), ragProperties.topK(), ragProperties.similarityThreshold(),
                        pass, total);
        Files.writeString(out, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
