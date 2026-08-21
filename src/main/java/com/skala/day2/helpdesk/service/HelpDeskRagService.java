package com.skala.day2.helpdesk.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.skala.day2.helpdesk.domain.HelpDeskDomain;
import com.skala.day2.helpdesk.domain.RagChunk;

@Service
public class HelpDeskRagService {

    private final VectorStore vectorStore;

    public HelpDeskRagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<RagChunk> ingestAll() {

        List<RagChunk> results = new ArrayList<>();

        results.addAll(ingestDomain(
                HelpDeskDomain.SEMICONDUCTOR,
                "classpath*:helpdesk-docs/semiconductor/*.md",
                "Semiconductor Fab Security Policy"));

        results.addAll(ingestDomain(
                HelpDeskDomain.FINANCE,
                "classpath*:helpdesk-docs/finance/*.md",
                "Finance Customer Data Security Policy"));

        return results;
    }

    private List<RagChunk> ingestDomain(
            HelpDeskDomain domain,
            String resourcePattern,
            String title) {

        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(resourcePattern);

            var splitter = TokenTextSplitter.builder()
                    .withChunkSize(350)
                    .withMinChunkSizeChars(150)
                    .build();

            List<RagChunk> results = new ArrayList<>();

            for (Resource resource : resources) {

                String source = resource.getFilename();

                if (source == null) {
                    continue;
                }

                TextReader reader = new TextReader(resource);

                reader.getCustomMetadata().put("domain", domain.name());
                reader.getCustomMetadata().put("source", source);
                reader.getCustomMetadata().put("title", title);
                reader.getCustomMetadata().put("version", "v1.0");

                List<Document> chunks =
                        splitter.apply(reader.get());

                // 같은 파일을 다시 인제스트해도 chunk가 중복되지 않게 삭제 후 저장
                var filter = new FilterExpressionBuilder()
                        .eq("source", source)
                        .build();

                vectorStore.delete(filter);
                vectorStore.add(chunks);

                results.add(new RagChunk(
                        domain,
                        source,
                        title,
                        "v1.0",
                        chunks.size(),
                        "인제스트 완료"));
            }

            return results;

        } catch (IOException e) {
            throw new IllegalStateException(
                    domain + " 문서 인제스트 중 오류가 발생했습니다.", e);
        }
    }

    public List<RagChunk> retrieve(
            HelpDeskDomain domain,
            String question,
            int topK) {

        var filter = new FilterExpressionBuilder()
                .eq("domain", domain.name())
                .build();

        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(question)
                                .topK(topK)
                                .similarityThresholdAll()
                                .filterExpression(filter)
                                .build());

        return documents.stream()
                .map(document -> {

                    String source = String.valueOf(
                            document.getMetadata().get("source"));

                    String title = String.valueOf(
                            document.getMetadata().get("title"));

                    String version = String.valueOf(
                            document.getMetadata().get("version"));

                    double score =
                            document.getScore() == null
                                    ? 0.0
                                    : document.getScore();

                    String text =
                            document.getText() == null
                                    ? ""
                                    : document.getText();

                    String snippet =
                            text.length() <= 160
                                    ? text
                                    : text.substring(0, 160);

                    return new RagChunk(
                            domain,
                            source,
                            title,
                            version,
                            score,
                            snippet);
                })
                .toList();
    }
}
