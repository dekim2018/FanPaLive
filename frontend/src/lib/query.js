/**
 * 탭, 필터, 페이지 같은 화면 상태를 주소(쿼리)에 남긴다. 새로고침이나 링크 공유를 해도 같은 화면이 열린다.
 * 기본값과 같은 항목은 주소에서 뺀다.
 */
export function q(route, key, fallback, allowed) {
  const v = route.query[key]
  if (typeof v !== 'string' || !v) return fallback
  return !allowed || allowed.includes(v) ? v : fallback
}

/** 화면이 스스로 맞춘 주소. 이 주소로의 이동은 화면을 다시 만들지 않는다(App.vue) */
let synced = null

export function syncQuery(router, route, values, defaults = {}) {
  const query = {}
  Object.entries(values).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined && String(v) !== String(defaults[k] ?? '')) query[k] = String(v)
  })
  synced = router.resolve({ query }).fullPath
  router.replace({ query }).catch(() => {})
}

/** 이번 이동이 syncQuery 로 맞춘 주소인지. 링크, 뒤로 가기처럼 바깥에서 쿼리가 바뀐 이동이면 false */
export function consumeSynced(to) {
  const hit = synced === to.fullPath
  synced = null
  return hit
}
