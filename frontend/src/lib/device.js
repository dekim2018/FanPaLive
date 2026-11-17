/**
 * 브라우저 식별값. 로그인 정보가 아니라 "같은 브라우저인지"만 구분한다.
 * - X-Device-Id: 하트 어뷰징 신호(한 기기에서 여러 계정)
 * - vid: 누적 순시청자 집계(비로그인 포함)
 * 저장소를 쓸 수 없는 환경(사생활 보호 모드 등)에서는 탭이 살아 있는 동안만 쓰는 값으로 대신한다.
 */
const KEY = 'fanpa.device'
let memoryId = null

function randomId() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID()
  return 'd' + Math.random().toString(36).slice(2) + Date.now().toString(36)
}

export function deviceId() {
  try {
    let id = window.localStorage.getItem(KEY)
    if (!id) {
      id = randomId()
      window.localStorage.setItem(KEY, id)
    }
    return id
  } catch {
    memoryId = memoryId || randomId()
    return memoryId
  }
}

export function deviceHeaders() {
  return { 'X-Device-Id': deviceId() }
}
