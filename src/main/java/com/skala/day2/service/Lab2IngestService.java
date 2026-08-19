package com.skala.day2.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.skala.day2.domain.IngestResult;

/**
 * Day 2 종합실습의 문서 인제스트 서비스.
 *
 * <p>{@code lab2-docs}의 Markdown 문서를 읽고 다음 순서로 VectorStore에 저장한다.
 *
 * <pre>
 * 문서 읽기 → 청크 분할 → 메타데이터 추가 → 기존 청크 삭제 → 임베딩 및 저장
 * </pre>
 *
 * <p>현재 문서는 모두 Markdown이므로 PDF·DOCX용 {@code TikaDocumentReader}가 아니라
 * {@link TextReader}를 사용한다.
 */
@Service
public class Lab2IngestService {

    private static final Logger log = LoggerFactory.getLogger(Lab2IngestService.class);

    private static final String DOCUMENT_PATTERN = "classpath*:lab2-docs/*.md";
    private static final String DOCUMENT_VERSION = "2026-08";

    private final VectorStore vectorStore;
    private final PathMatchingResourcePatternResolver resourceResolver;
    private final int chunkSize;
    private final int minChunkSizeChars;
    private final int chunkOverlapPercent;

    public Lab2IngestService(
            VectorStore vectorStore,
            @Value("${lab2.rag.chunk-size:400}") int chunkSize,
            @Value("${lab2.rag.min-chunk-size-chars:200}") int minChunkSizeChars,
            @Value("${lab2.rag.chunk-overlap-percent:0}") int chunkOverlapPercent) {
        this.vectorStore = vectorStore;
        this.resourceResolver = new PathMatchingResourcePatternResolver();
        this.chunkSize = requirePositive("chunk-size", chunkSize);
        this.minChunkSizeChars = requirePositive("min-chunk-size-chars", minChunkSizeChars);
        this.chunkOverlapPercent = requireOverlapPercent(chunkOverlapPercent);
    }

    /**
     * {@code lab2-docs}의 문서를 모두 재인제스트하고 문서별 청크 수를 반환한다.
     */
    public List<IngestResult> ingestAll() {
        try {
            Resource[] resources = resourceResolver.getResources(DOCUMENT_PATTERN);

            if (resources.length == 0) {
                throw new IllegalStateException(
                        "인제스트할 문서가 없습니다: src/main/resources/lab2-docs/*.md");
            }

            // 파일 순서를 고정하면 실행할 때마다 결과를 비교하기 쉽다.
            return Arrays.stream(resources)
                    .sorted(Comparator.comparing(
                            Resource::getFilename,
                            Comparator.nullsLast(String::compareTo)))
                    .map(this::ingest)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("lab2-docs 문서 목록을 읽지 못했습니다.", exception);
        }
    }

    /**
     * 문서 하나를 읽고 분할한 뒤 VectorStore에 저장한다.
     */
    private IngestResult ingest(Resource resource) {
        String source = resource.getFilename();
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("문서 파일명을 확인할 수 없습니다.");
        }

        // 1. Read — Markdown은 TextReader로 UTF-8 원문을 읽는다.
        TextReader reader = new TextReader(resource);
        reader.setCharset(StandardCharsets.UTF_8);

        // 출처와 버전은 인제스트 시점에 넣어야 검색·출처 표시·재색인에 사용할 수 있다.
        reader.getCustomMetadata().put("source", source);
        reader.getCustomMetadata().put("version", DOCUMENT_VERSION);

        List<Document> documents = reader.get();

        // 2. Split — A~E는 기존 TokenTextSplitter, F는 토큰 기반 겹침 분할기를 사용한다.
        // overlap 기본값이 0이므로 기존 실험과 운영 동작에는 변화가 없다.
        List<Document> chunks = splitDocuments(documents);

        if (chunks.isEmpty()) {
            throw new IllegalStateException("문서에서 청크를 만들지 못했습니다: " + source);
        }

        // 3. Re-index — 같은 source의 기존 청크를 지워 중복 적재를 방지한다.
        var sourceFilter = new FilterExpressionBuilder()
                .eq("source", source)
                .build();
        vectorStore.delete(sourceFilter);

        // 4. Write — VectorStore가 각 청크를 임베딩하고 저장한다.
        vectorStore.add(chunks);

        log.info("인제스트 완료 source={} version={} chunkSize={} minChunkChars={} overlap={}% chunks={}",
                source, DOCUMENT_VERSION, chunkSize, minChunkSizeChars,
                chunkOverlapPercent, chunks.size());

        return new IngestResult(source, chunks.size());
    }

    /** 잘못된 실험값으로 테스트를 시작하지 않도록 양수 여부를 검사한다. */
    private int requirePositive(String propertyName, int value) {
        if (value < 1) {
            throw new IllegalArgumentException(propertyName + "는 1 이상이어야 합니다.");
        }
        return value;
    }

    /**
     * overlap이 0이면 기존 Spring AI 분할기를 사용하고, 1~99이면 F 실험용 분할기를 사용한다.
     */
    private List<Document> splitDocuments(List<Document> documents) {
        if (chunkOverlapPercent == 0) {
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(chunkSize)
                    .withMinChunkSizeChars(minChunkSizeChars)
                    .withKeepSeparator(true)
                    .build();
            return splitter.apply(documents);
        }

        return new OverlapTokenTextSplitter(chunkSize, chunkOverlapPercent)
                .apply(documents);
    }

    /** 0은 겹침 없음, 1~99는 겹침 비율로 허용한다. */
    private int requireOverlapPercent(int overlapPercent) {
        if (overlapPercent < 0 || overlapPercent >= 100) {
            throw new IllegalArgumentException("chunk-overlap-percent는 0 이상 100 미만이어야 합니다.");
        }
        return overlapPercent;
    }
}
