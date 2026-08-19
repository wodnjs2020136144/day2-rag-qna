package com.skala.day2.service;

import java.util.List;

/** {@code golden.json} 한 문항. {@code src}가 null이면 "근거 없음"이 정답이라는 뜻이다. */
public record Golden(String q, List<String> must, String src) {}
