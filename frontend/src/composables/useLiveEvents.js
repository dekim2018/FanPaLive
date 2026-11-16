import { onBeforeUnmount, ref } from 'vue'
import { deviceId } from '../lib/device'

/**
 * 방송별 실시간 이벤트(SSE) 구독. EventSource 는 연결이 끊기면 브라우저가 자동으로 재연결한다.
 */
export function useLiveEvents(broadcastId) {
  const connected = ref(false)
  const handlers = new Map()
  let source = null

  function on(eventName, handler) {
    if (!handlers.has(eventName)) handlers.set(eventName, new Set())
    handlers.get(eventName).add(handler)
    if (source) attach(eventName)
  }

  function attach(eventName) {
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
    connected.value = false
  }

  onBeforeUnmount(close)

  return { on, connect, close, connected }
}
