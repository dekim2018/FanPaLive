/**
 * 팀 색 도우미. 구단마다 두 가지 색을 쓴다.
 * - colorPrimary: 진한 배경색 (예: 두산 남색, 롯데 남색)
 * - colorSecondary: 그 위에 올리는 연한 글자색 (예: 두산 흰색, 롯데 핑크빨강, SSG 노랑)
 * 같은 계열 구단(두산과 롯데, KIA와 SSG)은 글자색으로 구분된다.
 * 사용자가 신청한 해외 팀처럼 두 색의 대비가 부족하면 흰색/검정 중 대비가 큰 쪽으로 바꾼다.
 * 기준: 글자 4.5:1 (WCAG 2.1 AA)
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
  if (!valid(color)) return '#5e5ede'
  if (Math.max(contrast(color, WHITE), contrast(color, INK)) >= 4.5) return color
  return darkenUntil(color, WHITE, 4.5)
}

/** 아주 옅은 팀 색 배경(선택 상태, 강조 영역) */
export function tint(color, amount = 0.1) {
  if (!valid(color)) return '#eeeefc'
  return toHex(rgb(color).map((v) => 255 - (255 - v) * amount))
}

function mix(a, b, t) {
  const x = rgb(a)
  const y = rgb(b)
  return toHex(x.map((v, i) => v * (1 - t) + y[i] * t))
}

/** 배경색 위 글자색. 지정한 글자색이 4.5:1 이 안 되면 흰색/검정 중 대비가 큰 쪽 */
export function teamFg(bg, fg) {
  const base = solid(bg)
  return valid(fg) && contrast(base, fg) >= 4.5 ? fg : textOn(base)
}

/** 순위, 팬 수 같은 보조 글자: 글자색을 배경 쪽으로 조금 섞되 4.5:1 은 지킨다 */
export function teamSub(bg, fg) {
  const base = solid(bg)
  const text = teamFg(bg, fg)
  for (let t = 0.3; t > 0; t -= 0.02) {
    const c = mix(text, base, t)
    if (contrast(base, c) >= 4.6) return c
  }
  return text
}

/** 화면 일부를 팀 색으로 물들일 때 쓰는 CSS 변수 묶음 */
export function teamStyle(color, secondary) {
  if (!valid(color)) return {}
  const bg = solid(color)
  return {
    '--team': bg,
    '--team-on': teamFg(color, secondary),
    '--team-sub': teamSub(color, secondary),
    '--team-text': readable(color),
    '--team-soft': tint(color, 0.1)
  }
}

/**
 * 응원 게이지 두 막대의 색. 두 팀 배경색이 비슷하면(두산과 롯데 남색, KIA와 SSG 빨강) 한쪽을 글자색으로 칠한다.
 * 원정 글자색이 흰 화면에서 안 보이면(흰색 등) 홈 쪽을 바꾼다.
 */
export function gaugeColors(home, away) {
  const h = solid(home?.colorPrimary)
  const a = solid(away?.colorPrimary)
  const dist = Math.hypot(...rgb(h).map((v, i) => v - rgb(a)[i]))
  if (dist >= 90) return { home: h, away: a }
  const awayFg = teamFg(away?.colorPrimary, away?.colorSecondary)
  if (contrast(awayFg, WHITE) >= 1.4) return { home: h, away: awayFg }
  return { home: teamFg(home?.colorPrimary, home?.colorSecondary), away: a }
}

export const KIND_LABEL = { TEAM: '구단', PLAYER: '선수' }
export const PERIOD_LABEL = { WEEKLY: '주간', MONTHLY: '월간', SEASON: '시즌' }
