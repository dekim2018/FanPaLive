import { intlLocale, locale, tr } from '../i18n'

const dateFormatter = new Intl.DateTimeFormat(intlLocale, {
  month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
})

export function formatDate(value) {
  if (!value) return '-'
  return dateFormatter.format(new Date(value))
}

export function formatDuration(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds || 0))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const pad = (n) => String(n).padStart(2, '0')
  return h > 0 ? `${h}:${pad(m)}:${pad(sec)}` : `${m}:${pad(sec)}`
}

/** 원화. 영어 화면은 "₩10,000" */
export function formatWon(amount) {
  const n = Number(amount || 0).toLocaleString(intlLocale)
  return locale === 'en' ? `₩${n}` : tr('{0}원', n)
}

export function elapsedSince(value) {
  if (!value) return ''
  return formatDuration((Date.now() - new Date(value).getTime()) / 1000)
}

/** 큰 수 줄여 쓰기. 한국어는 만 단위(1.2만), 영어는 천 단위(12K) */
export function formatCount(n) {
  const v = Number(n || 0)
  if (locale === 'en') {
    if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M`
    if (v >= 10_000) return `${(v / 1000).toFixed(v >= 100_000 ? 0 : 1)}K`
    return v.toLocaleString(intlLocale)
  }
  if (v >= 10000) return `${(v / 10000).toFixed(v >= 100000 ? 0 : 1)}${tr('만')}`
  return v.toLocaleString(intlLocale)
}

const timeFormatter = new Intl.DateTimeFormat(intlLocale, { hour: '2-digit', minute: '2-digit' })
export function formatTime(value) {
  return value ? timeFormatter.format(new Date(value)) : '-'
}

/** "3분 전" 같은 상대 시각(알림 목록용) */
const relative = new Intl.RelativeTimeFormat(intlLocale, { numeric: 'auto' })
export function formatAgo(value) {
  if (!value) return ''
  const diff = (new Date(value).getTime() - Date.now()) / 1000
  const abs = Math.abs(diff)
  if (abs < 60) return relative.format(Math.round(diff), 'second')
  if (abs < 3600) return relative.format(Math.round(diff / 60), 'minute')
  if (abs < 86400) return relative.format(Math.round(diff / 3600), 'hour')
  return relative.format(Math.round(diff / 86400), 'day')
}
