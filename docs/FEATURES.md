# 팬파라이브 기능 정리

팬파라이브(FanPa Live)는 팬이 직접 하는 스포츠 **편파중계** 플랫폼입니다.
과제 요구사항(실시간 방송, 인증, 댓글, 후원, 녹화)을 그대로 지키면서, 그 위에 종목과 응원 대상(구단, 선수), 팬 등록, 하트 응원, 응원 랭킹, 후원 3종, 경기 매치룸을 더했습니다.

- 화면 구성(왼쪽 메뉴, 위쪽 검색, LIVE 배지와 시청자 수가 붙은 카드 격자, 리그 탭)은 국내 스트리밍 서비스의 스포츠 채널 구성을 참고했고, 로고와 색은 팬파라이브 고유 값을 씁니다.
- 색은 **밝은 화면 + 응원팀 색**입니다. 기본 강조색은 파랑(`#0b57d0`)이고, 구단 채널과 편파방 시청 화면은 그 팀 색으로 바뀝니다. KBO 구단 색은 대부분 진한 색이라 밝은 배경에서 잘 보이고, 한화 주황처럼 밝은 팀 색은 명도 대비를 계산해서 글자색(흰색/검정)을 고르거나 같은 계열로 어둡게 바꿉니다(`frontend/src/lib/team.js`).
- 실제 구단 로고는 쓰지 않고 팀 대표색과 약칭으로 만든 원형 표시를 씁니다.
- 경기 중계 화면(TV 중계 등)의 재송출은 금지하고, 방송 만들 때 안내에 동의해야 합니다.

## 목차

