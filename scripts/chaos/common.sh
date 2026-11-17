#!/usr/bin/env bash
# 장애 실험 공용 함수. docker compose 로 띄운 로컬 환경(.env 의 TEST_ACCOUNTS_ENABLED=true)에서 쓴다.
set -euo pipefail

BASE=${BASE:-http://localhost}
COMPOSE=${COMPOSE:-docker compose}

say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }
ok() { printf '  [통과] %s\n' "$*"; }
bad() { printf '  [실패] %s\n' "$*"; FAILED=1; }
FAILED=0

# 테스트 계정 토큰(원클릭 로그인). 비밀번호를 쓰지 않는다.
token() {
  curl -s -X POST "$BASE/api/auth/test-login" -H 'Content-Type: application/json' -d "{\"username\":\"$1\"}" \
    | sed -E 's/.*"accessToken":"([^"]+)".*/\1/'
}

# 응답 코드만
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

# 주소가 200 을 돌려줄 때까지 기다린다(최대 90초)
wait_ok() {
  for _ in $(seq 1 45); do
    [ "$(code "$1")" = 200 ] && return 0
    sleep 2
  done
  return 1
}

finish() {
  if [ "$FAILED" = 0 ]; then say "결과: 모두 통과"; else say "결과: 실패 있음"; exit 1; fi
}
