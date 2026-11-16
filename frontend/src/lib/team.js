/** 팀 색으로 화면 강조색을 바꿀 때 쓰는 도우미 */
export function teamStyle(color) {
  if (!color || !/^#[0-9a-fA-F]{6}$/.test(color)) return {}
  return { '--team': color }
}

/** 배경색 위에 올릴 글자색(밝은 팀 색이면 검정) */
export function textOn(color) {
  if (!color || !/^#[0-9a-fA-F]{6}$/.test(color)) return '#fff'
  const r = parseInt(color.slice(1, 3), 16)
  const g = parseInt(color.slice(3, 5), 16)
  const b = parseInt(color.slice(5, 7), 16)
  return (r * 299 + g * 587 + b * 114) / 1000 > 160 ? '#111' : '#fff'
}

export const KIND_LABEL = { TEAM: '구단', PLAYER: '선수' }
export const PERIOD_LABEL = { WEEKLY: '주간', MONTHLY: '월간', SEASON: '시즌' }

function luminance(color) {
  const r = parseInt(color.slice(1, 3), 16)
  const g = parseInt(color.slice(3, 5), 16)
  const b = parseInt(color.slice(5, 7), 16)
  return (r * 299 + g * 587 + b * 114) / 1000
}

/**
 * 어두운 배경 위 글자로 쓸 팀 색. 두산 베어스 남색처럼 너무 어두운 색이면 보조 색, 그것도 어두우면 밝은 회색을 쓴다.
 */
export function readable(color, secondary) {
  const ok = (c) => c && /^#[0-9a-fA-F]{6}$/.test(c) && luminance(c) >= 70
  if (ok(color)) return color
  if (ok(secondary)) return secondary
  return '#e6e8ec'
}
