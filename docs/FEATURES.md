# 팬파라이브 기능 정리

팬파라이브(FanPa Live)는 팬이 직접 하는 스포츠 **편파중계** 플랫폼입니다.
과제 요구사항(실시간 방송, 인증, 댓글, 후원, 녹화)을 그대로 지키면서, 그 위에 종목과 응원 대상(구단, 선수), 팬 등록, 하트 응원, 응원 랭킹, 후원 3종, 경기 매치룸을 더했습니다.
기획안 6장의 추가 기능 9가지(응원팀 방송 알림, 팬 등급과 배지, 승부 예측, 하이라이트 클립, 시즌 명예의 전당, 스트리머 순위, 경기 일정 가져오기, 신고와 운영 제재, 영어 화면)와 장애 실험, 2GB 서버 자원 예산까지 넣었습니다.

- 화면 구성(왼쪽 메뉴, 위쪽 검색, LIVE 배지와 시청자 수가 붙은 카드 격자, 리그 탭)은 국내 스트리밍 서비스의 스포츠 채널 구성을 참고했고, 로고와 색은 팬파라이브 고유 값을 씁니다.
- 색은 **밝은 화면 + 보라 키 컬러 + 구단별 2색**입니다.
  - 키 컬러: 지정색 `#b2b2ef`(hsl 240, 66%, 82%)는 흰 배경 글자로 대비가 2.0:1 이라, 같은 색상(240도)을 진하게 한 `#5e5ede` 를 버튼에 쓰고(흰 글씨 5.1:1) 금액, 닉네임 같은 강조 글자는 검은색으로 둡니다. `#b2b2ef` 는 아바타 테두리 같은 장식에, 선택 상태 테두리는 `#8080e5`(3.4:1)를 씁니다.
  - 구단별 2색: 진한 배경색 + 연한 글자색. 같은 계열(두산과 롯데 남색, KIA와 SSG 빨강)은 글자색으로 구분합니다(두산 흰색, 롯데 핑크빨강, KIA 흰색, SSG 노랑). 모든 조합은 대비 4.5:1 이상이고, 해외 팀 신청 색이 기준보다 낮으면 화면에서 흰색/검정 글자로 바꿉니다(`frontend/src/lib/team.js`).

| 구단 | 배경 | 글자 | 구단 | 배경 | 글자 |
| --- | --- | --- | --- | --- | --- |
| 두산 베어스 | `#131230` | `#FFFFFF` | 롯데 자이언츠 | `#0D2147` | `#F48FAA` |
| LG 트윈스 | `#A0073F` | `#FFE3EC` | 한화 이글스 | `#B84A00` | `#FFF1E3` |
| KIA 타이거즈 | `#B8001F` | `#FFFFFF` | NC 다이노스 | `#1F3A66` | `#E2C99A` |
| 삼성 라이온즈 | `#0B4A9A` | `#D6E8FF` | KT 위즈 | `#141414` | `#FF5A5F` |
| SSG 랜더스 | `#A80F27` | `#FFD25A` | 키움 히어로즈 | `#5A0A1C` | `#E9C28E` |

  매치룸 응원 게이지는 두 팀 배경색이 비슷하면 한쪽 막대를 글자색으로 칠합니다(두산 대 롯데는 롯데가 핑크, SSG 대 KIA는 SSG가 노랑).
- 로고는 보라 채팅 말풍선 안에 한쪽으로 기운 F(편파)와 LIVE 점을 넣은 모양이고, '팬파라이브' 글자는 검은고딕(Black Han Sans)입니다. 글꼴은 로고 다섯 글자만 남긴 3KB 파일로 사이트에 직접 넣었습니다(`frontend/public/fonts`).
- 실제 구단 로고는 쓰지 않고 팀 대표색과 약칭으로 만든 원형 표시를 씁니다.
- 경기 중계 화면(TV 중계 등)의 재송출은 금지하고, 방송 만들 때 안내에 동의해야 합니다.

## 목차

