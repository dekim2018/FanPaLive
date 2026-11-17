/**
 * 마지막으로 시작한 요청의 응답만 쓰게 한다.
 * 필터를 빠르게 바꾸거나 자동 새로고침과 겹치면 늦게 도착한 이전 요청의 응답이 최신 결과를 덮어쓰는데, 그것을 막는다.
 *
 *   const latest = latestOnly()
 *   const { value, stale } = await latest(api.list(params))
 *   if (stale) return
 *
 * 이전 요청이 실패한 경우도 stale 로 돌려주고, 마지막 요청의 실패만 그대로 던진다.
 */
export function latestOnly() {
  let seq = 0
  return async function run(promise) {
    const id = ++seq
    try {
      const value = await promise
      return { value, stale: id !== seq }
    } catch (e) {
      if (id !== seq) return { value: undefined, stale: true }
      throw e
    }
  }
}
