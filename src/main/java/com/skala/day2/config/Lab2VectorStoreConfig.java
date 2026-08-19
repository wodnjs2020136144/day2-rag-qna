package com.skala.day2.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Day 2 메인 실습 — 인메모리 벡터 저장소.
 *
 * <p>따로 설치할 것이 없다. 앱을 끄면 넣어 둔 문서도 사라지므로, 다시 띄우면 인제스트부터 한다.
 * 확장 과제 "pgvector 전환"에서는 이 빈을 걷어내고 {@code docker-compose.yml} 안내를 따른다.
 */
@Configuration
public class Lab2VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
