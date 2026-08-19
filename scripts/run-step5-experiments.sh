#!/usr/bin/env bash

# Day 2 Step 5 — A~F RAG 설정을 한 번에 비교한다.
#
# 실행:
#   ./scripts/run-step5-experiments.sh
#
# 결과:
#   build/step5-experiments/A.log ~ F.log
#   build/step5-experiments/summary.md
#   build/step5-experiments/summary.html
#
# 주의:
# - 실제 OpenAI 임베딩과 채팅 모델을 호출한다.
# - 한 조합이 8/10 미만으로 테스트에 실패해도 다음 조합을 계속 실행한다.
# - golden.json은 변경하지 않고 모든 조합이 동일한 10문항을 사용한다.

set -uo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly RESULT_DIR="${PROJECT_DIR}/build/step5-experiments"
readonly SUMMARY_FILE="${RESULT_DIR}/summary.md"
readonly HTML_FILE="${RESULT_DIR}/summary.html"
readonly TEST_CLASS="com.skala.day2.service.Lab2GoldenSetTest"
readonly DELAY_SECONDS="${LAB2_EXPERIMENT_DELAY_SECONDS:-2}"

if [[ -z "${OPENAI_API_KEY:-}" ]]; then
    echo "오류: OPENAI_API_KEY가 현재 터미널에 등록되어 있지 않습니다." >&2
    echo '먼저 export OPENAI_API_KEY="발급받은_API_KEY"를 실행하세요.' >&2
    exit 1
fi

if [[ ! -x "${PROJECT_DIR}/gradlew" ]]; then
    echo "오류: Gradle Wrapper를 찾거나 실행할 수 없습니다: ${PROJECT_DIR}/gradlew" >&2
    exit 1
fi

mkdir -p "${RESULT_DIR}"

# 실행할 조합을 표와 동일한 순서로 정의한다.
# 형식: 이름|청크 크기|겹침 비율|top-k|threshold|설명
readonly EXPERIMENTS=(
    "A|400|0|4|0.0|기준선"
    "B|200|0|4|0.0|작은 청크"
    "C|800|0|4|0.0|큰 청크"
    "D|400|0|8|0.0|넓은 검색"
    "E|400|0|4|0.7|엄격한 임계값"
    "F|400|20|4|0.0|20% 겹침"
)

# 최종 콘솔·HTML 표를 만들기 위해 각 실험 결과를 메모리에 모은다.
RESULT_ROWS=()
BEST_NAME=""
BEST_SCORE=-1

# summary.md를 매 실행마다 새로 만든다.
{
    echo "# Day 2 Step 5 — A~F 실험 결과"
    echo
    echo "| 조합 | 설명 | 청크 | 겹침 | top-k | threshold | 통과율 | 판정 | 로그 |"
    echo "| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |"
} > "${SUMMARY_FILE}"

run_experiment() {
    local name="$1"
    local chunk_size="$2"
    local overlap_percent="$3"
    local top_k="$4"
    local threshold="$5"
    local description="$6"
    local log_file="${RESULT_DIR}/${name}.log"

    echo "[${name}/F] ${description} 실행 중 — chunk=${chunk_size}, overlap=${overlap_percent}%, topK=${top_k}, threshold=${threshold}"

    # 상세 Gradle 출력은 화면에 쏟지 않고 조합별 로그 파일에만 저장한다.
    # 실험에서는 8/10 미만도 중요한 결과이므로 Gradle 실패와 관계없이 다음 조합을 계속 실행한다.
    (
        cd "${PROJECT_DIR}" || exit 1
        LAB2_CHUNK_SIZE="${chunk_size}" \
        LAB2_CHUNK_OVERLAP_PERCENT="${overlap_percent}" \
        LAB2_TOP_K="${top_k}" \
        LAB2_THRESHOLD="${threshold}" \
        ./gradlew test \
            -Peval \
            --tests "${TEST_CLASS}" \
            --rerun-tasks \
            --console=plain \
            --info
    ) > "${log_file}" 2>&1
    local gradle_status=$?

    # 테스트 로그의 마지막 "통과 N/10"을 추출한다. 실행 전 오류라면 N/A로 기록한다.
    local pass_rate
    pass_rate="$(sed -n 's/.*통과 \([0-9][0-9]*\/[0-9][0-9]*\).*/\1/p' "${log_file}" | tail -1)"
    if [[ -z "${pass_rate}" ]]; then
        pass_rate="N/A"
    fi

    local verdict
    local score=-1
    if [[ "${pass_rate}" == "N/A" ]]; then
        verdict="실행 오류"
    else
        score="${pass_rate%%/*}"
        if [[ ${score} -ge 8 ]]; then
            verdict="통과"
        else
            verdict="기준 미달"
        fi
    fi

    echo "       → ${pass_rate} · ${verdict} · 상세 로그: ${name}.log"

    # 실행 자체가 실패해 통과율을 얻지 못한 경우 마지막 로그를 바로 보여 준다.
    if [[ "${pass_rate}" == "N/A" ]]; then
        echo "       마지막 오류 로그:"
        tail -6 "${log_file}" | sed 's/^/         /'
    fi

    RESULT_ROWS+=("${name}|${description}|${chunk_size}|${overlap_percent}|${top_k}|${threshold}|${pass_rate}|${verdict}|${gradle_status}")

    if [[ ${score} -gt ${BEST_SCORE} ]]; then
        BEST_SCORE=${score}
        BEST_NAME=${name}
    fi

    # 로그 링크는 summary.md가 위치한 폴더를 기준으로 상대경로를 사용한다.
    echo "| ${name} | ${description} | ${chunk_size} | ${overlap_percent}% | ${top_k} | ${threshold} | ${pass_rate} | ${verdict} | [${name}.log](./${name}.log) |" \
        >> "${SUMMARY_FILE}"
}

