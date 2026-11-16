// 하트 몰림 부하 테스트 (k6)
// 실행: k6 run -e BASE=http://localhost -e TARGET=1 -e USERS=200 docs/k6/hearts.js
// 준비: 평가용 테스트 계정이 켜져 있어야 하고, USERS 수만큼 계정을 미리 만들어 둔다(아래 setup 이 회원가입을 시도).
// 주의: 한 PC 에서 돌리면 같은 IP 계정 수 신호에 걸려 대부분 의심 하트(점수 0)로 처리된다. 응답 속도와 한도 검증용이다.
// 확인할 값: http_req_duration p95, 한도 초과(409) 비율, 운영 화면 "반영 1번당 하트 수"
import http from 'k6/http'
import { check } from 'k6'

const BASE = __ENV.BASE || 'http://localhost'
const TARGET = Number(__ENV.TARGET || 1)
const USERS = Number(__ENV.USERS || 200)

export const options = {
  scenarios: {
    homerun: {
      executor: 'per-vu-iterations',
      vus: USERS,
      iterations: 4,
      maxDuration: '1m'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<200'],
    'checks{kind:limit}': ['rate==1']
  }
}

export function setup() {
  const tokens = []
  for (let i = 0; i < USERS; i++) {
    const username = `k6fan${i}`
    const password = 'k6-password-1234'
    http.post(`${BASE}/api/auth/signup`, JSON.stringify({ username, password, nickname: `k6팬${i}` }),
      { headers: { 'Content-Type': 'application/json' } })
    const res = http.post(`${BASE}/api/auth/login`, JSON.stringify({ username, password }),
      { headers: { 'Content-Type': 'application/json' } })
    tokens.push(res.json('accessToken'))
  }
  return { tokens }
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length]
  const res = http.post(`${BASE}/api/hearts`,
    JSON.stringify({ targetId: TARGET, amount: 1, source: 'TARGET_PAGE' }),
    { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}`, 'X-Device-Id': `k6-device-${__VU}` } })
  // 한 대상에 하루 3개까지라 4번째는 409 여야 한다(한도가 정확히 지켜지는지).
  if (__ITER < 3) {
    check(res, { 'heart accepted': (r) => r.status === 200 }, { kind: 'accept' })
  } else {
    check(res, { 'limit enforced': (r) => r.status === 409 }, { kind: 'limit' })
  }
}
