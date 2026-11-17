#!/bin/sh
# 이미 만들어진 Postgres 볼륨에는 init.sql 이 다시 실행되지 않는다.
# 새 서비스가 추가돼도 기존 데이터를 지우지 않고 필요한 DB 만 만든다(이미 있으면 그대로 둔다).
set -e
for db in auth_db broadcast_db interaction_db fandom_db; do
  exists=$(psql -h postgres -U livebroadcast -d livebroadcast -tAc "SELECT 1 FROM pg_database WHERE datname='$db'")
  if [ "$exists" != "1" ]; then
    psql -h postgres -U livebroadcast -d livebroadcast -c "CREATE DATABASE $db"
    echo "created $db"
  fi
done
