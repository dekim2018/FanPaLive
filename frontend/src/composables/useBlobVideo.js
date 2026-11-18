import { onBeforeUnmount, ref, watch } from 'vue'

/**
 * 녹화/클립 재생 주소를 통째로 받아 브라우저 메모리(blob)에서 재생한다.
 * 미디어 서버(MediaMTX Playback)는 범위 요청(Range)을 지원하지 않아서, 주소를 <video> 에 그대로 넣으면
 * 아직 받지 않은 구간으로 이동(탐색)할 수 없고 iOS Safari 는 재생을 거부할 수 있다.
 * 녹화 1분이 1MB 남짓이라 한 번에 받아도 부담이 작다.
 *
 * @param urlRef 재생 주소(ref 또는 computed). 바뀌면 이전 요청을 취소하고 새로 받는다.
 */
export function useBlobVideo(urlRef) {
  const src = ref('')
  const loading = ref(false)
  const failed = ref(false)
  let controller = null

  function revoke() {
    if (src.value) URL.revokeObjectURL(src.value)
    src.value = ''
  }

  async function load(url) {
    controller?.abort()
    revoke()
    failed.value = false
    if (!url) {
      loading.value = false
      return
    }
    const mine = new AbortController()
    controller = mine
    loading.value = true
    try {
      const res = await fetch(url, { signal: mine.signal })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const blob = await res.blob()
      if (controller !== mine) return
      src.value = URL.createObjectURL(blob)
    } catch (e) {
      if (e.name !== 'AbortError' && controller === mine) failed.value = true
    } finally {
      if (controller === mine) loading.value = false
    }
  }

  watch(urlRef, (url) => load(url), { immediate: true })
  onBeforeUnmount(() => {
    controller?.abort()
    revoke()
  })

  return { src, loading, failed, retry: () => load(urlRef.value) }
}