1. [과제 요구사항 대응표](#1-과제-요구사항-대응표)
2. [화면별 기능](#2-화면별-기능)
3. [팬덤 기능 규칙](#3-팬덤-기능-규칙)
4. [백엔드에서 신경 쓴 문제 9가지](#4-백엔드에서-신경-쓴-문제-9가지)
5. [서비스와 API](#5-서비스와-api)
6. [기능 스위치와 장애 시 동작](#6-기능-스위치와-장애-시-동작)
7. [확인 방법 (두산 베어스 예시)](#7-확인-방법-두산-베어스-예시)
8. [접근성과 화면 품질](#8-접근성과-화면-품질)
9. [후속 작업 (이번 제출에서 제외)](#9-후속-작업-이번-제출에서-제외)

---

## 1. 과제 요구사항 대응표

| 구분 | 요구사항 | 구현 | 팬파라이브에서 달라진 점 |
| --- | --- | --- | --- |
| 필수 | PC 라이브 방송 (화면, 탭/시스템 소리, 마이크) | `getDisplayMedia` + Web Audio 믹싱, WHIP 송출 | 변화 없음 |
| 필수 | 모바일 라이브 방송 (카메라, 마이크) | `getUserMedia`, 전면/후면 전환 | 변화 없음 |
| 필수 | 방송 등록, 수정, 삭제, 목록, 상세, 상태, 영구 저장 | broadcast-service, PostgreSQL | 응원팀, 경기 지정(선택), 리그/팀/경기 필터 |
| 필수 | 회원가입, 로그인, 로그아웃, 서버측 소유자 검증 | JWT + Refresh 쿠키 + 블랙리스트 | 사용자 역할(USER/ADMIN), 운영자 API 서버 검증 |
| 선택 | 댓글 CRUD, 작성자 검증 | interaction-service, SSE 실시간 | 작성자 주 응원팀 배지 |
| 선택 | 후원 (테스트 결제, 서버 검증, 중복 적립 방지, 방송별 내역과 합계) | 토스페이먼츠 SDK v2 + 승인 API | 후원 3종, 결제 결과 모름(UNKNOWN) 처리, 결제사 대사 |
| 선택 | 녹화와 다시보기, 저장 위치 영구 저장 | MediaMTX fMP4 + Playback | 리그/응원팀 필터, 응원팀 정보 보존 |

기존 기능은 하나도 빼지 않았습니다. 응원팀 없이 만드는 **일반 방송**도 그대로 됩니다.

## 2. 화면별 기능

| 화면 | 경로 | 기능 |
| --- | --- | --- |
| 홈 | `/` | 대표 LIVE 배너(시청자 수), 내 응원팀 LIVE, 오늘의 응원 대결(경기 카드와 게이지), 리그 탭별 스포츠 LIVE, 이번 주 KBO 응원 랭킹 TOP 5, 종목 통합 팬 TOP |
| 전체 방송 | `/live` | 방송 중/전체/종료 탭, 리그 필터, 15초마다 자동 갱신 |
| 스포츠 | `/sports` | 종목별 리그 목록, 팬 신청 가능 여부 |
| 리그 | `/leagues/:code` | 리그 편파방 LIVE, 구단/선수 목록(팬 많은 순, 이름순), 바로 하트 보내기 |
| 구단/선수 채널 | `/targets/:id` | 팀 색 배너, 주 응원팀 등록/팔로우/해제, 하트, 이번 주 순위, 팬 수, 누적 하트, 응원기금, 이번 달 편파방 순시청자, 편파방 LIVE, 서포터 순위(주간/월간/시즌), 주간 순위 변화, 다시보기, 응원기금 보내기 |
| 응원 랭킹 | `/rankings` | 리그 선택, 기간(주간/월간/시즌), 정렬(응원 점수, 팬 수, 하트, 후원), 지난주 대비 순위 변화 |
| 경기 일정 | `/matches` | 날짜별(어제부터 일주일), 리그별 경기 |
| 매치룸 | `/matches/:id` | 점수판, 실시간 응원 게이지(SSE), 양 팀 하트 버튼, 양 팀 편파방 목록 |
| 팀/선수 신청 | `/requests` | 해외 구단/선수 신청(이름, 영문, 약칭, 팀 색 미리보기, 소개, 참고 링크), 심사 중인 신청에 "나도 원해요", 내 신청 상태와 반려 사유 |
| 응원팀 고르기 | `/onboarding` | 가입 직후 이동. 리그별로 처음 누른 팀이 주 응원팀, 다음부터 팔로우 |
| 마이페이지 | `/me` | 내 응원팀, 오늘 남은 하트, 내가 한 후원(응원기금 구분), 받은 후원 |
| 운영 관리 | `/admin` | 신청 승인/반려, 경기 등록, 경기 시작/점수/종료/취소, 운영 지표(하트 쓰기 병합 효과, 유실 하트, 매치룸 구독 수) |
| 방송 만들기/수정 | `/broadcasts/new`, `/:id/edit` | 편파중계/일반 방송 선택, 리그와 응원팀(주 응원팀 기본 선택), 경기 지정, 중계권 안내 동의 |
| 시청 | `/broadcasts/:id` | 팀 색 테마, 응원팀 정보와 매치룸 바로가기, 하트, 현재 시청자와 누적 순시청자, 후원 대상 선택(방송자 / 응원기금), 채팅 응원팀 배지 |
| 스튜디오 | `/studio/:id` | 기존 송출 기능 그대로 |
| 다시보기 | `/recordings`, `/:id` | 리그 필터, 편파중계 표시 |
| 후원 결과 | `/donations/success`, `/fail` | 방송자 후원/응원기금 구분, 결제 확인 중(UNKNOWN) 안내 |

공통 레이아웃

- 위쪽 헤더: 메뉴 버튼, 로고, 구단/선수 검색(입력하면 바로 추천), 방송하기, 사용자 메뉴(마이페이지, 내 방송, 응원팀 설정, 운영 관리, 로그아웃)
- 왼쪽 메뉴: 홈, 전체 LIVE, 스포츠, 응원 랭킹, 경기 일정, 다시보기, 팀/선수 신청, 내 응원팀(★ 주 응원팀), 리그 바로가기
- 1100px 이하에서는 왼쪽 메뉴가 서랍으로 바뀌고, 모바일에서는 검색창이 헤더 가운데를 채웁니다.

## 3. 팬덤 기능 규칙

### 종목과 응원 대상

| 항목 | 내용 |
| --- | --- |
| 기본 데이터 | 야구(KBO, MLB), 축구(프리미어리그, 라리가, 분데스리가, 세리에A, K리그1), 농구(NBA), 테니스(ATP, WTA) |
| KBO | 10개 구단을 처음부터 등록 (두산 베어스, LG 트윈스, KIA 타이거즈, 삼성 라이온즈, SSG 랜더스, 롯데 자이언츠, 한화 이글스, NC 다이노스, KT 위즈, 키움 히어로즈) |
| 해외 구단/선수 | 사용자가 신청하고 운영자가 승인하면 공개. KBO 는 신청을 받지 않음 |
| 중복 신청 | 이름을 NFKC 정규화하고 공백과 기호를 지운 값으로 비교. 같은 리그에 같은 이름의 대기 신청이 있으면 새로 만들지 않고 "나도 원해요" 투표로 합침 |
| 숨김 | 운영자가 대상을 숨기면 목록과 랭킹에서 빠지고 기존 기록은 남음 |

### 팬 등록

| 항목 | 규칙 |
| --- | --- |
| 주 응원팀 | 리그마다 하나. 응원 점수 100점, 채팅 배지에 표시 |
| 팔로우 | 개수 제한 없음. 응원 점수 20점 |
| 주 응원팀 변경 | 30일에 한 번 (`PRIMARY_FAN_COOLDOWN`) |
| 동시 요청 | 사용자별 DB 잠금(`pg_advisory_xact_lock`)으로 같은 사람의 요청을 줄 세워 주 응원팀이 두 개가 되지 않게 함. DB 에도 부분 유니크 인덱스(리그당 주 응원팀 하나)가 있음 |

### 하트

| 항목 | 규칙 |
| --- | --- |
| 한도 | 하루 5개, 한 대상에 하루 3개 (KST 자정 초기화) |
| 점수 | 하트 1개당 10점. 가입 7일 이내 계정은 5점 |
| 어뷰징 | 같은 기기에서 오늘 3개 이상 계정, 같은 IP 에서 6개 이상 계정이 하트를 보내면 그 하트는 점수 0점(하트 수와 게이지에도 빠짐). 사용자에게는 똑같이 성공으로 보여 우회 방법을 알려주지 않음 |
| 개인정보 | 기기 ID 와 IP 는 SHA-256 으로 바꿔서만 저장 |

### 응원 랭킹

- 응원 점수 = 주 응원 팬 x 100 + 팔로우 x 20 + 하트 점수 + 후원금 / 100
- 기간: 주간(월요일 시작, KST), 월간, 시즌(3월 1일 시작)
- 점수가 같으면 그 점수에 먼저 도달한 대상이 위
- 매주 월요일 00:05 에 지난주 순위를 저장해서 순위 변화(▲▼)와 구단 채널의 순위 그래프에 씀

### 후원 3종

| 종류 | 받는 쪽 | 응원 점수 반영 | 화면 |
| --- | --- | --- | --- |
| 방송자 후원 | 방송자 | 없음 | 일반 방송 시청 화면 |
| 편파방 후원 | 방송자 | 방송의 응원팀에 반영 | 편파방 시청 화면에서 "방송자 후원" |
| 응원기금 | 구단/선수 (가상 기금) | 반영 | 구단 채널, 편파방 시청 화면에서 "응원기금" |

응원기금은 토스페이먼츠 테스트 결제로 만든 팬 활동 기록이며 실제 구단에 전달되지 않습니다(화면에 안내). 후원 대상과 금액은 서버가 방송과 응원 대상을 다시 조회해서 정합니다.

## 4. 백엔드에서 신경 쓴 문제 9가지

이 서비스에서 실제로 생기는 문제를 하나씩 정하고, 고른 방법과 버린 방법, 확인 방법을 적었습니다.

| 번호 | 문제 | 해결 | 버린 방법과 이유 | 위치 |
| --- | --- | --- | --- | --- |
| 1 | **하트 핫 키**: 두산 베어스 홈런 순간 한 팀 카운터 하나에 하트가 몰림. 요청마다 UPDATE 하면 같은 행 잠금 경합, 요청마다 ZINCRBY 하면 Redis 명령 수 = 요청 수 | 한도 검사(Lua)만 요청 안에서 하고, 하트는 메모리 큐에 넣은 뒤 200ms 마다 모아서 로그 배치 INSERT 1번 + 대상/날짜별 UPSERT + 대상별 ZINCRBY 1번 | 요청마다 DB 반영(잠금 경합), Kafka 도입(서버 1대 규모에 과함) | `HeartAccumulator`, 운영 화면의 "반영 1번당 하트 수" |
| 2 | **데이터마다 다른 내구성**: 돈과 하트를 같은 방식으로 저장할 필요가 없음 | 후원은 결제 DB 가 원본(강한 보장), 하트는 최대 200ms 유실 허용, 시청자 수는 근사치 | 모든 데이터를 같은 트랜잭션 수준으로 처리(하트에 과한 비용) | [docs/adr/0001-durability-grades.md](adr/0001-durability-grades.md) |
| 3 | **게이지 팬아웃**: 매치룸 구독자 수천 명에게 하트마다 이벤트를 보내면 하트 수 x 구독자 수 | 경기별 게이지를 Redis 에 더하고, 0.5초마다 바뀐 경기만 골라 한 번씩 보냄(conflation) | 하트마다 SSE 전송 | `RedisMatchGauge`, `MatchEventHub`, `FandomSchedulers` |
| 4 | **순시청자 수**: 시청자 ID 를 Set 에 다 넣으면 인기 방송 키가 수 MB | HyperLogLog(키당 최대 12KB, 표준 오차 약 0.81%), 방송별과 응원 대상 월별 두 가지 | Set, DB 집계 | `RedisViewerCounter`, `ViewerService` |
| 5 | **결제 결과를 모르는 경우**: 승인 요청 뒤 타임아웃이면 실패로 처리하면 돈은 빠지고 후원은 없는 상태가 됨 | `UNKNOWN` 상태로 남기고 1분마다 결제 조회 API 로 확정(DONE 이면 적립, 취소/만료면 실패). 매일 03:30 결제사 거래 내역과 비교해 놓친 승인은 적립하고, 우리만 확정한 건은 기록해 사람이 확인 | 타임아웃을 실패로 처리, 자동 재승인 | `DonationService.resolveUnknown`, `reconcileWithGateway`, `payment_reconcile_logs` |
| 6 | **후원이 랭킹에 두 번 들어가거나 빠지는 문제** | 커밋 뒤 Redis Stream 에 발행(eventId = 후원 ID), 팬덤 서비스는 소비 그룹으로 읽고 `donation_contributions` PK 로 한 번만 반영, 처리한 것만 ACK. 발행이 빠진 경우는 매일 04:10 결제 원본 합계와 비교해 보정 | 별도 발행 테이블과 재시도 장치(이 규모에서는 대사로 충분) | `DonationStreamRelay`, `DonationStreamConsumer`, `DonationEventService` |
| 7 | **랭킹 어뷰징** | 하트 한도 Lua(원자적 검사와 증가), 기기/IP 별 계정 수 신호로 점수 0점, 신규 계정 감점 | 캡차(사용성 저하), IP 차단(공용 와이파이 오탐) | `heart_limit.lua`, `abuse_record.lua`, `AbusePolicy` |
| 8 | **랭킹 동점과 기간 경계** | ZSET 점수 = 점수 x 1e8 + (1e8 - 1 - 기간 시작 후 경과 초) 로 인코딩해서 먼저 도달한 쪽이 위. 기간 키는 KST 기준, 시간은 `Clock` 주입으로 테스트 | 동점 시 ID 순(공정하지 않음) | `RankingScoreCodec`, `PeriodKeys`, `ranking_delta.lua` |
| 9 | **Redis 순위가 틀어졌을 때** | 매일 00:00:10 DB 일별 집계로 순위를 새 키에 만들고 RENAME 으로 한 번에 교체. 서버가 뜰 때도 비어 있으면 다시 만듦 | Redis 를 원본으로 사용 | `RankingService.rebuild`, `RankingWarmup` |

부하 테스트 스크립트: [docs/k6/hearts.js](k6/hearts.js)

## 5. 서비스와 API

### 서비스 구성

| 서비스 | 포트 | 추가/변경 |
| --- | --- | --- |
| gateway | 8080 | fandom-service 라우트, 응원기금과 시청자 수 라우트 |
| auth-service | 8081 | `role`(USER/ADMIN), JWT 에 `role`, `jat`(가입 시각) 클레임, 운영자 테스트 계정 `admin1` |
| broadcast-service | 8082 | 방송/녹화에 응원팀 정보(`side`), 목록 필터, fandom-service 확인(장애 시 일반 방송으로 생성) |
| interaction-service | 8083 | 후원 3종, UNKNOWN 처리, 결제 대사, 후원 이벤트 스트림, 순시청자, 내 후원/받은 후원 |
| fandom-service (신규) | 8084 | 종목, 리그, 응원 대상, 신청, 팬, 하트, 랭킹, 경기, 운영 |

### 추가된 API

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/sports` | - | 종목과 리그 |
| GET | `/api/leagues/{code}/targets?sort=fans\|name` | - | 리그의 구단/선수 |
| GET | `/api/targets?q=` | - | 구단/선수 검색 |
| GET | `/api/targets/{id}` | 선택 | 대상 상세, 이번 주 순위, 내 팬 상태 |
| GET | `/api/targets/{id}/supporters?period=` | - | 서포터 순위 |
| GET | `/api/targets/{id}/rank-history` | - | 주간 순위 기록 |
| PUT, DELETE | `/api/fans/{targetId}` | 필수 | 팬 등록(`{"primary":true}`), 해제 |
| GET | `/api/fans/me`, `/api/fans/badges?userIds=` | 필수, - | 내 응원팀, 채팅 배지 |
| POST | `/api/hearts` | 필수 | 하트 보내기 (`X-Device-Id` 헤더) |
| GET | `/api/hearts/me` | 필수 | 오늘 남은 하트 |
| GET | `/api/rankings?league=&period=&sort=` | - | 리그 랭킹 |
| GET | `/api/rankings/overall` | - | 종목 통합 팬 TOP |
| GET | `/api/matches?date=&league=`, `/upcoming`, `/{id}` | - | 경기 |
| GET | `/api/matches/{id}/events` | - | 응원 게이지 SSE |
| POST | `/api/target-requests` | 필수 | 신청(같은 신청이 있으면 투표로 합침, 200) |
| POST | `/api/target-requests/{id}/votes` | 필수 | 나도 원해요 |
| GET | `/api/target-requests`, `/mine` | -, 필수 | 심사 중 신청, 내 신청 |
| GET, POST | `/api/admin/target-requests`, `/{id}/approve`, `/{id}/reject` | ADMIN | 신청 심사 |
| PATCH | `/api/admin/targets/{id}` | ADMIN | 대상 숨김/공개 |
| POST, PATCH | `/api/admin/matches`, `/{id}` | ADMIN | 경기 등록, 상태와 점수 |
| GET | `/api/admin/ops` | ADMIN | 운영 지표 |
| GET | `/api/fandom/config` | - | 기능 스위치, 하트 한도 |
| POST | `/api/donations/orders` | 필수 | `targetType`(BROADCAST, FAN_TARGET), `broadcastId` 또는 `targetId` |
| POST | `/api/donations/confirm` | 필수 | 결과를 모르면 202 + `status: UNKNOWN` |
| GET | `/api/donations/mine`, `/received` | 필수 | 내가 한 후원, 받은 후원 |
| GET | `/api/targets/{id}/donations`, `/summary` | - | 응원 대상이 받은 후원 |
| GET | `/api/viewers?ids=1,2`, `/api/viewers/targets/{id}` | - | 현재 시청자, 누적 순시청자 |
| GET | `/api/broadcasts?league=&target=&match=` | - | 기존 목록에 필터 추가 |
| GET | `/api/recordings?league=&target=` | - | 기존 목록에 필터 추가 |

내부 API(외부 라우트 없음, `X-Internal-Key` 필요): `/internal/targets/{id}`, `/internal/matches/{id}`(fandom), `/internal/donations/target-totals?date=`(interaction)

### 데이터베이스

| DB | 마이그레이션 |
| --- | --- |
| auth_db | `V2__add_user_role.sql` |
| broadcast_db | `V2__add_fan_side.sql` (모두 NULL 허용 컬럼) |
| interaction_db | `V2__fan_target_donations.sql` (대상 종류 CHECK, UNKNOWN 상태, 부분 인덱스, `payment_reconcile_logs`) |
| fandom_db | `V1__create_fandom.sql`, `V2__seed_sports_and_kbo.sql` |

이미 돌고 있는 서버도 `docker compose up -d --build` 한 번이면 `postgres-init` 이 `fandom_db` 를 만들고 각 서비스가 V2 를 적용합니다. 기존 데이터는 그대로입니다.

## 6. 기능 스위치와 장애 시 동작

| 스위치 | 끄면 |
| --- | --- |
| `FEATURE_HEARTS` | 하트 API 가 막히고 버튼이 사라짐. 랭킹 조회는 그대로 |
| `FEATURE_MATCHES` | 경기 일정과 매치룸이 막힘 |
| `FEATURE_TARGET_REQUESTS` | 신청과 투표가 막힘 |

| 장애 | 동작 |
| --- | --- |
| fandom-service 중단 | 방송 만들기는 일반 방송으로 계속 됨(화면에 안내), 시청/댓글/방송자 후원 정상. 응원기금 주문만 잠시 실패 |
| Redis 중단 | 하트 한도를 확인할 수 없어 하트만 막힘(한도 없이 받지 않음). 랭킹은 DB 로 다시 만들 수 있음 |
| 결제사 응답 지연 | 후원이 UNKNOWN 으로 남고 1분 주기로 확정 |
| 후원 이벤트 유실 | 매일 04:10 결제 원본과 비교해 보정 |
| 서버 강제 종료 | 하트 최대 200ms 분량 유실 가능(정상 종료 시에는 큐를 비우고 종료) |

## 7. 확인 방법 (두산 베어스 예시)

준비: `.env` 에 `TEST_ACCOUNTS_ENABLED=true` 로 띄우면 로그인 화면에 `tester1`, `tester2`, `admin1`(운영자) 원클릭 로그인이 생깁니다.

1. **응원팀 정하기**: `tester2` 로그인 → 왼쪽 메뉴 "응원팀을 골라 보세요" → KBO 에서 두산 베어스 클릭(★ 주 응원), 한화 이글스 클릭(팔로우) → 저장. 왼쪽 메뉴 "내 응원팀"에 두 팀이 보임
2. **편파중계 만들기**: `tester1` 로그인 → 방송하기 → 편파중계, KBO 리그, 두산 베어스 선택 → 중계권 안내 체크 → 만들고 스튜디오로 이동 → 송출 시작
3. **시청 화면**: `tester2` 로 홈 → "내 응원팀 LIVE" 에 방송 카드(LIVE 배지, 시청자 수, 두산 표시) → 들어가면 응원팀 정보, 하트 버튼, 채팅 닉네임 옆 "두산" 배지
4. **하트**: 하트 보내기를 여러 번 누르면 3번째 이후 "한 대상에 하루 3개" 안내. 랭킹 화면에서 두산 베어스 점수가 오름(최대 0.2초 지연)
5. **후원 3종**: 시청 화면 후원 패널에서 "방송자 후원"과 "응원기금"을 각각 결제 → 결과 화면 문구 확인 → 두산 베어스 채널의 응원기금 합계와 랭킹 후원 열이 늘어남
6. **경기와 매치룸**: `admin1` 로그인 → 운영 관리 → 경기 관리에서 두산 베어스(홈) vs LG 트윈스(원정) 등록 → 경기 시작 → `/matches` 에서 매치룸 열기 → 다른 창에서 하트를 보내면 0.5초 안에 게이지가 움직임
7. **신청과 승인**: `tester2` → 팀/선수 신청 → MLB, "LA 다저스" 신청 → `tester1` 이 같은 이름을 " la  다저스 " 처럼 신청하면 새 신청 대신 "나도 원해요"로 합쳐짐 → `admin1` 이 승인 → MLB 리그에 공개
8. **권한**: `tester2` 토큰으로 `POST /api/admin/matches` 를 부르면 403, 비로그인으로 `POST /api/hearts` 는 401
9. **결제 결과 모름**: 토스 응답이 늦거나 끊기면 결과 화면에 "결제 확인 중"이 나오고, 1~2분 안에 마이페이지 후원 내역에 반영됨

```bash
BASE=http://localhost
T2=$(curl -s -X POST $BASE/api/auth/test-login -H 'Content-Type: application/json' -d '{"username":"tester2"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
DOOSAN=$(curl -s "$BASE/api/targets?q=두산" | sed -E 's/^\[\{"id":([0-9]+).*/\1/')

curl -s -X PUT $BASE/api/fans/$DOOSAN -H "Authorization: Bearer $T2" -H 'Content-Type: application/json' -d '{"primary":true}'
for i in 1 2 3 4; do
  curl -s -o /dev/null -w "하트 $i: %{http_code}\n" -X POST $BASE/api/hearts -H "Authorization: Bearer $T2" \
    -H 'X-Device-Id: demo-device-0001' -H 'Content-Type: application/json' -d "{\"targetId\":$DOOSAN,\"amount\":1,\"source\":\"TARGET_PAGE\"}"
done
curl -s "$BASE/api/rankings?league=KBO&period=WEEKLY" | head -c 400; echo
curl -s -o /dev/null -w "일반 사용자의 경기 등록: %{http_code}\n" -X POST $BASE/api/admin/matches -H "Authorization: Bearer $T2" -H 'Content-Type: application/json' -d '{}'
```

예상: 하트 1~3 은 200, 4 는 한도 초과 오류(409 계열), 랭킹 첫 줄에 두산 베어스, 마지막 줄 403

## 8. 접근성과 화면 품질

Vercel [Web Interface Guidelines](https://github.com/vercel-labs/web-interface-guidelines) 규칙으로 화면 코드를 검사해서 고쳤고, 화면마다 [axe-core](https://github.com/dequelabs/axe-core) 로 WCAG 2.1 AA 자동 검사를 돌려 위반 0건을 확인했습니다(홈, 구단 채널, 리그, 랭킹, 매치룸, 시청, 방송 만들기, 신청, 응원팀 고르기, 마이페이지, 운영 관리, 모바일 화면).

| 항목 | 적용 내용 |
| --- | --- |
| 명도 대비 | 본문 16:1 이상, 보조 글자 4.5:1 이상, 팀 색은 계산으로 보정 |
| 키보드 | 모든 링크와 버튼에 `:focus-visible` 테두리, 본문으로 건너뛰기 링크, 메뉴 Esc 로 닫기, 닫힌 서랍 메뉴는 Tab 이동에서 제외 |
| 화면 읽기 프로그램 | 아이콘 버튼 `aria-label`, 장식 아이콘 `aria-hidden`, 토스트와 채팅과 플레이어 상태 `aria-live`, 선택 버튼 `aria-pressed`, 표 `caption`과 `scope`, 페이지마다 `h1` |
| 폼 | 모든 입력에 라벨, `name`, `autocomplete`, 아이디 입력 맞춤법 검사 끔, 금액 입력 `inputmode="numeric"`, 모바일 확대 방지를 위한 16px 입력 글자 |
| 터치 | 손가락 기기에서 버튼 높이 40~44px, `touch-action: manipulation`, 노치 영역(`safe-area-inset`) 여백 |
| 움직임 | `prefers-reduced-motion` 이면 애니메이션 끔, `transition: all` 없음 |
| 글자 | 한국어 어절 단위 줄바꿈(`word-break: keep-all`), 진행 중 문구는 `…`, 숫자 열 `tabular-nums` |
| 주소 상태 | 랭킹(리그, 기간, 정렬), 전체 방송(탭, 리그, 페이지), 경기 일정(날짜, 리그)을 주소 쿼리에 남겨 새로고침과 공유에 그대로 유지 |

## 9. 후속 작업 (이번 제출에서 제외)

기획안 6장의 추가 기능 중 아래는 이번 제출 범위에서 제외했습니다(기획안 6장 "팀 홈 페이지"는 구단/선수 채널로 구현).

| 기능 | 내용 | 남겨 둔 이유 |
| --- | --- | --- |
| 응원팀 방송 시작 알림 | 주 응원팀 편파방이 켜지면 사이트 안 알림 | 팬 수만큼 팬아웃되는 알림 저장소 설계가 필요 |
| 팬 레벨과 배지 | 시청 시간, 하트, 후원 누적 등급 | 포인트 원장 설계 필요 |
| 승부 예측 | 무료 포인트로 승리팀 예측 | 경기 종료 정산 배치 필요, 사행성 검토 |
| 하이라이트 클립 | 녹화본 구간 공유 | Playback 구간 재생 재사용으로 추가 가능 |
| 시즌 명예의 전당 | 시즌 종료 순위 보관 | 시즌 스냅샷은 주간 스냅샷 구조로 확장 가능 |
| 스트리머 랭킹 | 팀별 인기 편파 스트리머 | 순시청자 HLL 데이터 재사용 가능 |
| 경기 일정 자동 연동 | 외부 스포츠 데이터 API | 현재는 운영자가 등록. 포트 하나로 교체 가능하게 둠 |
| 신고와 운영 제재 | 방송, 댓글 신고 | 운영자 역할은 이미 있음 |
| 영어 화면 | 해외 종목 팬용 | vue-i18n 도입 필요 |
