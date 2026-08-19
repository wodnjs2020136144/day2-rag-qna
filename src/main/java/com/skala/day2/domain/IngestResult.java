package com.skala.day2.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 인제스트 결과 — 문서 하나가 몇 조각으로 쪼개졌는지. {@code /lab2/ingest}가 문서마다 하나씩 돌려준다.
 * 두 번 인제스트해도 이 숫자가 같아야 정상이다(재색인, 완료 기준 8번).
 */
public record IngestResult(
        @Schema(example = "return-policy.md") String source,
        @Schema(example = "4") int chunks) {}
