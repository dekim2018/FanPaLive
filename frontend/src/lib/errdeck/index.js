import { createErrDeck } from './errdeck-vue.js'

// 토큰과 Bearer 값은 보내기 전에 가린다. 주소의 쿼리는 SDK 가 기본으로 빼고 보낸다.
const SECRETS = /((?:[?&]|\b)token=)[^&#\s"']*|(Bearer\s+)[A-Za-z0-9._~+/=-]+/g
const redact = (text) => text.replace(SECRETS, (_m, token, bearer) => `${token ?? ''}${bearer ?? ''}[Filtered]`)

/**
 * ErrDeck(같은 사람이 만든 모니터링 서비스) 연결. 처리하지 못한 예외와 Vue 오류, 페이지 이동과 API 호출(axios 포함)의 시간을 보낸다.
 * /api 호출에는 traceparent 를 붙여 gateway 와 각 서비스의 요청과 같은 추적으로 잇는다(같은 출처만). docs/ERRDECK.md
 */
export function startErrDeck(app, router, dsn) {
  app.use(
    createErrDeck({
      dsn,
      environment: import.meta.env.MODE,
      release: import.meta.env.VITE_APP_RELEASE,
      router,
      tracesSampleRate: Number(import.meta.env.VITE_ERRDECK_TRACE_SAMPLE_RATE ?? 0.1),
      redact,
    }),
  )
}
