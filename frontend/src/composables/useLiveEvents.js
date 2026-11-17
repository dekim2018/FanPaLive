import { onBeforeUnmount, ref } from 'vue'
import { deviceId } from '../lib/device'

/**
 * 방송별 실시간 이벤트(SSE) 구독. EventSource 는 연결이 끊기면 브라우저가 자동으로 재연결한다.
 */
export function useLiveEvents(broadcastId) {
  const connected = ref(false)
  const handlers = new Map()
  const attached = new Set()
  let source = null

  /** 핸들러를 등록하고, 해제 함수를 돌려준다. 컴포넌트가 먼저 사라지면 해제해야 같은 이벤트가 중복 처리되지 않는다. */
  function on(eventName, handler) {
    if (!handlers.has(eventName)) handlers.set(eventName, new Set())
    handlers.get(eventName).add(handler)
    if (source) attach(eventName)
    return () => handlers.get(eventName)?.delete(handler)
  }

  // 이벤트 이름마다 리스너는 하나만 붙인다(연결 뒤에 on 을 다시 불러도 중복으로 붙지 않게).
  function attach(eventName) {
    if (attached.has(eventName)) return
    attached.add(eventName)
    source.addEventListener(eventName, (e) => {
      let data = null
      try { data = JSON.parse(e.data) } catch { data = e.data }
      handlers.get(eventName)?.forEach((h) => h(data))
    })
  }

  function connect() {
    if (source) return
    // vid 는 누적 순시청자 집계용. 재연결로 다시 보내도 서버는 한 명으로 센다.
    source = new EventSource(`/api/broadcasts/${broadcastId}/events?vid=${encodeURIComponent(deviceId())}`)
    source.addEventListener('open', () => { connected.value = true })
    source.addEventListener('error', () => { connected.value = false })
    source.addEventListener('connected', () => { connected.value = true })
    for (const name of handlers.keys()) attach(name)
  }

  function close() {
    source?.close()
    source = null
    attached.clear()
    connected.value = false
  }

  onBeforeUnmount(close)

  return { on, connect, close, connected }
}
