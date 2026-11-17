/**
 * 탭, 필터, 페이지 같은 화면 상태를 주소(쿼리)에 남긴다. 새로고침이나 링크 공유를 해도 같은 화면이 열린다.
 * 기본값과 같은 항목은 주소에서 뺀다.
 */
export function q(route, key, fallback, allowed) {
  const v = route.query[key]
  if (typeof v !== 'string' || !v) return fallback
  return !allowed || allowed.includes(v) ? v : fallback
}

export function syncQuery(router, route, values, defaults = {}) {
  const query = {}
  Object.entries(values).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined && String(v) !== String(defaults[k] ?? '')) query[k] = String(v)
  })
  router.replace({ query }).catch(() => {})
}
