#!/usr/bin/env bash
# 장애 실험 4종을 차례로 돌린다. 각 실험은 끝나면 원래 상태로 되돌린다.
cd "$(dirname "$0")"
status=0
for t in fandom-down redis-down toss-blackhole stream-backlog; do
  ./$t.sh || status=1
done
exit $status
