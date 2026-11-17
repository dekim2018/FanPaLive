#!/usr/bin/env bash
# 실험 4. fandom-service 가 멈춘 동안 쌓인 이벤트(후원 확정, 편파방 시작)는 사라지지 않는가
# 가설: Redis Stream 소비 그룹이라 소비자가 없는 동안 메시지가 쌓이고, 다시 켜지면 밀린 것부터 처리한다.
#       같은 이벤트를 두 번 처리해도 DB 유일 제약(event_id) 때문에 한 번만 반영된다.
cd "$(dirname "$0")" && source ./common.sh

say "fandom-service 일시 정지(pause, 메모리 상태 유지)"
$COMPOSE pause fandom-service >/dev/null
for i in 1 2 3 4 5; do
  $COMPOSE exec -T redis redis-cli XADD fanpa:stream:broadcasts '*' eventId "chaos-live-$i" broadcastId 999$i \
    targetId 1 targetName "두산 베어스" title "밀린 이벤트 $i" ownerId 1 ownerNickname tester1 \
    startedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" >/dev/null
done
LAG=$($COMPOSE exec -T redis redis-cli XINFO GROUPS fanpa:stream:broadcasts | awk 'f{print;exit} /^lag$/{f=1}')
echo "  쌓인 메시지(lag): ${LAG:-알 수 없음}"

say "다시 켜고 처리 확인"
$COMPOSE unpause fandom-service >/dev/null
sleep 5
PENDING=$($COMPOSE exec -T redis redis-cli XPENDING fanpa:stream:broadcasts fandom | head -1)
LAG2=$($COMPOSE exec -T redis redis-cli XINFO GROUPS fanpa:stream:broadcasts | awk 'f{print;exit} /^lag$/{f=1}')
[ "${PENDING:-0}" = 0 ] && ok "처리 중 메시지 없음" || bad "대기 목록 $PENDING"
[ "${LAG2:-0}" = 0 ] && ok "밀린 메시지 모두 소비" || bad "lag $LAG2"

say "같은 이벤트를 다시 넣어도 알림은 한 번"
$COMPOSE exec -T redis redis-cli XADD fanpa:stream:broadcasts '*' eventId chaos-live-1 broadcastId 9991 targetId 1 \
  targetName "두산 베어스" title "중복" ownerId 1 ownerNickname tester1 startedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" >/dev/null
sleep 3
DUP=$($COMPOSE exec -T postgres psql -U livebroadcast -d fandom_db -tAc \
  "SELECT COUNT(*) FROM notifications GROUP BY user_id, event_id HAVING COUNT(*) > 1" | wc -l)
[ "$DUP" = 0 ] && ok "중복 알림 0건" || bad "중복 알림 $DUP"
$COMPOSE exec -T postgres psql -U livebroadcast -d fandom_db -qc "DELETE FROM notifications WHERE event_id LIKE 'chaos-live-%'" >/dev/null
finish
