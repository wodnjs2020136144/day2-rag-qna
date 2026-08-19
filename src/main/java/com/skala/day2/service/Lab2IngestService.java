package com.skala.day2.service;

import java.util.List;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.skala.day2.domain.IngestResult;

/**
 * TODO: Step 1 — 인제스트 (교안 p.222)
 *
 * <p>읽기 → 분할 → 메타데이터 → 저장 순서로 구현한다. 참고: 교안 lab-guide
 * `02_lab-guide.md`의 "Step 1 — 인제스트" 코드 예시, 강사 샘플 `08_위키QnA/WikiRag.java`의 {@code 넣기()}.
 *
 * <p>채울 것:
 * <ol>
 *   <li>{@code src/main/resources/lab2-docs/*.md}를 전부 읽는다
 *       ({@code PathMatchingResourcePatternResolver}로 classpath 패턴 검색, 08_위키QnA 참고)</li>
 *   <li>{@code TokenTextSplitter.builder().withChunkSize(400).withMinChunkSizeChars(200).build()}로
 *       분할한다. ⚠️ {@code new TokenTextSplitter()} 생성자는 쓰지 않는다 — deprecated다.
 *       Step 5 실험표(청크 200/400/800)를 채우려면 builder여야 파라미터를 바꿀 수 있다.</li>
 *   <li>{@code source}(파일명)·{@code version} 메타데이터를 이 시점에 반드시 넣는다 — 나중엔 못 넣는다.
 *       출처 표기(완료 기준 3번)가 여기서 시작한다.</li>
 *   <li>같은 {@code source}를 {@code FilterExpressionBuilder().eq("source", ...)}로 지운 뒤 다시
 *       넣는다(재색인). 두 번 인제스트해도 조각 수가 늘지 않아야 정상이다 — 완료 기준 8번,
 *       "오늘의 진짜 학습 지점" 중 하나(p.229).</li>
 * </ol>
 */
@Service
public class Lab2IngestService {

    private final VectorStore vectorStore;

    public Lab2IngestService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /** lab2-docs 아래 문서를 전부 (재)인제스트하고 문서별 결과를 돌려준다. */
    public List<IngestResult> ingestAll() {
        throw new UnsupportedOperationException(
                "TODO: Step 1 — 교안 p.222. lab2-docs/*.md를 읽어 분할·메타데이터·재색인 후 저장한다.");
    }
}
