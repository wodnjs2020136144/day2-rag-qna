package com.skala.day2.service;

import java.io.IOException;
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

@Service
public class Lab2IngestService {

    private final VectorStore vectorStore;

    public Lab2IngestService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<IngestResult> ingestAll() {

        try {
            // 1. lab2-docs 아래의 모든 md 문서를 찾는다.
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:lab2-docs/*.md");

            // 2. 문서를 400 토큰 기준으로 분할한다.
            var splitter = TokenTextSplitter.builder()
                    .withChunkSize(400)
                    .withMinChunkSizeChars(200)
                    .build();

            List<IngestResult> results = new ArrayList<>();

            for (Resource resource : resources) {

                String source = resource.getFilename();

                if (source == null) {
                    continue;
                }

                // 3. 파일을 Document로 읽고 메타데이터를 넣는다.
                TextReader reader = new TextReader(resource);
                reader.getCustomMetadata().put("source", source);
                reader.getCustomMetadata().put("version", "v1");

                List<Document> documents = reader.get();

                // 4. Document를 여러 chunk로 분할한다.
                List<Document> chunks = splitter.apply(documents);

                // 5. 같은 source가 이미 있으면 먼저 삭제한다.
                var filter = new FilterExpressionBuilder()
                        .eq("source", source)
                        .build();

                vectorStore.delete(filter);

                // 6. 새 chunk를 VectorStore에 저장한다.
                vectorStore.add(chunks);

                // 7. 파일별 chunk 개수를 결과에 기록한다.
                results.add(new IngestResult(source, chunks.size()));
            }

            return results;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "lab2-docs 문서를 읽는 중 오류가 발생했습니다.", e);
        }
    }
}
