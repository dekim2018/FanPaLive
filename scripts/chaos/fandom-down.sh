#!/usr/bin/env bash
# 실험 1. fandom-service 가 죽어도 방송은 만들어지는가
# 가설: 응원팀 확인이 안 되면 broadcast-service 는 방송을 "일반 방송"으로 만들고(side=null), 시청과 댓글은 그대로 된다.
cd "$(dirname "$0")" && source ./common.sh

T1=$(token tester1)
say "fandom-service 중지"
$COMPOSE stop fandom-service >/dev/null

say "편파방 만들기 요청(응원팀 1번)"
RES=$(curl -s -X POST "$BASE/api/broadcasts" -H "Authorization: Bearer $T1" -H 'Content-Type: application/json' \
  -d '{"title":"장애 실험 편파방","description":"","targetId":1}')
ID=$(echo "$RES" | sed -E 's/.*"id":([0-9]+).*/\1/')
if echo "$RES" | grep -q '"side":null'; then ok "일반 방송으로 생성됨(id=$ID)"; else bad "응답: $RES"; fi

say "시청 정보와 댓글"
[ "$(code "$BASE/api/broadcasts/$ID")" = 200 ] && ok "방송 조회 200" || bad "방송 조회 실패"
C=$(code -X POST "$BASE/api/broadcasts/$ID/comments" -H "Authorization: Bearer $T1" -H 'Content-Type: application/json' -d '{"content":"댓글은 된다"}')
[ "$C" = 201 ] && ok "댓글 작성 201" || bad "댓글 작성 $C"
H=$(code "$BASE/api/rankings?league=KBO")
[ "$H" -ge 500 ] && ok "팬덤 기능만 실패($H)" || bad "랭킹 응답 $H"

say "fandom-service 다시 시작"
$COMPOSE start fandom-service >/dev/null
wait_ok "$BASE/api/sports" && ok "복구됨" || bad "90초 안에 복구되지 않음"
curl -s -o /dev/null -X DELETE "$BASE/api/broadcasts/$ID" -H "Authorization: Bearer $T1"
finish
