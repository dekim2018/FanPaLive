/**
 * 팀 색 도우미. 화면 배경이 밝으므로, 팀 색을 글자나 테두리로 쓸 때는 흰 배경 대비를 계산해서
 * 부족하면(한화 주황, 노랑 계열 등) 같은 색 계열을 더 어둡게 만들어 쓴다.
 * 기준: 글자 4.5:1, 테두리와 아이콘 3:1 (WCAG 2.1 AA)
 */
const HEX = /^#[0-9a-fA-F]{6}$/
const WHITE = '#ffffff'
const INK = '#111827'

function rgb(color) {
  return [1, 3, 5].map((i) => parseInt(color.slice(i, i + 2), 16))
}

function toHex([r, g, b]) {
  return '#' + [r, g, b].map((v) => Math.round(Math.max(0, Math.min(255, v))).toString(16).padStart(2, '0')).join('')
}

function luminance(color) {
  const [r, g, b] = rgb(color).map((v) => {
    const c = v / 255
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

export function contrast(a, b) {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (hi + 0.05) / (lo + 0.05)
}

function valid(color) {
  return typeof color === 'string' && HEX.test(color)
}

/** 원하는 대비가 나올 때까지 검정 쪽으로 섞는다(색 계열은 유지). */
function darkenUntil(color, bg, ratio) {
  let c = rgb(color)
  for (let i = 0; i < 20 && contrast(toHex(c), bg) < ratio; i++) {
    c = c.map((v) => v * 0.88)
  }
  return toHex(c)
}

/** 팀 색 위에 올릴 글자색. 흰 글자와 검은 글자 중 대비가 큰 쪽 */
export function textOn(color) {
  if (!valid(color)) return WHITE
  return contrast(color, WHITE) >= contrast(color, INK) ? WHITE : INK
}

/** 흰 배경 위 글자로 쓸 팀 색(4.5:1 이상). 보조 색이 더 잘 맞으면 보조 색 */
export function readable(color, secondary, bg = WHITE) {
  if (valid(color) && contrast(color, bg) >= 4.5) return color
  if (valid(secondary) && contrast(secondary, bg) >= 4.5 && secondary.toLowerCase() !== '#000000') return secondary
  return valid(color) ? darkenUntil(color, bg, 4.5) : INK
}

/** 흰 배경 위 버튼 배경으로 쓸 팀 색. 흰/검은 글자 중 하나와 4.5:1 이상이 되게 조정 */
export function solid(color) {
  if (!valid(color)) return '#0b57d0'
  if (Math.max(contrast(color, WHITE), contrast(color, INK)) >= 4.5) return color
  return darkenUntil(color, WHITE, 4.5)
}

/** 아주 옅은 팀 색 배경(선택 상태, 강조 영역) */
export function tint(color, amount = 0.1) {
  if (!valid(color)) return '#e8f0fe'
  return toHex(rgb(color).map((v) => 255 - (255 - v) * amount))
}

/** 화면 전체를 팀 색으로 물들일 때 쓰는 CSS 변수 묶음 */
export function teamStyle(color, secondary) {
  if (!valid(color)) return {}
  const bg = solid(color)
  return {
    '--team': bg,
    '--team-on': textOn(bg),
    '--team-text': readable(color, secondary),
    '--team-soft': tint(color, 0.1)
  }
}

export const KIND_LABEL = { TEAM: '구단', PLAYER: '선수' }
export const PERIOD_LABEL = { WEEKLY: '주간', MONTHLY: '월간', SEASON: '시즌' }
