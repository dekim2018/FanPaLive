#!/usr/bin/env bash
# 실험 2. Redis 가 죽으면 하트는 어떻게 되는가
# 가설: 하루 한도를 확인할 수 없으므로 하트는 받지 않는다(한도 없이 받지 않음). 방송 목록과 상세는 DB 라서 그대로 된다.
cd "$(dirname "$0")" && source ./common.sh

T2=$(token tester2)
say "Redis 중지"
$COMPOSE stop redis >/dev/null
sleep 2

H=$(code -X POST "$BASE/api/hearts" -H "Authorization: Bearer $T2" -H 'X-Device-Id: chaos-device-0001' \
  -H 'Content-Type: application/json' -d '{"targetId":1,"amount":1,"source":"TARGET_PAGE"}')
# 한도 확인(Redis)이 안 되면 502, 로그아웃 목록(Redis)을 못 읽어 인증 단계에서 막히면 401/500 이다. 어느 쪽이든 받지 않으면 통과.
[ "$H" != 200 ] && ok "하트 거절($H)" || bad "Redis 없이 하트가 들어감(한도 없이 받으면 안 됨)"
[ "$(code "$BASE/api/broadcasts")" = 200 ] && ok "방송 목록 200" || bad "방송 목록 실패"

say "Redis 다시 시작"
$COMPOSE start redis >/dev/null
sleep 5
H=$(code -X POST "$BASE/api/hearts" -H "Authorization: Bearer $T2" -H 'X-Device-Id: chaos-device-0001' \
  -H 'Content-Type: application/json' -d '{"targetId":1,"amount":1,"source":"TARGET_PAGE"}')
[ "$H" = 200 ] || [ "$H" = 409 ] && ok "복구 뒤 하트 응답 $H" || bad "복구 뒤 하트 응답 $H"
finish
