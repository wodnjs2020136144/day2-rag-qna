package com.skala.day2.web;

/** 예외 응답 — 스택트레이스는 절대 노출하지 않는다. 상세는 로그에, 사용자에게는 추적 ID만. */
public record ErrorResponse(String message, String traceId) {}
