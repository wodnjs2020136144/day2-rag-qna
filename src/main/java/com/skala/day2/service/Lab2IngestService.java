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

import com.skala.day2.domain.IngestResult;

/**
 * Step 1 — 인제스트 (교안 p.222).
 *
 * <p>{@code lab2-docs/*.md} 문서마다 읽기 → 분할 → 메타데이터 부여 → 재색인(지우고 넣기) 순으로
 * 처리한다. {@code source}·{@code version} 메타데이터는 분할 시점에 넣는다 — 벡터 저장소에 들어간
 * 뒤에는 메타데이터만 따로 갱신할 방법이 없다.
 */
@Service
public class Lab2IngestService {

    private static final String LAB2_DOCS_PATTERN = "classpath:/lab2-docs/*.md";

    // 실측: 이 실습 문서는 각 ~100토큰이라 기본값(400/200)으로는 문서당 1청크 — 청크 검색이 아니라
    // 문서 전체 단위 검색이 돼 버려 무관한 질문("우주 배송")도 주제어 겹침만으로 높은 점수를 받는다.
    // 섹션 단위로 쪼개려고 값을 낮췄다 — Step 5 실험표(200/400/800)를 채울 때 이 두 상수만 바꾼다.
    private static final int CHUNK_SIZE = 120;
    private static final int MIN_CHUNK_SIZE_CHARS = 80;

    private final VectorStore vectorStore;
    private final FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

    public Lab2IngestService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
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
                .withChunkSize(CHUNK_SIZE)
                .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                .build()
                .apply(raw);

        List<Document> tagged = chunks.stream()
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

    private Resource[] resolveDocResources() {
        try {
            return new PathMatchingResourcePatternResolver().getResources(LAB2_DOCS_PATTERN);
        } catch (IOException e) {
            throw new IllegalStateException("lab2-docs 문서를 읽는 중 오류가 발생했습니다: " + LAB2_DOCS_PATTERN, e);
        }
    }
}