print_terminal_summary() {
    echo
    echo "================================================================================"
    echo "                          Step 5 A~F 최종 비교"
    echo "================================================================================"
    printf "%-4s | %-16s | %6s | %7s | %5s | %9s | %7s | %s\n" \
        "조합" "설명" "청크" "겹침" "top-k" "threshold" "통과율" "판정"
    echo "--------------------------------------------------------------------------------"

    local row
    for row in "${RESULT_ROWS[@]}"; do
        local name description chunk_size overlap_percent top_k threshold pass_rate verdict gradle_status
        IFS='|' read -r name description chunk_size overlap_percent top_k threshold pass_rate verdict gradle_status <<< "${row}"
        printf "%-4s | %-16s | %6s | %6s%% | %5s | %9s | %7s | %s\n" \
            "${name}" "${description}" "${chunk_size}" "${overlap_percent}" \
            "${top_k}" "${threshold}" "${pass_rate}" "${verdict}"
    done

    echo "================================================================================"
    if [[ ${BEST_SCORE} -ge 0 ]]; then
        echo "최고 통과율: ${BEST_NAME} 조합 (${BEST_SCORE}/10)"
    else
        echo "통과율을 계산하지 못했습니다. 각 로그의 API 키·429·연결 오류를 확인하세요."
    fi
}

write_html_summary() {
    {
        echo '<!doctype html>'
        echo '<html lang="ko"><head><meta charset="utf-8">'
        echo '<meta name="viewport" content="width=device-width,initial-scale=1">'
        echo '<title>Day 2 Step 5 — A~F 실험 결과</title>'
        echo '<style>'
        echo 'body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;margin:40px;background:#f6f8fa;color:#1f2328}'
        echo '.card{max-width:1100px;margin:auto;background:white;border:1px solid #d0d7de;border-radius:12px;padding:28px;box-shadow:0 4px 16px #1f232812}'
        echo 'table{width:100%;border-collapse:collapse;margin-top:20px}th,td{padding:12px;border-bottom:1px solid #d8dee4;text-align:center}th{background:#f0f3f6}'
        echo 'tr.pass{background:#dafbe1}tr.below{background:#fff8c5}tr.error{background:#ffebe9}'
        echo '.legend{display:flex;gap:16px;margin-top:16px;font-size:14px}.best{font-size:18px;font-weight:700;color:#0969da}'
        echo 'a{color:#0969da}</style></head><body><main class="card">'
        echo '<h1>Day 2 Step 5 — A~F 실험 결과</h1>'
        echo '<p>같은 golden.json 10문항으로 청크·top-k·threshold·overlap 설정을 비교한 결과입니다.</p>'
        echo '<table><thead><tr><th>조합</th><th>설명</th><th>청크</th><th>겹침</th><th>top-k</th><th>threshold</th><th>통과율</th><th>판정</th><th>로그</th></tr></thead><tbody>'

        local row
        for row in "${RESULT_ROWS[@]}"; do
            local name description chunk_size overlap_percent top_k threshold pass_rate verdict gradle_status css_class
            IFS='|' read -r name description chunk_size overlap_percent top_k threshold pass_rate verdict gradle_status <<< "${row}"
            case "${verdict}" in
                "통과") css_class="pass" ;;
                "기준 미달") css_class="below" ;;
                *) css_class="error" ;;
            esac
            echo "<tr class=\"${css_class}\"><td><strong>${name}</strong></td><td>${description}</td><td>${chunk_size}</td><td>${overlap_percent}%</td><td>${top_k}</td><td>${threshold}</td><td><strong>${pass_rate}</strong></td><td>${verdict}</td><td><a href=\"./${name}.log\">상세</a></td></tr>"
        done

        echo '</tbody></table>'
        if [[ ${BEST_SCORE} -ge 0 ]]; then
            echo "<p class=\"best\">최고 통과율: ${BEST_NAME} 조합 (${BEST_SCORE}/10)</p>"
        fi
        echo '<div class="legend"><span>초록: 8/10 이상</span><span>노랑: 기준 미달</span><span>빨강: 실행 오류</span></div>'
        echo '</main></body></html>'
    } > "${HTML_FILE}"
}

for experiment in "${EXPERIMENTS[@]}"; do
    IFS='|' read -r name chunk_size overlap_percent top_k threshold description <<< "${experiment}"
    run_experiment \
        "${name}" \
        "${chunk_size}" \
        "${overlap_percent}" \
        "${top_k}" \
        "${threshold}" \
        "${description}"

    # 연속 API 호출로 429가 발생할 가능성을 줄인다. 0으로 설정하면 기다리지 않는다.
    if [[ "${name}" != "F" && "${DELAY_SECONDS}" != "0" ]]; then
        sleep "${DELAY_SECONDS}"
    fi
done

if [[ ${BEST_SCORE} -ge 0 ]]; then
    {
        echo
        echo "최고 통과율: **${BEST_NAME} 조합 (${BEST_SCORE}/10)**"
    } >> "${SUMMARY_FILE}"
fi

print_terminal_summary
write_html_summary

echo
echo "A~F 실험이 끝났습니다."
echo "터미널 표 외에 브라우저 결과를 보려면 다음 명령을 실행하세요."
echo "open \"${HTML_FILE}\""
echo
echo "Markdown 요약: ${SUMMARY_FILE}"
echo "HTML 요약: ${HTML_FILE}"
echo "개별 로그: ${RESULT_DIR}/A.log ~ F.log"
