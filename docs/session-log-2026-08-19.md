# 세션 작업 로그 — 2026-08-19

> ⚠️ 이 문서는 `docs/결과보고서.md`(교육 산출물)가 아니다. 이 세션(경로 복구 ~ 완료 기준 검증)에서
> 실제로 무엇을 했고 왜 그렇게 했는지 남기는 작업 로그다. 다음 세션(Step 5 실험표·결과보고서 작성)이
> 여기서 이어받을 수 있게 하는 게 목적이다.

**브랜치**: `hwangjaewon/day2-rag-qna` · **범위**: 경로 복구 + Step 1~3 구현 + 완료 기준 8개 검증

---

## 1. 배경 — 경로 복구

브랜치의 이전 커밋(`9a2ec80 main branch 머지 완료`)에서 소스·문서 경로가 Gradle 표준 소스셋 밖으로
잘못 이동돼 있었다(IDE 드래그 사고로 추정, 내용 변경 없이 순수 경로 이동만 있었음):

```
src/main/java/com/skala/day2/**     →  src/main/resources/java/com/skala/day2/**
src/main/resources/lab2-docs/*.md   →  src/test/lab2-docs/*.md
```

이 상태로는 컴파일 자체가 되지 않아, `git mv`로 원래 경로로 되돌렸다 (`22ded14`).

---

## 2. Step 1~3 구현

| Step | 파일 | 내용 |
|---|---|---|
| 1. 인제스트 | `Lab2IngestService` | classpath에서 `lab2-docs/*.md` 검색 → `TokenTextSplitter`로 분할 → `source`·`version` 메타데이터 부여 → 같은 `source`를 필터로 지운 뒤 재삽입(재색인) |
| 2. 검색 | `Lab2QnaService#retrieve` | `vectorStore.similaritySearch()` → `Chunk(source, score, snippet)`로 점수를 감추지 않고 노출 |
| 3. 답변 | `Lab2QnaService#ask` | 근거가 비면 모델을 부르지 않고 `AnswerDto.unknown()` 반환. 근거가 있으면 `{placeholder}`+`.param()` 바인딩으로 프롬프트를 구성해 `.entity(AnswerDto.class)`로 구조화 답변. `finishReason=length`는 예외로 던져 `Lab2ExceptionHandler`가 503으로 처리 |

---

## 3. 발견한 문제와 튜닝 과정 (이번 세션의 핵심)

### 3-1. 청크가 문서 단위로 뭉치는 문제

`lab2-docs/*.md` 3개 문서는 각각 ~100토큰밖에 안 되는데, 기본 청크 파라미터(`chunkSize=400`,
`minChunkSizeChars=200`)로는 문서 전체가 **청크 1개**로만 인제스트됐다. 그 결과 "검색"이 사실상
"문서 전체 단위 유사도 비교"가 돼 버려, "우주 배송"처럼 무관한 질문도 "배송"이라는 단어가 겹치는
`shipping-policy.md` 전체와 높은 점수로 매칭됐다.

**조치**: `CHUNK_SIZE=120`, `MIN_CHUNK_SIZE_CHARS=80`으로 낮춰 섹션 단위로 재분할했다. 재인제스트
결과 `membership.md` 4청크, `return-policy.md` 4청크, `shipping-policy.md` 3청크로 쪼개졌다.

### 3-2. threshold 재측정 — 여전히 분리 불가능하다는 결론

청크를 쪼갠 뒤 golden.json 10문항 전체를 `/lab2/retrieve`(topK=6)로 실측했다:

- 정답 청크 점수: 대략 0.22 ~ 0.54
- "우주 배송"(오답, 근거 없음이 정답) 최고 점수: **0.4513** — 정답 점수대 한가운데

즉 "배송"이라는 단어 자체가 겹치는 한, 청크를 아무리 잘게 쪼개도 벡터 유사도 하나로는 정답과 오답을
분리할 수 없다는 게 실측으로 확인됐다.

**결론**: threshold(`0.2`)는 "점수가 완전히 바닥인" 극단적 무관 질문만 거르는 1차 방어선으로 두고,
실제 거절 판단은 시스템 프롬프트("근거에 없으면 확인되지 않습니다")의 grounded 판단(2차 방어선)에
맡겼다. 코드(`ask()`)는 evidence가 완전히 비었을 때만 모델을 부르지 않고 즉시 거절한다.

