# 사내문서QnA_메인실습

**Day 2 메인 실습 · 사내 문서 Q&A(RAG) API (p.219–229)**

강사 제공 샘플(07~09)과 달리 이 리포지토리는 대응하는 강사 샘플이 없다 — `08_위키QnA`(RAG 기본)와
`09_HyDE비교`를 참고해 직접 구현하는 결과물이다. 강사 샘플과 교안 자세한 설계 배경은
강의 자료 리포지토리 `skala-springai`의 `docs/SpringAI-이해-및-활용_Day2_2026-08/02_lab-guide.md`
"Day 2 메인 실습" 절과 `skala-springai/SpringAI_실습2/07_라우터와교정`·`08_위키QnA`·`09_HyDE비교`를 참조.

**부팅은 되지만 RAG 로직은 비어 있는 상태로 커밋돼 있다.** `service` 패키지의 두 파일이
`UnsupportedOperationException("TODO: ...")`을 던진다 — 이게 정상이다. 두 사람이 각자 브랜치에서
채운다.

## 실행

```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun          # VS Code 는 F5
```

## 확인

```bash
curl -X POST localhost:8080/lab2/ingest
curl --get --data-urlencode "q=반품 기한" localhost:8080/lab2/retrieve
curl -X POST --get --data-urlencode "q=반품 기한" localhost:8080/lab2/ask
```

지금 상태로 실행하면 셋 다 503(`"요청을 처리하지 못했습니다..."`)이 뜬다 — 스택트레이스는 노출되지
않고, 실제 TODO 메시지는 서버 로그에서 확인한다. 구현이 끝나면 200으로 바뀐다.

Swagger UI — <http://localhost:8080/swagger-ui.html>

키 없이 도는 테스트만 돌리려면:

```bash
./gradlew test          # Lab2ControllerTest 3건 — 컨트롤러 위임만 확인, 모델 호출 없음
```

골든 세트 평가(모델을 실제로 호출하므로 기본 테스트에서 제외돼 있다):

```bash
./gradlew test -Peval
```

---

## 협업 구조 — 경쟁이 아니라 "같은 조건, 다른 확장"

채점이 없는 실습이라 **코드를 합치지 않는다.** 대신 세 사람이 같은 조건으로 각자 완주하고,
차이는 확장 과제에서만 만든다.

```
main                             ← 이 스캐폴드 · lab2-docs · golden.json (공통 기준자)
├─ hwangjaewon/day2-rag-qna      ← 황재원: Step 1~5 완주 + 확장 과제 ①
├─ parksungwoo/day2-rag-qna      ← 박성우: Step 1~5 완주 + 확장 과제 ②
└─ lyoungah-kim/day2-rag-qna     ← 김령아: Step 1~5 완주 + 확장 과제 ③
```

브랜치명은 `<github-id>/<repo-name>` 형태다 — GitHub Flow에서 개인 소유 브랜치에 흔히 쓰는 패턴이고,
`git branch -a`만 봐도 누가 작업 중인지 바로 드러난다.

**규칙**

1. 세 브랜치는 **머지하지 않는다.** 각자 끝까지 간다.
2. `main` 변경(이 문서, `lab2-docs/*.md`, `golden.json`, 스캐폴드 코드)은 **반드시 PR로** 올리고
   서로 리뷰·승인한 뒤 머지한다. 협업 경험은 여기서 남긴다.
3. **`golden.json`은 임의로 고치지 않는다.** 10문항을 각자 다르게 재면 "나 8/10, 너 7/10"이
   아무 의미가 없다. 문항을 바꿔야 할 필요가 있으면 PR로 상의한다.
4. 완주 후 **다른 두 사람의 브랜치를 체크아웃해 자기 손으로 돌려본다.** 같은 코드인데 통과율이
   다르면 왜 다른지 판다(모델 비결정성? 청킹 파라미터? 프롬프트 문구?) — Day 1 완료 기준 5번(재현성)의
   확장판. 세 명이니 시간이 빠듯하면 최소 한 명씩은 서로 겹치지 않게 나눠 재현한다.
