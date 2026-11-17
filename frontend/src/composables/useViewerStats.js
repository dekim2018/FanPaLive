import { ref } from 'vue'
import { viewerApi } from '../api/fandom'

/** 방송 목록의 시청자 수를 한 번에 가져온다(카드마다 요청하지 않도록). */
export function useViewerStats() {
  const stats = ref({})
  async function refresh(broadcasts) {
    const ids = broadcasts.filter((b) => b.status === 'LIVE').map((b) => b.id)
    if (!ids.length) return
    try {
      stats.value = { ...stats.value, ...(await viewerApi.stats(ids)) }
    } catch {
      // 시청자 수는 보조 정보라 실패해도 목록은 보여 준다.
    }
  }
  return { stats, refresh }
}
