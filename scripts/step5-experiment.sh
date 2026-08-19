#!/usr/bin/env bash
# Step 5 — 실험표(A~F + G) 측정 스크립트 (교안 p.226, 완료 기준 7번).
#
# 조합마다 -Dlab2.rag.* 시스템 프로퍼티로 청크 크기·topK·threshold·겹침 비율을 바꿔
# `./gradlew test -Peval`을 돌리고, 실행 로그와 build/step5/result.txt(통과 수)를
# docs/step5-실험결과.md에 조합별로 누적 append한다.
#
# G행(chunkSize=120/minChars=80)은 교안 표에는 없지만 이 프로젝트가 실제로 채택한 값이라
# 기준선으로 함께 잰다 — docs/session-log-2026-08-19.md 3-1~3-2절 참고.
#
# threshold가 표에 적히지 않은 행은 전부 0.2로 고정한다 — 실험은 한 번에 한 변수만 바꿔야
# 비교가 된다.
#
# 실행: OPENAI_API_KEY가 있는 터미널에서 `./scripts/step5-experiment.sh`
# 비용: 조합 7개 x golden.json 10문항 = gpt-4o-mini 호출 70회 + 임베딩(무시할 수준).

set -uo pipefail
cd "$(dirname "$0")/.."

if [ -z "${OPENAI_API_KEY:-}" ]; then
  echo "OPENAI_API_KEY가 설정돼 있지 않다. export OPENAI_API_KEY=\"sk-...\" 후 다시 실행한다." >&2
  exit 1
fi

OUT_MD="docs/step5-실험결과.md"
RESULT_FILE="build/step5/result.txt"
RAW_LOG_DIR="build/step5/logs"
mkdir -p "$RAW_LOG_DIR"
rm -f "$RESULT_FILE"   # 이전 세션의 잔여 기록과 섞이지 않게 시작 전에 비운다

# 라벨 | chunkSize | minChunkSizeChars | topK | threshold | overlapRatio
COMBINATIONS=(
  "A_기준|400|200|4|0.2|0.0"
  "B_작게|200|100|4|0.2|0.0"
  "C_크게|800|400|4|0.2|0.0"
  "D_넓게|400|200|8|0.2|0.0"
  "E_엄격|400|200|4|0.7|0.0"
  "F_겹침|400|200|4|0.2|0.2"
  "G_채택값|120|80|4|0.2|0.0"
)

{
  echo "# Step 5 실험 결과 — $(date '+%Y-%m-%d %H:%M:%S')"
  echo
  echo "> 자동 생성 — \`scripts/step5-experiment.sh\` 실행 결과를 그대로 append한다."
  echo "> threshold가 표에 없는 조합은 전부 0.2로 고정(한 번에 한 변수만 바꿈)."
  echo
  echo "| 라벨 | chunkSize | minChunkSizeChars | topK | threshold | overlapRatio | 통과 |"
  echo "|---|---|---|---|---|---|---|"
} > "$OUT_MD"

SUMMARY_ROWS=()

for combo in "${COMBINATIONS[@]}"; do
  IFS='|' read -r label chunk minChars topK threshold overlap <<< "$combo"
  echo "=== [$label] chunkSize=$chunk minChunkSizeChars=$minChars topK=$topK threshold=$threshold overlapRatio=$overlap ==="

  LOG_FILE="$RAW_LOG_DIR/${label}.log"
  ./gradlew test -Peval \
    -Dlab2.rag.chunk-size="$chunk" \
    -Dlab2.rag.min-chunk-size-chars="$minChars" \
    -Dlab2.rag.top-k="$topK" \
    -Dlab2.rag.similarity-threshold="$threshold" \
    -Dlab2.rag.overlap-ratio="$overlap" \
    > "$LOG_FILE" 2>&1
  status=$?

  # result.txt는 append 방식이라 이번 실행분(마지막 줄)만 뽑는다.
  pass_line=$(tail -n 1 "$RESULT_FILE" 2>/dev/null || echo "결과 없음(테스트 자체가 실행되지 않았을 수 있다)")

  echo "  -> $pass_line (gradle exit=$status, 로그: $LOG_FILE)"
  SUMMARY_ROWS+=("$label|$pass_line|$status")

  {
    echo "| $label | $chunk | $minChars | $topK | $threshold | $overlap | \`$pass_line\` |"
  } >> "$OUT_MD"
done

{
  echo
  echo "## 조합별 실패 문항 (골든셋 로그의 \`실패:\` 라인)"
  echo
  for row in "${SUMMARY_ROWS[@]}"; do
    IFS='|' read -r label _ _ <<< "$row"
    echo "### $label"
    echo
    echo '```'
    grep -A2 "실패:" "$RAW_LOG_DIR/${label}.log" || echo "(실패 문항 없음 또는 로그에서 찾지 못함)"
    echo '```'
    echo
  done
} >> "$OUT_MD"

echo
echo "완료. 결과: $OUT_MD (원본 로그: $RAW_LOG_DIR/)"
