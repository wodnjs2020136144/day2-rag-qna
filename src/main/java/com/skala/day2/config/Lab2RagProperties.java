package com.skala.day2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Step 5 실험표(A~F, p.226)를 재기 위해 청크·검색 파라미터를 코드 상수에서 뽑아냈다.
 *
 * <p>기본값(`application.yml`의 {@code lab2.rag.*})은 이 실습 문서(각 ~100토큰)를 기준으로 실측한
 * 값이다 — 교안 기본값(chunk-size=400, min-chunk-size-chars=200)으로는 문서 전체가 청크 1개로
 * 뭉쳐 "검색"이 사실상 "문서 단위 비교"가 돼 버린다({@code docs/session-log-2026-08-19.md} 3-1절
 * 참고). {@code scripts/step5-experiment.sh}가 {@code -Dlab2.rag.*} 시스템 프로퍼티로 조합을 바꿔
 * 가며 돌릴 때만 이 기본값에서 벗어난다.
 *
 * @param chunkSize            {@code TokenTextSplitter.withChunkSize}
 * @param minChunkSizeChars    {@code TokenTextSplitter.withMinChunkSizeChars}
 * @param overlapRatio         청크 앞에 직전 청크 꼬리를 이어 붙이는 비율(0~1). {@code TokenTextSplitter}
 *                             2.0.0에는 겹침 옵션 자체가 없어 분할 후처리로 구현했다(실험표 F 전용,
 *                             기본은 0 — 겹치지 않는다). 문자 기준 근사이지 토큰 기준이 아니다.
 * @param topK                 {@code /lab2/ask}가 근거로 쓸 상위 문서 수({@code /lab2/retrieve}는
 *                             호출자가 직접 topK를 넘기므로 영향받지 않는다)
 * @param similarityThreshold  1차 방어선 threshold. 정답·오답 점수대가 겹쳐 완전히 걸러지지 않는다는
 *                             한계는 {@code Lab2QnaService} 주석 참고 — 진짜 거절은 시스템 프롬프트의
 *                             grounded 판단(2차 방어선)이 담당한다.
 */
@ConfigurationProperties("lab2.rag")
public record Lab2RagProperties(
        int chunkSize,
        int minChunkSizeChars,
        double overlapRatio,
        int topK,
        double similarityThreshold) {
}
