#!/bin/zsh
set -e

if [[ -z "${OPENAI_API_KEY:-}" ]]; then
  read -s "OPENAI_API_KEY?OpenAI API Key 입력: "
  export OPENAI_API_KEY
  echo
fi

echo "OPENAI_API_KEY loaded (length=${#OPENAI_API_KEY})"

exec env OPENAI_API_KEY="$OPENAI_API_KEY" \
  ./gradlew bootRun --no-daemon
