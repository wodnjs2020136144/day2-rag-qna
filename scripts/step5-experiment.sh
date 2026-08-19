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
# 사용법:
#   ./scripts/step5-experiment.sh                       # 7개 조합 전체 — 표를 새로 쓴다
#   ./scripts/step5-experiment.sh --combos E_엄격,G_채택값  # 지정한 조합만 — 기존 표는 보존하고
#                                                           # "재실행" 섹션만 append한다
# 비용: 조합 7개 x golden.json 10문항 = gpt-4o-mini 호출 70회 + 임베딩(무시할 수준).

set -uo pipefail
cd "$(dirname "$0")/.."

if [ -z "${OPENAI_API_KEY:-}" ]; then
  echo "OPENAI_API_KEY가 설정돼 있지 않다. export OPENAI_API_KEY=\"sk-...\" 후 다시 실행한다." >&2
  exit 1
fi

FILTER=""
if [ "${1:-}" = "--combos" ]; then
  FILTER="${2:-}"
fi

OUT_MD="docs/step5-실험결과.md"
RESULT_FILE="build/step5/result.txt"
RAW_LOG_DIR="build/step5/logs"
mkdir -p "$RAW_LOG_DIR"

# 라벨 | chunkSize | minChunkSizeChars | topK | threshold | overlapRatio
ALL_COMBINATIONS=(
  "A_기준|400|200|4|0.2|0.0"
  "B_작게|200|100|4|0.2|0.0"
  "C_크게|800|400|4|0.2|0.0"
  "D_넓게|400|200|8|0.2|0.0"
  "E_엄격|400|200|4|0.7|0.0"
  "F_겹침|400|200|4|0.2|0.2"
  "G_채택값|120|80|4|0.2|0.0"
)

COMBINATIONS=()
if [ -n "$FILTER" ]; then
  IFS=',' read -ra WANTED <<< "$FILTER"
  for combo in "${ALL_COMBINATIONS[@]}"; do
    label="${combo%%|*}"
    for w in "${WANTED[@]}"; do
      if [ "$label" = "$w" ]; then
        COMBINATIONS+=("$combo")
      fi
    done
  done
  if [ "${#COMBINATIONS[@]}" -eq 0 ]; then
    echo "지정한 조합을 찾지 못했다: $FILTER" >&2
    exit 1
  fi
  echo "부분 실행: ${FILTER} — 기존 $OUT_MD 표는 건드리지 않고 재실행 섹션만 append한다."
else
  COMBINATIONS=("${ALL_COMBINATIONS[@]}")
  rm -f "$RESULT_FILE"   # 전체 실행일 때만 이전 세션의 잔여 기록과 섞이지 않게 비운다
  {
    echo "# Step 5 실험 결과 — $(date '+%Y-%m-%d %H:%M:%S')"
    echo
    echo "> 자동 생성 — \`scripts/step5-experiment.sh\` 실행 결과를 그대로 append한다."
    echo "> threshold가 표에 없는 조합은 전부 0.2로 고정(한 번에 한 변수만 바꿈)."
    echo
    echo "| 라벨 | chunkSize | minChunkSizeChars | topK | threshold | overlapRatio | 통과 |"
    echo "|---|---|---|---|---|---|---|"
  } > "$OUT_MD"
fi

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

  # 보험 — 콘솔 로그가 또 새더라도 test-results XML에는 항상 전체 로그가 남는다.
  XML_FILE="build/test-results/test/TEST-com.skala.day2.service.Lab2GoldenSetTest.xml"
  if [ -f "$XML_FILE" ]; then
    cp "$XML_FILE" "$RAW_LOG_DIR/${label}.xml"
  fi

  # result.txt는 append 방식이라 이번 실행분(마지막 줄)만 뽑는다.
  pass_line=$(tail -n 1 "$RESULT_FILE" 2>/dev/null || echo "결과 없음(테스트 자체가 실행되지 않았을 수 있다)")

  echo "  -> $pass_line (gradle exit=$status, 로그: $LOG_FILE)"
  SUMMARY_ROWS+=("$label")

  if [ -z "$FILTER" ]; then
    echo "| $label | $chunk | $minChars | $topK | $threshold | $overlap | \`$pass_line\` |" >> "$OUT_MD"
  fi
done

{
  echo
  if [ -n "$FILTER" ]; then
    echo "## 재실행 — $(date '+%Y-%m-%d %H:%M:%S') (${FILTER})"
  else
    echo "## 조합별 실패 문항 (골든셋 로그의 \`실패:\` 라인)"
  fi
  echo
  for label in "${SUMMARY_ROWS[@]}"; do
    echo "### $label"
    echo
    echo '```'
    grep -A2 "실패:" "$RAW_LOG_DIR/${label}.log" \
      || grep -A2 "실패:" "$RAW_LOG_DIR/${label}.xml" 2>/dev/null \
      || echo "(실패 문항 없음 또는 로그에서 찾지 못함)"
    echo '```'
    echo
  done
} >> "$OUT_MD"

echo
echo "완료. 결과: $OUT_MD (원본 로그: $RAW_LOG_DIR/)"
