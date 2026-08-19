package com.skala.day2.service;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 5 실험표(A~F)의 통과율 숫자만으로는 "왜 그런지"를 알 수 없다 — 그 이유는 문서별 청크 수다.
 * 이 테스트는 회귀 방지가 아니라 <b>실험표 해석 근거를 로그로 남기는 것</b>이 목적이다
 * ({@code docs/결과보고서.md} 5절, {@code docs/step5-실험결과.md} 참고). 임베딩·모델 호출이 없어
 * {@code OPENAI_API_KEY} 없이 {@code ./gradlew test}로 돈다.
 */
class Lab2ChunkingTest {

    private static final Logger log = LoggerFactory.getLogger(Lab2ChunkingTest.class);
    private static final String LAB2_DOCS_PATTERN = "classpath:/lab2-docs/*.md";

    @Test
    void 조합별_청크_수() throws IOException {
        // 실험표에서 실제로 쓴 (chunkSize, minChunkSizeChars) 조합. topK·threshold·overlap은
        // 분할 결과에 영향을 주지 않으므로 여기서는 재지 않는다.
        int[][] combos = {
                {400, 200}, // A(기준)·D(넓게)·E(엄격)와 동일한 분할
                {200, 100}, // B(작게)
                {800, 400}, // C(크게)
                {120, 80},  // G(채택값)
        };

        for (int[] combo : combos) {
            int chunkSize = combo[0];
            int minChunkSizeChars = combo[1];

            int total = 0;
            StringBuilder perDoc = new StringBuilder();
            for (Resource resource : resolveDocResources()) {
                List<Document> raw = new TextReader(resource).get();
                List<Document> chunks = TokenTextSplitter.builder()
                        .withChunkSize(chunkSize)
                        .withMinChunkSizeChars(minChunkSizeChars)
                        .build()
                        .apply(raw);
                total += chunks.size();
                perDoc.append(resource.getFilename()).append('=').append(chunks.size()).append(' ');
            }

            log.info("[Step5] chunkSize={} minChunkSizeChars={} -> 총 {}청크 ({})",
                    chunkSize, minChunkSizeChars, total, perDoc.toString().trim());
            assertThat(total).isGreaterThan(0);
        }
    }

    private Resource[] resolveDocResources() throws IOException {
        return new PathMatchingResourcePatternResolver().getResources(LAB2_DOCS_PATTERN);
    }
}
