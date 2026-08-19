# Step 5 실험 결과 — 2026-08-19 17:26:47

> 자동 생성 — `scripts/step5-experiment.sh` 실행 결과를 그대로 append한다.
> threshold가 표에 없는 조합은 전부 0.2로 고정(한 번에 한 변수만 바꿈).

| 라벨 | chunkSize | minChunkSizeChars | topK | threshold | overlapRatio | 통과 |
|---|---|---|---|---|---|---|
| A_기준 | 400 | 200 | 4 | 0.2 | 0.0 | `2026-08-19T08:27:06.365612Z	chunkSize=400	minChunkSizeChars=200	overlapRatio=0.0	topK=4	similarityThreshold=0.2	pass=10/10` |
| B_작게 | 200 | 100 | 4 | 0.2 | 0.0 | `2026-08-19T08:27:29.209772Z	chunkSize=200	minChunkSizeChars=100	overlapRatio=0.0	topK=4	similarityThreshold=0.2	pass=10/10` |
| C_크게 | 800 | 400 | 4 | 0.2 | 0.0 | `2026-08-19T08:27:49.228854Z	chunkSize=800	minChunkSizeChars=400	overlapRatio=0.0	topK=4	similarityThreshold=0.2	pass=10/10` |
| D_넓게 | 400 | 200 | 8 | 0.2 | 0.0 | `2026-08-19T08:28:09.322616Z	chunkSize=400	minChunkSizeChars=200	overlapRatio=0.0	topK=8	similarityThreshold=0.2	pass=10/10` |
| E_엄격 | 400 | 200 | 4 | 0.7 | 0.0 | `2026-08-19T08:28:17.220873Z	chunkSize=400	minChunkSizeChars=200	overlapRatio=0.0	topK=4	similarityThreshold=0.7	pass=1/10` |
| F_겹침 | 400 | 200 | 4 | 0.2 | 0.2 | `2026-08-19T08:28:37.718243Z	chunkSize=400	minChunkSizeChars=200	overlapRatio=0.2	topK=4	similarityThreshold=0.2	pass=10/10` |
| G_채택값 | 120 | 80 | 4 | 0.2 | 0.0 | `2026-08-19T08:28:58.566338Z	chunkSize=120	minChunkSizeChars=80	overlapRatio=0.0	topK=4	similarityThreshold=0.2	pass=9/10` |

## 조합별 실패 문항 (골든셋 로그의 `실패:` 라인)

### A_기준

```
(실패 문항 없음 또는 로그에서 찾지 못함)
```

### B_작게

```
(실패 문항 없음 또는 로그에서 찾지 못함)
```

### C_크게

```
(실패 문항 없음 또는 로그에서 찾지 못함)
```

### D_넓게

```
(실패 문항 없음 또는 로그에서 찾지 못함)
```

### E_엄격

```
    2026-08-19T17:28:14.881+09:00  WARN 57588 --- [사내문서QnA_메인실습] [    Test worker] c.skala.day2.service.Lab2GoldenSetTest   : 실패: 단순 변심 반품은 며칠 이내인가요?
     답변: 확인되지 않습니다.
     출처: []
    2026-08-19T17:28:15.169+09:00  WARN 57588 --- [사내문서QnA_메인실습] [    Test worker] c.skala.day2.service.Lab2GoldenSetTest   : 실패: 물건 돌려보내려면 며칠 안에 해야 해요?
     답변: 확인되지 않습니다.
     출처: []
    2026-08-19T17:28:15.419+09:00  WARN 57588 --- [사내문서QnA_메인실습] [    Test worker] c.skala.day2.service.Lab2GoldenSetTest   : 실패: 반품 배송비는 누가 부담하나요?
     답변: 확인되지 않습니다.
     출처: []
    2026-08-19T17:28:15.550+09:00  WARN 57588 --- [사내문서QnA_메인실습] [    Test worker] c.skala.day2.service.Lab2GoldenSetTest   : 실패: 제주도는 배송비가 더 드나요?
     답변: 확인되지 않습니다.
     출처: []
    2026-08-19T17:28:15.851+09:00  WARN 57588 --- [사내문서QnA_메인실습] [    Test worker] c.skala.day2.service.Lab2GoldenSetTest   : 실패: 택배는 보통 얼마나 걸려요?
     답변: 확인되지 않습니다.
     출처: []
    2026-08-19T17:28:16.082+09:00  WARN 57588 --- [사내문서QnA_메인실습] [    Test worker] c.skala.day2.service.Lab2GoldenSetTest   : 실패: 배송은 평균 며칠 소요되나요?
     답변: 확인되지 않습니다.
     출처: []
    2026-08-19T17:28:16.342+09:00  WARN 57588 --- [사내문서QnA_메인실습] [    Test worker] c.skala.day2.service.Lab2GoldenSetTest   : 실패: 골드 등급 적립률은?
     답변: 확인되지 않습니다.
     출처: []
    2026-08-19T17:28:16.742+09:00  WARN 57588 --- [사내문서QnA_메인실습] [    Test worker] c.skala.day2.service.Lab2GoldenSetTest   : 실패: 포인트 유효기간은 얼마나 되나요?
     답변: 확인되지 않습니다.
     출처: []
    2026-08-19T17:28:16.982+09:00  WARN 57588 --- [사내문서QnA_메인실습] [    Test worker] c.skala.day2.service.Lab2GoldenSetTest   : 실패: 실버 등급이 되려면 얼마나 사야 하나요?
     답변: 확인되지 않습니다.
     출처: []
```

### F_겹침

```
(실패 문항 없음 또는 로그에서 찾지 못함)
```

### G_채택값

```
    2026-08-19T17:28:48.338+09:00  WARN 57999 --- [사내문서QnA_메인실습] [    Test worker] c.skala.day2.service.Lab2GoldenSetTest   : 실패: 물건 돌려보내려면 며칠 안에 해야 해요?
     답변: 상품 불량이나 오배송으로 인한 반품은 수령 후 30일 이내에 신고해야 합니다.
     출처: [return-policy.md]
```

