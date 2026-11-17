#!/usr/bin/env bash
# 컨테이너별 메모리 사용량을 일정 간격으로 재서 CSV 로 남긴다.
#   ./scripts/measure-memory.sh [횟수=12] [간격초=10] > memory.csv
# 부하와 같이 볼 때: 다른 터미널에서 k6 run docs/k6/<시나리오>.js 를 돌리는 동안 실행한다.
set -euo pipefail
COUNT=${1:-12}
INTERVAL=${2:-10}
PROJECT=${COMPOSE_PROJECT_NAME:-live-broadcast}

echo "time,container,mem_used_mib,mem_limit_mib,mem_percent,cpu_percent"
for _ in $(seq 1 "$COUNT"); do
  NOW=$(date +%H:%M:%S)
  docker stats --no-stream --format '{{.Name}},{{.MemUsage}},{{.MemPerc}},{{.CPUPerc}}' \
    | grep "^${PROJECT}-" \
    | while IFS=, read -r name usage memp cpup; do
        used=$(echo "$usage" | awk -F' / ' '{print $1}')
        limit=$(echo "$usage" | awk -F' / ' '{print $2}')
        to_mib() {
          case "$1" in
            *GiB) awk -v v="${1%GiB}" 'BEGIN{printf "%.0f", v*1024}' ;;
            *MiB) awk -v v="${1%MiB}" 'BEGIN{printf "%.0f", v}' ;;
            *KiB) awk -v v="${1%KiB}" 'BEGIN{printf "%.1f", v/1024}' ;;
            *) echo "$1" ;;
          esac
        }
        echo "$NOW,${name#${PROJECT}-},$(to_mib "$used"),$(to_mib "$limit"),${memp%\%},${cpup%\%}"
      done
  sleep "$INTERVAL"
done
