# 2GB 서버 자원 예산

팬파라이브는 Java 서비스 5개(gateway, auth, broadcast, interaction, fandom)와 PostgreSQL, Redis, MediaMTX, Caddy 를 한 서버에 띄웁니다.
기본 설정(`Dockerfile` 의 `-Xmx384m`)으로는 Java 서비스만 합쳐 2GB 를 넘길 수 있어서, 2GB 서버용 상한을 따로 정했습니다.

```bash
docker compose -f docker-compose.yml -f docker-compose.small.yml up -d --build
./scripts/measure-memory.sh 12 10 > memory.csv     # 10초 간격 12번 측정
```

## 예산표

| 컨테이너 | 메모리 상한 | 힙(-Xmx) | 메타스페이스 | 근거 |
| --- | --- | --- | --- | --- |
| postgres | 256MB | - | - | `shared_buffers=64MB`, `max_connections=60`(서비스 4개 x 풀 10 + 여유) |
| redis | 128MB | - | - | `maxmemory 96mb`, `noeviction`(하트 한도 키가 몰래 지워지면 안 되므로 꽉 차면 쓰기 오류로 알게 한다) |
| auth-service | 220MB | 96MB | 96MB | 로그인, 토큰 발급뿐이라 가장 작다 |
| broadcast-service | 260MB | 128MB | 112MB | JPA(Hibernate) 클래스가 많아 메타스페이스를 조금 더 준다 |
| interaction-service | 260MB | 128MB | 112MB | JPA, SSE 연결(채팅, 후원 알림) |
| fandom-service | 260MB | 128MB | 96MB | JDBC 만 써서 메타스페이스가 작고, 하트 병합 큐와 SSE 연결에 힙을 쓴다 |
| gateway | 200MB | 96MB | 80MB | 요청 전달만 함 |
| mediamtx | 160MB | - | - | WebRTC 중계, 녹화 파일 쓰기 |
| web (Caddy) | 64MB | - | - | 정적 파일, 프록시 |
| **합계** | **1,808MB** | | | 운영체제와 Docker 에 약 200MB 를 남긴다 |

상한은 "이걸 넘으면 그 컨테이너만 재시작된다"는 선입니다. 한 서비스의 메모리 누수가 서버 전체를 멈추게 하지 않도록 서비스마다 따로 둡니다.

## JVM 옵션을 고른 이유

| 옵션 | 이유 | 대가 |
| --- | --- | --- |
| `-XX:+UseSerialGC` | GC 스레드가 하나라 작은 힙에서 G1 보다 메모리를 덜 씀 | 힙이 커지면 멈춤이 길어짐(이 크기에서는 수십 ms) |
| `-Xss512k` | 스레드 스택 절반 | 깊은 재귀가 없어 문제없음 |
| `-XX:TieredStopAtLevel=1` | C2 컴파일러를 끄면 JIT 메모리와 CPU 를 아낌 | 최고 처리량은 조금 낮아짐 |
| `-XX:ReservedCodeCacheSize=48m` | JIT 코드 영역 상한(기본 240MB 예약) | C1 만 쓰므로 충분 |
| `-XX:+AutoCreateSharedArchive` | 첫 기동 때 클래스 공유 파일(CDS)을 만들고 다음부터 재사용해서 기동 시간과 메타스페이스 사용을 줄임 | 컨테이너를 새로 만들면 첫 기동 때 다시 만듦 |
| `spring.threads.virtual.enabled=true` | 요청을 가상 스레드로 처리해서 SSE 연결이 많아도 플랫폼 스레드가 늘지 않음(Java 21 이상) | `synchronized` 안에서 오래 막히는 코드가 있으면 운반 스레드를 붙잡음(이 프로젝트는 해당 없음) |

## 측정 결과

측정은 `measure-memory.sh` 출력(CSV)을 그대로 붙입니다. 아래 칸은 측정 후 채웁니다.

| 상태 | auth | broadcast | interaction | fandom | gateway | postgres | redis | mediamtx | 합계 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 기동 직후(유휴) | | | | | | | | | |
| 방송 1개 + 시청자 20명 | | | | | | | | | |
| 하트 부하(`docs/k6/hearts.js`) | | | | | | | | | |

## 넘칠 때 먼저 볼 것

1. `docker stats` 에서 상한에 붙은 컨테이너를 찾는다.
2. Java 서비스라면 힙인지(GC 로그) 힙 밖인지(메타스페이스, 스레드) 나눈다. `docker compose exec <서비스> jcmd 1 VM.native_memory summary` 는 `-XX:NativeMemoryTracking=summary` 를 켰을 때 쓸 수 있다.
3. PostgreSQL 이면 연결 수(`SELECT count(*) FROM pg_stat_activity`)부터 본다. 서비스 풀 크기를 줄이는 것이 `shared_buffers` 를 줄이는 것보다 효과가 크다.
4. 그래도 모자라면 스왑 1~2GB 를 켠다(`docs/DEPLOY.md`). 상한을 무작정 올리면 다른 컨테이너가 밀려난다.