실제 `/lab2/ask` 호출로 두 경로 모두 검증됨:
- "단순 변심 반품은 며칠 이내인가요?" → `grounded: true`, 정상 답변 + 출처
- "우주 배송도 되나요?" → `grounded: false`, "확인되지 않습니다" (evidence는 있었지만 모델이 거절)

### 3-3. `sources` 대괄호 오염 버그

정상 답변의 `sources` 필드에 `"[return-policy.md]"`처럼 대괄호가 그대로 섞여 나오는 문제를 발견했다.
프롬프트 컨텍스트를 `[source] 본문` 형식으로 넣었더니 모델이 대괄호까지 그대로 베낀 것.

**조치**: 컨텍스트 포맷을 `출처: %s%n%s`로 바꾸고, `Lab2QnaService#sanitizeSources()`를 추가해
응답의 `sources`에서 대괄호·"출처:" 접두어를 방어적으로 제거하도록 했다. 재검증 결과
`sources: ["return-policy.md"]`로 깨끗하게 나옴을 확인.

---

## 4. 검증 결과

### 완료 기준 8개 캡처 (`docs/images/`)

| 파일 | 기준 | 내용 |
|---|---|---|
| `01-ingest-1st.jpg` | 1 | 1회차 인제스트 — 문서 3종, 청크 4/4/3 |
| `02-retrieve.jpg` | 2 | `/lab2/retrieve?q=반품 기한` — 점수·출처 노출 |
| `03-05-ask-answer.jpg` | 3·5 | 정상 답변 — `answer`/`sources`/`grounded` 구조화 응답 |
| `04-ask-reject.jpg` | 4 | "우주 배송도 되나요?" → 거절 |
| `08-reindex-2nd.jpg` | 8 | 2회차 인제스트 — 청크 수 1회차와 동일 |

### 골든 세트 평가 (완료 기준 6번)

`./gradlew test -Peval` 실행 결과 **통과 9/10**.

유일한 실패:
- 질문: "물건 돌려보내려면 며칠 안에 해야 해요?" (정답: "7일", `return-policy.md`의 단순 변심 반품 섹션)
- 실제 답변: "상품 불량이나 오배송으로 인한 반품은 수령 후 30일 이내에 신고해야 합니다."
- 출처는 `return-policy.md`로 맞았지만, **같은 문서 안의 다른 섹션**(불량·오배송 규정)을 근거로 답함.
- 분류: ㉠에 가까움 — 섹션 단위 청킹에서 top-4 안에 정답 섹션("단순 변심 반품")이 충분히 높은
  순위로 들지 못했을 가능성. Step 5 실험 D(topK 확대)나 확장 과제 HyDE로 개선 여지가 있다.

---

## 5. 커밋 목록

| 커밋 | 내용 |
|---|---|
| `22ded14` | 잘못 이동된 소스·문서 경로를 Gradle 표준 소스셋으로 복구 |
| `b65bed1` | Step 1~3 RAG 로직 구현 — 인제스트·검색·답변(직접 조립형) |
| `74544bd` | 골든셋 테스트에 인제스트 선행 단계 추가 (`@BeforeEach`) |
| `03d653e` | 완료 기준 검증 캡처 추가 (`docs/images/*`) |
| `f46e1c5` | main의 3인 구성 반영(`683344a`)을 동기화 — 이 브랜치가 2인 구성 시절 갈라져 나와 있던 README·결과보고서 반영 누락분을 병합으로 해소 |

---

## 6. 남은 작업 (이번 세션 범위 밖)

- **Step 5 실험표(A~F)** — 청크 크기(200/400/800)·topK·threshold 조합별 골든셋 통과율 측정.
  3-1·3-2에서 이미 청크 120·threshold 0.2 조합의 실측치를 확보해 뒀으니 이어서 바로 시작 가능.
- **`docs/결과보고서.md` 실제 작성** — 지금까지의 실측 수치(청크 수, 점수 분포, 9/10 결과, 실패 유형
  분석)를 표에 옮겨 채운다.
- **확장 과제** — 아직 미정.
