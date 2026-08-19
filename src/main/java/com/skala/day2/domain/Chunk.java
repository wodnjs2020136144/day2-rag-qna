package com.skala.day2.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 검색 결과 하나. {@code /lab2/retrieve}가 그대로 노출한다 — 점수를 감추지 않는다(교안 p.223).
 *
 * <p>두 사람이 같은 모양으로 결과를 비교할 수 있도록 이 record는 고정한다.
 */
public record Chunk(
        @Schema(example = "return-policy.md") String source,
        @Schema(example = "0.71") double score,
        @Schema(example = "단순 변심에 의한 반품은 상품 수령 후 7일 이내...") String snippet) {}