5. 확장 과제 전후 통과율은 PR 코멘트나 Issue에 기록하고 서로 데모로 설명한다.

**메인 실습(Step 1~3) 구현 방식은 직접 조립형으로 통일한다** — `vectorStore.similaritySearch()` →
근거 포맷팅 → 수동 프롬프트 → `.entity(AnswerDto.class)`. 교안 Step 1~3이 실제로 시킨 방식이다.
어드바이저형(`QuestionAnswerAdvisor`)은 확장 과제 메뉴에만 남겨 둔다 — 완료 기준 4번(거절)·5번
(grounded)에서 직접 조립형보다 불리하다(아래 확장 과제 표 참고).

### 시간 배분 제안 (100분, 교안 p.220 배분 기준)

| 구간 | 시간 | 내용 |
|---|---|---|
| 0. 합의 | 10분 | `golden.json` 10문항을 **둘이 함께 확정**하고 main에 고정(이미 초안이 있다 — 검토만 해도 된다). 확장 과제 축 배정 |
| 1. 인제스트 | 25분 | Step 1 — 각자 구현. 두 번 돌려 청크 수 동일 확인(완료 기준 8) |
| 2. 검색·답변 | 35분 | Step 2~3 — `/retrieve`로 점수를 눈으로 본 뒤 `/ask`. 거절·출처·grounded |
| 3. 측정 | 25분 | Step 4 — 골든 세트 실행, 실패를 ㉠/㉡로 분류. Step 5 실험표 A~D |
| 4. 정리 | 15분 | 결과보고서 기록, 서로 결과 공유 |