1. [과제 요구사항 대응표](#1-과제-요구사항-대응표)
2. [화면별 기능](#2-화면별-기능)
3. [팬덤 기능 규칙](#3-팬덤-기능-규칙)
4. [백엔드에서 신경 쓴 문제 12가지](#4-백엔드에서-신경-쓴-문제-12가지)
5. [서비스와 API](#5-서비스와-api)
6. [기능 스위치와 장애 시 동작](#6-기능-스위치와-장애-시-동작)
7. [확인 방법 (두산 베어스 예시)](#7-확인-방법-두산-베어스-예시)
8. [접근성과 화면 품질](#8-접근성과-화면-품질)
9. [기획안 6장 반영 현황과 남은 한계](#9-기획안-6장-반영-현황과-남은-한계)
10. [장애 실험과 자원 예산](#10-장애-실험과-자원-예산)

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
| 구단/선수 채널 | `/targets/:id` | 팀 색 배너, 주 응원팀 등록/팔로우/해제, 하트, 이번 주 순위, 팬 수, 누적 하트, 응원금, 이번 달 편파방 순시청자, 편파방 LIVE, 서포터 순위(주간/월간/시즌), **스트리머 순위(이번 달)**, 주간 순위 변화, 다시보기, **하이라이트**, 응원금 보내기 |
| 응원 랭킹 | `/rankings` | 리그 선택, 기간(주간/월간/시즌), 정렬(응원 점수, 팬 수, 하트, 후원), 지난주 대비 순위 변화 |
| 경기 일정 | `/matches` | 날짜별(어제부터 일주일), 리그별 경기 |
| 매치룸 | `/matches/:id` | 점수판, 실시간 응원 게이지(SSE), 양 팀 하트 버튼, **승부 예측(분포, 내 선택, 결과)**, 양 팀 편파방 목록 |
| 팀/선수 신청 | `/requests` | 해외 구단/선수 신청(이름, 영문, 약칭, 팀 색 미리보기, 소개, 참고 링크), 심사 중인 신청에 "나도 원해요", 내 신청 상태와 반려 사유 |
| 응원팀 고르기 | `/onboarding` | 가입 직후 이동. 리그별로 처음 누른 팀이 주 응원팀, 다음부터 팔로우 |
| 마이페이지 | `/me` | **팬 등급과 포인트(다음 등급까지 진행률), 업적 배지, 최근 승부 예측**, 내 응원팀, 오늘 남은 하트, 내가 한 후원(응원금 구분), 받은 후원 |
| 운영 관리 | `/admin` | 신청 승인/반려, **신고 처리(댓글 숨김, 방송 중단, 문제없음)**, 경기 등록, 경기 시작/점수/종료/취소, **일정 가져오기(CSV 미리보기 후 등록)**, **시즌 마감**, 운영 지표(하트 쓰기 병합 효과, 유실 하트, 매치룸 구독 수) |
| 방송 만들기/수정 | `/broadcasts/new`, `/:id/edit` | 편파중계/일반 방송 선택, 리그와 응원팀(주 응원팀 기본 선택), 경기 지정, 중계권 안내 동의 |
| 시청 | `/broadcasts/:id` | 팀 색 테마, 응원팀 정보와 매치룸 바로가기, 하트, 현재 시청자와 누적 순시청자, 후원 대상 선택(방송자 / 응원금), 채팅 응원팀 배지와 **팬 등급 배지**, **방송 신고, 댓글 신고**, 운영자 방송 중단과 댓글 숨김 |
| 스튜디오 | `/studio/:id` | 기존 송출 기능 그대로 |
| 다시보기 | `/recordings`, `/:id` | 리그 필터, 편파중계 표시, **보고 있는 장면부터 15/30/60초 클립 만들기**, 이 녹화의 하이라이트 |
| 하이라이트 | `/clips`, `/clips/:id` | 전체, 내 응원팀별, 리그별 클립 모음, 클립 재생, 링크 복사, 원본 다시보기 |
| 명예의 전당 | `/hall-of-fame` | 리그별 지난 시즌 응원 점수 최종 순위(상위 10)와 우승팀 MVP 팬 |
| 후원 결과 | `/donations/success`, `/fail` | 방송자 후원/응원금 구분, 결제 확인 중(UNKNOWN) 안내 |

공통 레이아웃

- 위쪽 헤더: 메뉴 버튼, 로고, 구단/선수 검색(입력하면 바로 추천), **언어 선택(한국어/English)**, **알림 종(안 읽은 수, 실시간)**, 방송하기, 사용자 메뉴(마이페이지, 내 방송, 응원팀 설정, 운영 관리, 로그아웃)
- 왼쪽 메뉴: 홈, 전체 LIVE, 스포츠, 응원 랭킹, 경기 일정, **하이라이트**, **명예의 전당**, 다시보기, 팀/선수 신청, 내 응원팀(★ 주 응원팀), 리그 바로가기
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
| 응원금 | 구단/선수 (가상 기금) | 반영 | 구단 채널, 편파방 시청 화면에서 "응원금" |

응원금은 토스페이먼츠 테스트 결제로 만든 팬 활동 기록이며 실제 구단에 전달되지 않습니다(화면에 안내). 후원 대상과 금액은 서버가 방송과 응원 대상을 다시 조회해서 정합니다.

### 응원팀 방송 알림

- 편파방에 영상이 실제로 들어오기 시작하면(시작 버튼만 누른 경우는 제외) broadcast-service 가 커밋 뒤 Redis Stream(`fanpa:stream:broadcasts`)에 이벤트를 남깁니다.
- fandom-service 가 소비 그룹으로 읽어 그 팀 팬 전원(주 응원팀, 팔로우 모두, 방송자 본인 제외)에게 알림을 만듭니다. 팬이 수천 명이어도 `INSERT ... SELECT` 한 문장입니다.
- 이벤트 ID 는 "방송 ID + 방송 시작 시각"이라 송출이 끊겼다 다시 붙어도 알림은 한 번입니다. 시작한 지 10분이 지난 이벤트는 알리지 않습니다(장애 뒤 밀린 알림 폭주 방지).
- 접속 중인 사람에게는 SSE 로 바로 보냅니다. 브라우저 EventSource 는 Authorization 헤더를 못 보내서, 로그인 토큰을 주소에 싣지 않고 60초짜리 1회용 티켓을 받아 연결합니다(Redis `GETDEL`).
- 읽은 알림은 30일 뒤 지웁니다(매주 일요일 04:40).

### 팬 포인트와 등급

| 등급 | 포인트 | 쌓는 방법 |
| --- | --- | --- |
| 루키 | 0 | 하트 1개 2점(하루치를 다음 날 00:30 에 한 번에 적립, 어뷰징 의심 하트 제외) |
| 일반 | 100 | 후원 100원당 1점(응원금과 편파방 방송자 후원. 응원팀 없는 일반 방송 후원은 제외. 최소 1점) |
| 진성 | 1,000 | 승부 예측 적중 30점 |
| 영구결번 | 5,000 | |

- 포인트는 원장(`fan_point_ledger`)에만 쌓이고 `(사용자, 사유, 근거 키)` 유일 제약으로 같은 근거가 두 번 쌓이지 않습니다. 합계(`fan_profiles`)는 원장에 새로 들어간 행만 같은 문장(CTE)에서 더합니다.
- 업적 배지: 예언자(예측 5번 적중), 개근 응원단(하트 준 날 7일 이상), 서포터(후원 1번 이상).
- 채팅 닉네임 옆에 등급 배지가 붙습니다(루키는 표시하지 않음). 포인트는 돈으로 바꿀 수 없습니다.

### 승부 예측

- 경기 시작 전(`SCHEDULED` 이고 시작 시각 전)까지 이길 팀을 고르고 바꿀 수 있습니다. 분포는 누구나 봅니다.
- 운영자가 경기를 종료하거나 취소하면 같은 트랜잭션에서 정산합니다. 무승부와 취소는 무효(VOID), 적중은 30점, 결과는 알림으로 갑니다.
- 정산은 `UPDATE ... WHERE result IS NULL RETURNING` 이라 두 번 돌려도 두 번째는 아무것도 바꾸지 않습니다.
- 돈이나 후원과 섞지 않는 무료 예측입니다(사행성 방지).

### 하이라이트 클립

- 다시보기에서 지금 보는 장면부터 15, 30, 60초를 고르면 클립이 됩니다. 영상을 새로 자르지 않고 "녹화 시작부터 몇 초, 몇 초 동안"만 저장하고, 재생은 MediaMTX Playback 의 구간 재생(`/get?start=&duration=`)을 씁니다. 저장 공간이 늘지 않고 바로 만들어집니다.
- 녹화가 끝난 영상(READY)에서만, 한 사람이 녹화 하나에 20개까지 만들 수 있습니다. 지우기는 만든 사람, 원본 녹화 소유자, 운영자만. 원본을 지우면 클립도 같이 지워집니다.
- 클립은 원본 녹화의 응원팀 정보를 물려받아 팀별로 모아 볼 수 있습니다.

### 시즌 명예의 전당

- 새 시즌 첫날 00:20 에 지난 시즌 응원 점수 최종 순위를 `ranking_snapshots(SEASON)` 에 남기고, 우승팀에 시즌 동안 가장 많이 기여한 팬을 MVP 로 기록합니다. 운영자가 날짜를 골라 다시 실행할 수 있고, 다시 실행하면 최신 값으로 덮어씁니다.

### 스트리머 순위

- 응원팀별, 월별로 그 팀 편파방을 연 스트리머를 비교합니다. 점수 = 순시청자 1명당 1점 + 받은 후원 1,000원당 1점(돈만으로 순위가 정해지지 않게 시청자 비중을 크게 둠).
- 순시청자는 기존 시청 기록 때 같은 시청자 키를 스트리머별 HyperLogLog 에도 넣어 셉니다(키당 최대 12KB). 후원은 확정된 후원만 DB 에서 합칩니다.

### 경기 일정 가져오기

- 운영자가 CSV 파일을 올리거나 엑셀에서 복사해 붙여 넣으면(`날짜,시각,홈팀,원정팀,구장`) 미리보기에서 줄마다 새 경기, 중복, 오류(이유와 줄 번호)를 보여 주고, 확인 후 새 경기만 등록합니다. 같은 파일을 다시 올려도 중복은 건너뜁니다.
- 팀은 정식 이름, 영문 이름, 약칭 모두로 찾습니다(예: "두산 베어스", "두산"). 시각은 KST 로 읽습니다.
- 공급원은 `ScheduleSourcePort` 하나로 감쌌습니다. 지금은 CSV 어댑터만 있고, 외부 스포츠 데이터 API 를 쓰게 되면 어댑터만 하나 더 만들면 됩니다(서비스 코드 그대로).

### 신고와 운영 제재

- 로그인 사용자는 방송과 댓글을 신고할 수 있습니다(사유: 중계권 영상 재송출, 욕설과 비하, 도배와 광고, 기타). 같은 대상을 두 번 신고할 수 없고, 내 방송과 댓글은 신고할 수 없습니다.
- 신고 시점의 내용(댓글 본문, 방송 제목)을 남겨서 대상이 지워진 뒤에도 운영자가 판단할 수 있습니다.
- 운영자 조치: 댓글 숨김(삭제하고 모든 시청자 화면에서 실시간으로 내림), 방송 중단(방송 종료, 송출 연결 끊기, 녹화 마무리, 공개 목록에서 숨김, 다시 시작 금지), 문제없음. 같은 대상의 열린 신고는 한 번에 닫힙니다.
- 운영자 경로는 `/api/moderation/**` 입니다(`/api/admin/**` 은 게이트웨이에서 fandom-service 로 가므로). 서버는 보안 설정과 컨트롤러 양쪽에서 ADMIN 을 확인합니다.

### 영어 화면

- 헤더의 언어 선택으로 한국어와 영어를 바꿉니다. 처음에는 브라우저 언어(`navigator.languages`)로 정하고, 고른 값은 저장합니다.
- 한국어 문장을 그대로 키로 쓰는 방식이라(gettext 방식) 한국어 화면은 사전 없이 원문이 나오고, 영어 사전에 없는 값(사용자 닉네임, 방송 제목)은 원문 그대로 보입니다.
- 화면 문구, 서버 오류 문구(값이 들어가는 문구는 정규식으로), 시드 데이터(종목, 리그, KBO 10개 구단 이름)를 번역합니다. 날짜, 숫자, 금액(₩) 표기도 언어에 맞춥니다.
- 문장 안 일부를 굵게 하거나 링크로 만들 때는 문장을 조각내지 않고 자리표시자에 끼워 넣어(`TrText`) 영어 어순이 깨지지 않게 했습니다.

## 4. 백엔드에서 신경 쓴 문제 12가지

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
| 10 | **알림 팬아웃**: 인기 팀 팬 수천 명에게 알림 한 번 | `fan_memberships` 에서 `INSERT ... SELECT ... ON CONFLICT DO NOTHING RETURNING user_id` 한 문장, 실시간 전송은 커밋 뒤 접속 중인 사람에게만 | 팬마다 INSERT(왕복 수천 번), 웹 푸시(이번 범위 밖) | `JdbcNotificationRepository`, `NotificationService`, `NotificationHub` |
| 11 | **포인트 이중 적립**: 같은 후원 이벤트가 두 번 오거나 새벽 적립을 두 번 돌림 | 원장 유일 제약 `(user_id, reason, ref_key)` + CTE 로 "원장에 새로 들어간 행만" 합계에 더함 | 합계만 저장(원인 추적 불가), 애플리케이션에서 먼저 조회 후 저장(경쟁 조건) | `JdbcPointLedgerRepository` |
| 12 | **예측 정산이 두 번 도는 경우**: 운영자가 종료를 두 번 누르거나 재시도 | `UPDATE ... WHERE result IS NULL RETURNING *` 로 바뀐 행만 돌려받고, 그 행에만 포인트와 알림 | 정산 여부 플래그를 경기에 따로 둠(두 테이블을 맞춰야 함) | `JdbcPredictionRepository.settle`, `PredictionService` |

부하 테스트 스크립트: [docs/k6/hearts.js](k6/hearts.js)

## 5. 서비스와 API

### 서비스 구성

| 서비스 | 포트 | 추가/변경 |
| --- | --- | --- |
| gateway | 8080 | fandom-service 라우트, 응원금과 시청자 수 라우트, 알림/예측/명예의 전당(fandom), 클립(broadcast), 신고/운영/스트리머 순위(interaction) |
| auth-service | 8081 | `role`(USER/ADMIN), JWT 에 `role`, `jat`(가입 시각) 클레임, 운영자 테스트 계정 `admin1` |
| broadcast-service | 8082 | 방송/녹화에 응원팀 정보(`side`), 목록 필터, fandom-service 확인(장애 시 일반 방송으로 생성), 편파방 시작 이벤트, 하이라이트 클립, 운영 제재(중단, 숨김) |
| interaction-service | 8083 | 후원 3종, UNKNOWN 처리, 결제 대사, 후원 이벤트 스트림, 순시청자, 내 후원/받은 후원, 신고와 운영 조치, 스트리머 순위 |
| fandom-service (신규) | 8084 | 종목, 리그, 응원 대상, 신청, 팬, 하트, 랭킹, 경기, 운영, 알림, 팬 포인트와 등급, 승부 예측, 명예의 전당, 일정 가져오기 |

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
| GET | `/api/notifications?page=&size=`, `/unread-count` | 필수 | 내 알림, 안 읽은 수 |
| POST | `/api/notifications/read` | 필수 | 읽음 처리(`ids` 비우면 전부) |
| POST | `/api/notifications/stream-ticket` | 필수 | 실시간 구독용 60초 1회용 티켓 |
| GET | `/api/notifications/stream?ticket=` | 티켓 | 알림 SSE(`unread`, `notification` 이벤트) |
| GET | `/api/fans/levels?userIds=` | - | 채팅용 팬 등급(최대 100명) |
| GET | `/api/fans/profile/me` | 필수 | 내 포인트, 등급, 다음 등급까지, 업적 |
| GET, POST | `/api/matches/{id}/prediction` | 선택, 필수 | 예측 분포와 내 선택, 예측하기(`{"pick":"HOME"}`) |
| GET | `/api/predictions/me` | 필수 | 내 최근 예측 |
| GET | `/api/hall-of-fame?league=` | - | 지난 시즌 최종 순위와 MVP |
| POST | `/api/admin/matches/import/preview`, `/api/admin/matches/import` | ADMIN | 일정 미리보기, 등록(`leagueCode`, `payload`) |
| POST | `/api/admin/seasons/close?date=` | ADMIN | 시즌 마감 수동 실행 |
| GET | `/api/clips?target=&league=`, `/api/clips/{id}` | - | 클립 목록, 상세(재생 주소 포함) |
| GET, POST | `/api/recordings/{id}/clips` | -, 필수 | 녹화의 클립, 클립 만들기(`offsetSeconds`, `durationSeconds`, `title`) |
| DELETE | `/api/clips/{id}` | 필수 | 만든 사람, 녹화 소유자, 운영자 |
| POST | `/api/reports` | 필수 | 신고(`type`: BROADCAST, COMMENT, `targetId`, `reason`, `detail`) |
| GET | `/api/moderation/reports?status=` | ADMIN | 신고 목록(같은 대상 열린 신고 수 포함) |
| POST | `/api/moderation/reports/{id}/resolve` | ADMIN | `action`: HIDE_COMMENT, END_BROADCAST, DISMISS |
| POST | `/api/moderation/comments/{id}/hide`, `/api/moderation/broadcasts/{id}/end` | ADMIN | 신고 없이 바로 조치 |
| GET | `/api/streamers/ranking?target=&month=` | - | 응원팀별 월간 스트리머 순위 |

내부 API(외부 라우트 없음, `X-Internal-Key` 필요): `/internal/targets/{id}`, `/internal/matches/{id}`(fandom), `/internal/donations/target-totals?date=`(interaction), `POST /internal/broadcasts/{id}/moderate`(broadcast, 운영 제재)

서비스 사이 이벤트(Redis Stream, 소비 그룹 `fandom`): `fanpa:stream:donations`(후원 확정, interaction → fandom), `fanpa:stream:broadcasts`(편파방 시작, broadcast → fandom)

### 데이터베이스

| DB | 마이그레이션 |
| --- | --- |
| auth_db | `V2__add_user_role.sql` |
| broadcast_db | `V2__add_fan_side.sql` (모두 NULL 허용 컬럼), `V3__add_fan_side_text_color.sql` (구단 글자색), `V4__moderation_and_clips.sql` (운영 제재 컬럼, `clips`) |
| interaction_db | `V2__fan_target_donations.sql` (대상 종류 CHECK, UNKNOWN 상태, 부분 인덱스, `payment_reconcile_logs`), `V3__reports.sql` (`reports`, 스트리머별 후원 합계 인덱스) |
| fandom_db | `V1__create_fandom.sql`, `V2__seed_sports_and_kbo.sql`, `V3__kbo_two_tone_palette.sql` (KBO 2색 팔레트), `V4__notifications_levels_predictions_hof.sql` (`notifications`, `fan_point_ledger`, `fan_profiles`, `match_predictions`, `season_mvps`) |

이미 돌고 있는 서버도 `docker compose up -d --build` 한 번이면 `postgres-init` 이 `fandom_db` 를 만들고 각 서비스가 V2 를 적용합니다. 기존 데이터는 그대로입니다.

## 6. 기능 스위치와 장애 시 동작

| 스위치 | 끄면 |
| --- | --- |
| `FEATURE_HEARTS` | 하트 API 가 막히고 버튼이 사라짐. 랭킹 조회는 그대로 |
| `FEATURE_MATCHES` | 경기 일정과 매치룸이 막힘 |
| `FEATURE_TARGET_REQUESTS` | 신청과 투표가 막힘 |

| 장애 | 동작 |
| --- | --- |
| fandom-service 중단 | 방송 만들기는 일반 방송으로 계속 됨(화면에 안내), 시청/댓글/방송자 후원 정상. 응원금 주문만 잠시 실패 |
| Redis 중단 | 하트 한도를 확인할 수 없어 하트만 막힘(한도 없이 받지 않음). 랭킹은 DB 로 다시 만들 수 있음 |
| 결제사 응답 지연 | 후원이 UNKNOWN 으로 남고 1분 주기로 확정 |
| 후원 이벤트 유실 | 매일 04:10 결제 원본과 비교해 보정 |
| 서버 강제 종료 | 하트 최대 200ms 분량 유실 가능(정상 종료 시에는 큐를 비우고 종료) |
| 편파방 시작 이벤트를 못 남김(Redis 중단) | 방송은 그대로 시작되고 알림만 빠짐(알림은 부가 기능) |
| fandom-service 가 멈춘 동안 이벤트가 쌓임 | 다시 켜지면 밀린 것부터 처리, 10분 지난 편파방 알림은 건너뜀, 같은 이벤트는 한 번만 반영 |
| 알림 SSE 끊김 | 1회용 티켓을 새로 받아 2초부터 최대 60초까지 늘려 가며 다시 연결, 놓친 알림은 목록에 있음 |

이 표의 주요 항목은 [장애 실험](CHAOS.md) 스크립트로 직접 확인할 수 있습니다.

## 7. 확인 방법 (두산 베어스 예시)

준비: `.env` 에 `TEST_ACCOUNTS_ENABLED=true` 로 띄우면 로그인 화면에 `tester1`, `tester2`, `admin1`(운영자) 원클릭 로그인이 생깁니다.

1. **응원팀 정하기**: `tester2` 로그인 → 왼쪽 메뉴 "응원팀을 골라 보세요" → KBO 에서 두산 베어스 클릭(★ 주 응원), 한화 이글스 클릭(팔로우) → 저장. 왼쪽 메뉴 "내 응원팀"에 두 팀이 보임
2. **편파중계 만들기**: `tester1` 로그인 → 방송하기 → 편파중계, KBO 리그, 두산 베어스 선택 → 중계권 안내 체크 → 만들고 스튜디오로 이동 → 송출 시작
3. **시청 화면**: `tester2` 로 홈 → "내 응원팀 LIVE" 에 방송 카드(LIVE 배지, 시청자 수, 두산 표시) → 들어가면 응원팀 정보, 하트 버튼, 채팅 닉네임 옆 "두산" 배지
4. **하트**: 하트 보내기를 여러 번 누르면 3번째 이후 "한 대상에 하루 3개" 안내. 랭킹 화면에서 두산 베어스 점수가 오름(최대 0.2초 지연)
5. **후원 3종**: 시청 화면 후원 패널에서 "방송자 후원"과 "응원금"을 각각 결제 → 결과 화면 문구 확인 → 두산 베어스 채널의 응원금 합계와 랭킹 후원 열이 늘어남
6. **경기와 매치룸**: `admin1` 로그인 → 운영 관리 → 경기 관리에서 두산 베어스(홈) vs LG 트윈스(원정) 등록 → 경기 시작 → `/matches` 에서 매치룸 열기 → 다른 창에서 하트를 보내면 0.5초 안에 게이지가 움직임
7. **신청과 승인**: `tester2` → 팀/선수 신청 → MLB, "LA 다저스" 신청 → `tester1` 이 같은 이름을 " la  다저스 " 처럼 신청하면 새 신청 대신 "나도 원해요"로 합쳐짐 → `admin1` 이 승인 → MLB 리그에 공개
8. **권한**: `tester2` 토큰으로 `POST /api/admin/matches` 를 부르면 403, 비로그인으로 `POST /api/hearts` 는 401
9. **결제 결과 모름**: 토스 응답이 늦거나 끊기면 결과 화면에 "결제 확인 중"이 나오고, 1~2분 안에 마이페이지 후원 내역에 반영됨
10. **응원팀 방송 알림**: `tester2`(두산 팬) 로그인 상태로 둔 채 `tester1` 이 두산 편파방 송출 시작 → `tester2` 화면 헤더 알림 종에 숫자가 붙고 "두산 베어스 편파방이 시작됐어요" 토스트 → 누르면 시청 화면
11. **승부 예측**: `tester2` 로 예정 경기 매치룸 → 두산 승 선택(다시 눌러 바꾸기 가능) → `admin1` 이 경기 시작, 점수, 종료 → `tester2` 에게 "승부 예측 적중!" 알림, 마이페이지 포인트 +30
12. **하이라이트**: 다시보기 상세에서 원하는 장면으로 옮긴 뒤 30초, 제목 입력, 클립 만들기 → 하이라이트 메뉴와 두산 채널 하이라이트 탭에 표시
13. **신고와 중단**: `tester2` 가 방송 신고(중계권 영상 재송출) → `admin1` 운영 관리 신고 처리에서 방송 중단 → 방송이 종료되고 목록에서 사라지며 `tester1` 은 다시 시작할 수 없음("운영 정책 위반으로 중단된 방송은 다시 시작할 수 없습니다")
14. **일정 가져오기**: `admin1` 운영 관리 → 일정 가져오기 → 예시 넣기 → 미리보기(2줄째는 같은 경기라 중복) → 등록 → 같은 내용으로 다시 미리보기하면 모두 중복
15. **영어 화면**: 헤더 언어 선택에서 English → 메뉴, 버튼, 오류 문구, 구단 이름(Doosan Bears)이 영어로 바뀜. 닉네임과 방송 제목은 원문 그대로

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

Vercel [Web Interface Guidelines](https://github.com/vercel-labs/web-interface-guidelines) 규칙으로 화면 코드를 검사해서 고쳤고, 화면마다 [axe-core](https://github.com/dequelabs/axe-core) 로 WCAG 2.1 AA 자동 검사를 돌려 위반 0건을 확인했습니다(홈, 구단 채널, 리그, 랭킹, 매치룸, 시청, 방송 만들기, 신청, 응원팀 고르기, 마이페이지, 운영 관리, 모바일 화면, 그리고 추가 기능 화면인 알림, 승부 예측, 팬 등급, 하이라이트 목록과 재생, 클립 만들기, 명예의 전당, 스트리머 순위, 신고 처리, 일정 가져오기. 한국어와 영어 화면 각각 29개, 모두 58번).

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
| 언어 | `<html lang>` 을 화면 언어에 맞춤, 영어 화면에서도 같은 대비와 줄바꿈 규칙, 날짜와 상대 시각(`Intl.RelativeTimeFormat`) 현지화 |
| 대화상자 | 신고 창 `role="dialog"`, `aria-modal`, 제목 연결, 열리면 첫 선택지로 포커스, Esc 로 닫기 |

## 9. 기획안 6장 반영 현황과 남은 한계

기획안 6장 추가 기능은 모두 구현했습니다(기획안 "팀 홈 페이지"는 구단/선수 채널로 구현).

| 기능 | 구현 | 남은 한계 |
| --- | --- | --- |
| 응원팀 방송 시작 알림 | 사이트 안 알림 + SSE 실시간 + 알림 목록 | 브라우저 꺼져 있을 때 받는 웹 푸시는 없음(서비스 워커와 VAPID 키가 필요) |
| 팬 레벨과 배지 | 4단계 등급, 포인트 원장, 업적 배지 3종, 채팅 등급 배지 | 시청 시간은 포인트에 넣지 않음(탭만 열어 두는 어뷰징을 막을 방법이 먼저 필요) |
| 승부 예측 | 무료 예측, 종료 시 정산, 적중 30점, 결과 알림 | 점수 차, 선발 투수 같은 세부 예측은 없음 |
| 하이라이트 클립 | 녹화 구간 저장, 팀별 모음, 재생, 삭제 권한 | 썸네일 이미지 없음(팀 색 카드로 대신) |
| 시즌 명예의 전당 | 자동 마감 + 수동 재실행, 상위 10과 MVP | 선수 대상은 시즌 개념이 리그와 같다고 가정 |
| 스트리머 랭킹 | 팀별 월간, 순시청자 HLL + 확정 후원 | 순시청자는 근사치(약 0.81% 오차) |
| 경기 일정 자동 연동 | 가져오기 포트 + CSV 어댑터, 미리보기, 중복 건너뛰기 | 외부 스포츠 데이터 API 어댑터는 계약과 키가 필요해서 만들지 않음(포트만 준비) |
| 신고와 운영 제재 | 방송/댓글 신고, 숨김, 중단, 문제없음, 같은 대상 일괄 처리 | 사용자 정지(계정 단위 제재)는 없음 |
| 영어 화면 | 전체 화면, 서버 오류 문구, 시드 데이터 | 사용자가 신청한 해외 팀 이름은 신청한 한글 이름으로 보임(영문 이름 칸은 상세 화면에 표시) |

## 10. 장애 실험과 자원 예산

- [장애 실험](CHAOS.md): fandom-service 중단, Redis 중단, 결제사 무응답, 이벤트 밀림 4가지를 스크립트로 직접 일으키고 기대 동작을 확인합니다(`scripts/chaos/`).
- [2GB 서버 자원 예산](RESOURCE-BUDGET.md): 컨테이너별 메모리 상한과 JVM 옵션(`docker-compose.small.yml`), 측정 스크립트(`scripts/measure-memory.sh`).
