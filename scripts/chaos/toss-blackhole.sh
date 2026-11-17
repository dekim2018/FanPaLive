#!/usr/bin/env bash
# 실험 3. 결제사가 응답하지 않으면 후원은 어떻게 되는가
# 가설: 승인 요청이 타임아웃되면 주문은 결과 모름(UNKNOWN)으로 남고 202 를 돌려준다. 돈이 빠졌는지 모르는 상태에서
#       실패로 처리하지 않는다. 결제사가 돌아오면 1분 주기 확인 작업이 확정(DONE) 또는 실패(FAILED)로 정리한다.
# 방법: TOSS_API_URL 을 응답하지 않는 주소(10.255.255.1, 라우팅되지 않는 사설 IP)로 바꿔 interaction-service 를 다시 띄운다.
cd "$(dirname "$0")" && source ./common.sh

say "결제사 주소를 응답 없는 곳으로 바꿔 재시작"
TOSS_API_URL=http://10.255.255.1 $COMPOSE up -d interaction-service >/dev/null
sleep 20

T2=$(token tester2)
ORDER=$(curl -s -X POST "$BASE/api/donations/orders" -H "Authorization: Bearer $T2" -H 'Content-Type: application/json' \
  -d '{"targetType":"FAN_TARGET","targetId":1,"amount":1000,"message":"장애 실험"}')
OID=$(echo "$ORDER" | sed -E 's/.*"orderId":"([^"]+)".*/\1/')
echo "  주문 $OID"
RES=$(curl -s -w '\n%{http_code}' -X POST "$BASE/api/donations/confirm" -H "Authorization: Bearer $T2" \
  -H 'Content-Type: application/json' -d "{\"orderId\":\"$OID\",\"paymentKey\":\"chaos-test-key\",\"amount\":1000}" --max-time 60)
CODE=$(echo "$RES" | tail -1)
if [ "$CODE" = 202 ] && echo "$RES" | grep -q UNKNOWN; then ok "202 + UNKNOWN"; else bad "응답 $CODE $(echo "$RES" | head -1)"; fi

say "결제사 주소 원래대로"
$COMPOSE up -d interaction-service >/dev/null
echo "  (테스트 결제 키로 실제 승인된 결제가 아니라서, 복구 뒤 확인 작업이 FAILED 로 정리하는 것까지 볼 수 있다)"
finish
