package com.skala.day2.web;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 예외 → 응답 변환은 한 곳(여기)이다 — Day 1 {@code Lab1ExceptionHandler}와 같은 패턴이라 이미 완성해
 * 뒀다. 스택트레이스는 절대 노출하지 않고, 상세는 로그에만 남기고 사용자에게는 안전한 문구 + traceId만 준다
 * (03_agent-context.md 하드 규칙).
 *
 * <p>TODO(선택): Step 1에서 인제스트 실패를 구분해서 안내하고 싶다면(예: 문서 파싱 실패)
 * 전용 예외 클래스를 만들고 이 핸들러에 {@code @ExceptionHandler}를 하나 추가한다.
 * 지금 상태로도 스캐폴드의 {@code UnsupportedOperationException}은 아래 {@code unexpected()}가
 * 받아 503으로 안전하게 응답한다 — "TODO: ..." 메시지는 서버 로그에서 확인한다.
 */
@RestControllerAdvice
class Lab2ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Lab2ExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] 요청 처리 실패", traceId, e);   // 상세는 로그에만
        return ResponseEntity.status(503).body(
                new ErrorResponse("요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.", traceId));
    }
}