확장 과제는 4구간 끝난 뒤 남는 시간이나 수업 후에 붙인다(p.228: "하나 붙이고 골든 세트로 재고,
좋아지면 남긴다. 여섯 개를 한꺼번에 붙이지 않는다").

---

## TODO 체크리스트 (Step별 · 완료 기준 매핑)

| Step | 파일 | 교안 페이지 | 완료 기준 |
|---|---|---|---|
| 1. 인제스트 | `service/Lab2IngestService.java` | p.222 | 1(인제스트) · **8(재색인)** |
| 2. 검색 | `service/Lab2QnaService.java` (`retrieve`) | p.223 | 2(검색 확인) |
| 3. 답변 | `service/Lab2QnaService.java` (`ask`) | p.224 | 3(출처) · **4(거절)** · 5(구조화 응답) |
| 4. 평가 | `src/test/.../Lab2GoldenSetTest.java` | p.225 | 6(평가 8/10) |
| 5. 실험 | 결과보고서 실험표 | p.226 | 7(실험) |

**각자 흔히 걸리는 함정 한 줄씩** (파일 안 Javadoc에도 있다):

- **Step 1** — 인제스트를 두 번 돌려도 청크 수가 같아야 한다. `FilterExpressionBuilder().eq("source", …)`로
  **지우고 넣기**. `source`·`version` 메타데이터는 인제스트 시점이 아니면 나중에 못 넣는다.
  `new TokenTextSplitter()`는 deprecated — `TokenTextSplitter.builder().withChunkSize(400)...build()`.
- **Step 2** — 점수를 감추지 않는다. `/retrieve`를 먼저 만들고 눈으로 확인한 뒤 `/ask`로 넘어간다.
- **Step 3** — 근거가 비면 **모델을 부르지 않고** `AnswerDto.unknown()`을 반환한다. 프롬프트에
  거절 지시가 없으면 근거가 맞아도 지어낸다. 사용자 입력은 `{placeholder}` + `.param()`으로 바인딩
  (`.user("..." + q)`처럼 직접 이어 붙이지 않는다). 응답은 문자열 파싱이 아니라
  `.entity(AnswerDto.class)`.
- **Step 4** — 실패한 문항의 답을 반드시 읽는다. ㉠ 근거를 못 찾았다(청킹·임베딩·top-k·질문 변환
  문제) / ㉡ 찾고도 잘못 답했다(프롬프트·모델·근거 포맷 문제) — 둘은 고칠 곳이 완전히 다르다.

---

## 확장 과제 메뉴 — 서로 다른 축을 하나씩 고른다

같은 축이면 서로 공유할 때 들을 얘기가 없다. 세 명이 **검색 품질 · 운영·보안 · 아키텍처(또는 대화)**
축을 하나씩 나눠 갖는 걸 권장한다.

| 축 | 확장 과제 | 골든셋 숫자가 움직이나 | 참조 | 난이도 | 배우는 것 |
|---|---|---|---|---|---|
| **검색 품질** | 질문 변환(HyDE) | ✅ — golden.json에 표현 바꾼 문항이 있다 | `skala-springai/SpringAI_실습2/09_HyDE비교` 그대로 | 낮음 | 검색 실패의 진짜 원인은 어휘 차이 |
| | 재순위(rerank) | ✅ | 없음 (9장) | 높음 | 후보 20 → 상위 4 |
| | 하이브리드 검색 | ✅ | 없음 (9장) | 높음 | 고유명사·상품코드에 강해짐 |
| **운영·보안** | 메타데이터 필터 | ❌ — 별도 권한 테스트 필요 | 교안 `skala-springai/docs/.../01_study-guide.md` §28 (p.192) | 중간 | **권한은 프롬프트가 아니라 검색으로 강제한다**(Day1 권한 격리와 직결) |
| | pgvector 전환 | ❌ — 재시작 후 유지 확인 | `docker-compose.yml` | 낮음 | 인메모리 → 실제 저장소, HNSW 인덱스 |
| **아키텍처** | 어드바이저 전환 | ➖ 비슷할 것 | 교안 `skala-springai/docs/.../01_study-guide.md` §28 (p.187–192) | 중간 | 코드 40줄 → 3줄, 대신 거절·grounded가 어려워짐(아래 참고) |
| **대화** | 대화형 RAG | ➖ 골든셋으로 못 잼 | 12장(이후 회차) | 높음 | 후속 질문의 대명사 해석 |

> **"어드바이저 전환"을 고를 때 주의**: `QuestionAnswerAdvisor`는 근거가 비어도 모델을 부른다 —
> 거절(완료 기준 4번)을 코드가 아니라 프롬프트로만 막아야 한다. `grounded` 플래그도 직접 조립형처럼
> "내가 센 근거 개수"가 아니라 어드바이저의 `RETRIEVED_DOCUMENTS` 컨텍스트에서 꺼내야 한다
> (교안 `01_study-guide.md` §29). 시간이 빠듯하면 이 확장은 뒤로 미룬다.

---

## `08_위키QnA`·`09_HyDE비교`를 참고할 때 그대로 베끼면 안 되는 것

강사 샘플은 개념 하나만 드러내려는 축약 코드다 — 이 프로젝트의 하드 규칙과 다르게 짜여 있다.

1. `.user("[근거]%n%s%n[질문] %s".formatted(합치기(근거), q))` — 사용자 입력을 문자열에 직접
   이어 붙인다. 이 프로젝트는 `{placeholder}` + `.param()` 바인딩을 쓴다
   (`skala-springai` 리포지토리 `docs/SpringAI-이해-및-활용_Day1_2026-08/03_agent-context.md` 하드 규칙).
2. `09_HyDE비교`는 `@RestController`가 `VectorStore`를 직접 호출한다 — 컨트롤러 직접 호출 금지
   규칙 위반이다. 이 프로젝트는 컨트롤러가 `ChatClient`/`VectorStore`를 모른다.
3. `application.yml`의 전역 `temperature: 0.7` — 이 프로젝트는 용도별 `ChatClient` 빈
   (`Lab2AiConfig`)에 `0.0`으로 고정한다.
4. `new TokenTextSplitter()` — deprecated 생성자다. `TokenTextSplitter.builder()...build()`를
   쓴다(Step 5 실험표의 청크 크기 실험을 하려면 builder가 필요하다).
5. `08`은 `similarityThreshold(0.5)`, `09`는 threshold가 없다 — HyDE 전후를 비교할 때는 같은
   조건(둘 다 threshold 적용 또는 둘 다 미적용)으로 재야 공정하다.

`09_HyDE비교`는 `08_위키QnA`의 상위집합(같은 `lab8-docs`·로직을 `lab9` 패키지로 복제)이라 `09`
하나만 띄우면 `/lab8/ingest`와 `/lab9/compare`를 함께 쓸 수 있다. 확장 과제로 HyDE를 고른 사람은
이 구조를 그대로 참고하면 된다.

---

## 1.1.8 → 2.0.0 이식 메모

이 프로젝트는 Day 1 메인 실습·`skala-springai`의 `CLAUDE.md` 고정값과 맞춰
**Boot 4.1.0 + spring-ai-bom 2.0.0**을 쓴다. 강사 샘플(`skala-springai/SpringAI_실습2`의
`07_라우터와교정`·`08_위키QnA`·`09_HyDE비교`, 그리고 `SpringAI_실습`의 02~06)은 전부
**Boot 3.5.16 + spring-ai 1.1.8**이다. 코드를 참고할 때 아래를 바꿔서 옮긴다.

| 강사 샘플(1.1.8) | 이 프로젝트(2.0.0) |
|---|---|
| `.defaultOptions(ChatOptions.builder()...build())` | `.build()` 없이 Builder를 그대로 넘긴다(`Lab2AiConfig` 참고) |
| `new TokenTextSplitter()` | `TokenTextSplitter.builder().withChunkSize(400)...build()` |
| BOM: `ext { springAiVersion = '1.1.8' } / dependencyManagement { imports { ... } }` | `implementation platform("org.springframework.ai:spring-ai-bom:2.0.0")` |
| Boot 3.5.16의 `@WebMvcTest` | Boot 4는 `spring-boot-starter-webmvc-test` 스타터가 별도로 있어야 딸려온다(이미 build.gradle에 있다) |
| `QuestionAnswerAdvisor`가 필요할 때 | 좌표는 `org.springframework.ai:spring-ai-vector-store-advisor`다. ⚠️ 이름을 추측하면
`spring-ai-advisors-vector-store`처럼 틀리기 쉽다 — 실제 컴파일로 확정한 값이다(BOM에 여러 후보
아티팩트가 있으니 확신 없으면 `./gradlew dependencies`로 재확인한다) |

인메모리 `SimpleVectorStore`(`org.springframework.ai:spring-ai-vector-store`), `TokenTextSplitter`,
`TextReader`는 1.1.8과 2.0.0 양쪽 다 같은 좌표·패키지로 존재한다(2.0.0에서 실제 컴파일로 확인함).

## 이 폴더에 있는 것

- `domain/Chunk.java`, `AnswerDto.java`, `IngestResult.java` — 응답 모양(완성, 손대지 않는다)
- `config/Lab2VectorStoreConfig.java` — 인메모리 VectorStore 빈(완성)
- `config/Lab2AiConfig.java` — 답변 전용 `ChatClient` 빈(완성 — temperature·maxTokens는 조정 가능)
- `service/Lab2IngestService.java` — **TODO: Step 1**
- `service/Lab2QnaService.java` — **TODO: Step 2·3**
- `web/Lab2Controller.java` — 3개 엔드포인트(완성, 손대지 않는다)
- `web/Lab2ExceptionHandler.java`, `ErrorResponse.java` — 예외 응답(완성)
- `src/main/resources/lab2-docs/` — 반품·배송·멤버십 규정 3종
- `src/test/resources/golden.json` — 골든 세트 10문항(고정, PR로만 변경)
- `src/test/.../Lab2GoldenSetTest.java` — **TODO: Step 4**(채점 로직)
- `src/test/.../Lab2ControllerTest.java` — 컨트롤러 스모크 테스트(완성, 참고용)
- `docker-compose.yml` — 확장 과제 "pgvector 전환" 전용
