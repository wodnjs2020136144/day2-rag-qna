package com.skala.day2.domain;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * {@code /lab2/ask}의 응답. 문자열이 아니라 구조화 출력으로 받는다(교안 p.224) — 답변·출처·근거
 * 사용 여부를 한 번에 검증할 수 있다. 두 사람이 같은 모양으로 결과를 비교할 수 있도록 이 record는 고정한다.
 */
public record AnswerDto(
        @Schema(example = "단순 변심 반품은 상품 수령 후 7일 이내 가능합니다. [출처: return-policy.md]")
        String answer,
        @Schema(example = "[\"return-policy.md\"]") List<String> sources,
        @Schema(description = "검색된 근거를 실제로 사용해 답했는지") boolean grounded) {

    /** 근거가 없어 모델을 부르지 않았을 때 반환한다 — 완료 기준 4번(거절). */
    public static AnswerDto unknown() {
        return new AnswerDto("확인되지 않습니다.", List.of(), false);
    }
}
