package com.skala.day2.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.skala.day2.config.Lab2RagProperties;
import com.skala.day2.domain.IngestResult;

/**
 * Step 1 — 인제스트 (교안 p.222).
 *
 * <p>{@code lab2-docs/*.md} 문서마다 읽기 → 분할 → (실험표 F 전용) 겹침 후처리 → 메타데이터 부여 →
 * 재색인(지우고 넣기) 순으로 처리한다. {@code source}·{@code version} 메타데이터는 분할 시점에
 * 넣는다 — 벡터 저장소에 들어간 뒤에는 메타데이터만 따로 갱신할 방법이 없다.
 *
 * <p>청크 크기·겹침 비율은 더 이상 상수가 아니라 {@link Lab2RagProperties}(`lab2.rag.*`)에서 온다 —
 * Step 5 실험표(A~F)를 조합별로 재려면 시스템 프로퍼티로 값을 바꿔야 하는데, 상수로 박아 두면 매번
 * 코드를 고치고 커밋해야 해서 재현이 안 된다. 왜 이 기본값(120/80)을 골랐는지는
 * {@code application.yml}의 {@code lab2.rag} 블록 주석 참고.
 */
@Service
public class Lab2IngestService {

    private static final String LAB2_DOCS_PATTERN = "classpath:/lab2-docs/*.md";

    private final VectorStore vectorStore;
    private final Lab2RagProperties props;
    private final FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

    public Lab2IngestService(VectorStore vectorStore, Lab2RagProperties props) {
        this.vectorStore = vectorStore;
        this.props = props;
    }

    /** lab2-docs 아래 문서를 전부 (재)인제스트하고 문서별 결과를 돌려준다. */
    public List<IngestResult> ingestAll() {
        Resource[] resources = resolveDocResources();
        String version = Instant.now().toString();

        List<IngestResult> results = new ArrayList<>();
        for (Resource resource : resources) {
            results.add(ingestOne(resource, version));
        }
        return results;
    }

    private IngestResult ingestOne(Resource resource, String version) {
        String source = resource.getFilename();

        List<Document> raw = new TextReader(resource).get();
        List<Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(props.chunkSize())
                .withMinChunkSizeChars(props.minChunkSizeChars())
                .build()
                .apply(raw);

        List<Document> overlapped = applyOverlap(chunks, props.overlapRatio());

        List<Document> tagged = overlapped.stream()
                .map(chunk -> chunk.mutate()
                        .metadata("source", source)
                        .metadata("version", version)
                        .build())
                .toList();

        // 재색인 — 같은 source를 지운 뒤 다시 넣는다. 두 번 인제스트해도 조각 수가 늘지 않는다.
        vectorStore.delete(filterBuilder.eq("source", source).build());
        vectorStore.add(tagged);

        return new IngestResult(source, tagged.size());
    }

    /**
     * {@code TokenTextSplitter}(2.0.0)에는 겹침 옵션이 없다 — {@code Builder}에 해당 메서드 자체가
     * 없다(실측: 2.0.0 jar를 {@code javap}로 확인). 실험표 F(겹침 20%)를 재기 위해, 분할 후 각
     * 청크 앞에 직전 청크 꼬리의 {@code ratio} 비율만큼을 문자 단위로 이어 붙인다. 토큰 기준이
     * 아니라 문자 기준 근사다 — 정확한 토큰 겹침이 필요하면 이 방식으로는 부족하다.
     *
     * <p>{@code ratio <= 0}이면(기본값) 그대로 돌려준다 — A~E, G 조합은 이 경로를 타지 않는다.
     */
    private List<Document> applyOverlap(List<Document> chunks, double ratio) {
        if (ratio <= 0 || chunks.size() < 2) {
            return chunks;
        }

        List<Document> result = new ArrayList<>(chunks.size());
        result.add(chunks.get(0));
        for (int i = 1; i < chunks.size(); i++) {
            String prevText = chunks.get(i - 1).getText();
            String currText = chunks.get(i).getText();
            int tailLength = (int) Math.round((prevText != null ? prevText.length() : 0) * ratio);
            String tail = prevText != null && tailLength > 0
                    ? prevText.substring(Math.max(0, prevText.length() - tailLength))
                    : "";
            Document overlapped = chunks.get(i).mutate()
                    .text(tail + currText)
                    .build();
            result.add(overlapped);
        }
        return result;
    }

    private Resource[] resolveDocResources() {
        try {
            return new PathMatchingResourcePatternResolver().getResources(LAB2_DOCS_PATTERN);
        } catch (IOException e) {
            throw new IllegalStateException("lab2-docs 문서를 읽는 중 오류가 발생했습니다: " + LAB2_DOCS_PATTERN, e);
        }
    }
}
