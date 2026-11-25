# ErrDeck 으로 모니터링(로컬)

같은 사람이 만든 모니터링 서비스 [ErrDeck](https://github.com/dekim2018/ErrDeck) 에 에러, 성능 추적, 지표를 보낸다. 기본으로는 꺼져 있고, SDK 없이 빌드하면 지금과 똑같다. ErrDeck 저장소가 같은 상위 폴더(`../ErrDeck`)에 있다고 본다.

## 무엇이 보이나

- 에러: 각 서비스의 처리하지 못한 예외(500)와 ERROR 로그, 화면의 처리하지 못한 예외. 같은 원인은 이슈 하나로 묶이고 횟수와 처음/마지막 발생이 붙는다
- 추적: 화면의 페이지 이동 → axios 의 `/api` 호출 → gateway(라우트) → 서비스 → 내부 API(예: broadcast → fandom) → SQL 이 한 그림으로 이어진다
- 지표: ErrDeck 통합 화면의 서비스별 요약 카드(생존, CPU, 힙, p95, 5xx, 커넥션 풀 대기)와 그라파나
- 업타임과 알림: gateway 의 상태 확인 모니터, Discord 알림(ErrDeck 콘솔에서 규칙을 만든다)

## 띄우기

```bash
# 1. ErrDeck: SDK 를 로컬 메이븐에 넣고, 띄우고, 팬파라이브 프로젝트와 DSN 을 만든다
(cd ../ErrDeck && ./gradlew :core:publishToMavenLocal :sdk-spring:publishToMavenLocal \
  && docker compose --profile core --profile obs up -d --build \
  && docker compose exec -T postgres psql -U errdeck < scripts/seed/systems.sql \
  && docker compose exec -T postgres psql -U errdeck < scripts/seed/fanpalive-local.sql)   # 마지막 출력이 서비스별 DSN

# 2. 팬파라이브 서버: SDK 를 넣어 빌드하고, 서비스마다 자기 DSN 을 준다. 지표를 열려면 MANAGEMENT_EXPOSURE
./gradlew -Perrdeck=true bootJar
MANAGEMENT_EXPOSURE=health,prometheus ERRDECK_DSN=http://{gateway 키}@localhost:18080/{gateway 프로젝트} java -jar gateway/build/libs/gateway-1.0.0.jar
# auth-service, broadcast-service, interaction-service, fandom-service 도 같은 방식(각자의 DSN)

# 3. 화면
cd frontend && VITE_ERRDECK_DSN=http://{fanpalive-web 키}@localhost:18080/{fanpalive-web 프로젝트} npm run dev
```

ErrDeck 콘솔(`../ErrDeck/web` 에서 `npm run dev`. 5173 이 겹치면 화면을 다른 포트로)의 전체 보기에서 팬파라이브 시스템을 본다. ErrDeck 의 프로메테우스는 같은 맥의 8080~8084 를 긁는다.

| 환경 변수 | 뜻 | 기본 |
| --- | --- | --- |
| `ERRDECK_DSN` | 서비스의 DSN. 비어 있으면 SDK 가 꺼진다 | 비어 있음 |
| `ERRDECK_TRACE_SAMPLE_RATE` | 요청이 시작될 때 추적할 비율. 뽑히지 않아도 에러와 1초 넘는 요청은 남는다 | 0.1 |
| `APP_RELEASE`, `APP_ENVIRONMENT` | 릴리스(재발 판정)와 환경 | local |
| `MANAGEMENT_EXPOSURE` | 액추에이터 노출. 지표를 열려면 `health,prometheus` | health |
| `VITE_ERRDECK_DSN` | 화면의 DSN. 없으면 SDK 를 내려받지도 않는다 | 비어 있음 |

## 바꾼 것

- 루트 `build.gradle`: 서비스에 `micrometer-registry-prometheus`(노출은 `MANAGEMENT_EXPOSURE` 로 켤 때만), `-Perrdeck=true` 일 때만 `mavenLocal()` 의 ErrDeck SDK
- 각 서비스 `application.yml`: `errdeck` 설정, 요청 시간 히스토그램, 노출 목록을 환경 변수로. 보안 설정에 `/actuator/prometheus` 허용(노출하지 않으면 404)
- `common/http/InternalHttpConfig`: 서비스 사이 내부 API 클라이언트 4개가 `RestClient.builder()` 대신 공통 빌더 빈을 받는다. SDK 가 그 빌더에 붙어 `traceparent` 로 다음 서비스와 추적을 잇는다. 토스와 MediaMTX 같은 외부 호출은 그대로(추적 헤더를 바깥으로 보내지 않는다)
- 화면: `src/lib/errdeck`(빌드한 SDK 와 시작 코드). 토큰과 Bearer 는 보내기 전에 가린다. SDK 를 고치면 `npm run errdeck:sync`

## 로컬에서 확인한 것(2026-11-25)

확인용 PostgreSQL 과 Redis, 서비스 5개를 18180~18184 로 따로 띄워 확인했다.

- 화면 `/live` 의 이동이 `GET /api/broadcasts`, `GET /api/sports` 같은 axios 호출을 거쳐 gateway(`GET route:broadcast-service`)와 각 서비스의 요청까지 같은 trace_id 로 이어졌다
- 방송 만들기가 gateway → broadcast-service(`POST /api/broadcasts`) → fandom-service(`GET /internal/targets/{targetId}`)로 한 추적이 되었고, SQL 이 값을 지운 채 스팬으로 남았다
- 화면 예외 메시지의 `?token=` 이 가려졌다
