/**
 * 화면 다국어(한국어, 영어).
 *
 * 한국어 문장을 그대로 키로 쓴다(gettext 방식). 한국어 화면은 사전 없이 원문이 나오고,
 * 영어 화면은 en.js 에서 같은 문장을 찾아 바꾼다. 사전에 없는 문장은 원문 그대로 보여 준다.
 * 사용자 이름이나 방송 제목처럼 사전에 없는 값을 넣어도 안전하다.
 *
 * 언어는 시작할 때 한 번 정한다(저장한 선택, 없으면 브라우저 언어). 바꾸면 새로고침해서
 * 모듈 상수까지 모두 새 언어로 다시 만든다. 화면마다 반응형 처리를 넣지 않아도 되는 대신 바꿀 때 한 번 깜빡인다.
 */
import en, { patterns } from './en'

export const LOCALES = [
  { code: 'ko', label: '한국어' },
  { code: 'en', label: 'English' }
]
const KEY = 'fanpa.locale'

function detect() {
  try {
    const fromUrl = new URLSearchParams(window.location.search).get('lang')
    if (fromUrl === 'ko' || fromUrl === 'en') return fromUrl
  } catch {
    // 주소를 읽지 못하면 다음 단계로 넘어간다.
  }
  try {
    const saved = localStorage.getItem(KEY)
    if (saved === 'ko' || saved === 'en') return saved
  } catch {
    // 저장소를 못 쓰는 환경(사생활 보호 모드 등)은 브라우저 언어로 정한다.
  }
  const langs = navigator.languages?.length ? navigator.languages : [navigator.language || 'ko']
  for (const lang of langs) {
    const base = String(lang).toLowerCase().slice(0, 2)
    if (base === 'ko') return 'ko'
    if (base === 'en') return 'en'
  }
  return 'ko'
}

export const locale = detect()
const dict = locale === 'en' ? en : null

if (typeof document !== 'undefined') {
  document.documentElement.lang = locale
}

/**
 * 번역. 자리표시자 {0}, {1} 은 뒤에 오는 값으로 채운다.
 * 예) tr('{0}명 시청 중', 12) -> 한국어 "12명 시청 중", 영어 "12 watching"
 */
export function tr(text, ...args) {
  if (text == null) return ''
  const source = String(text)
  let template = dict ? dict[source] : source
  if (template === undefined) template = fromPattern(source)
  if (!args.length) return template
  return template.replace(/\{(\d+)\}/g, (m, i) => (args[i] ?? ''))
}

/** 서버가 값을 이어 붙여 만든 문구(금액, 일수 등)를 정규식으로 맞춰 바꾼다. 못 맞추면 원문 */
function fromPattern(source) {
  for (const [rx, replacement] of patterns) {
    if (rx.test(source)) return source.replace(rx, replacement)
  }
  return source
}

export function setLocale(next) {
  if (next === locale) return
  try {
    localStorage.setItem(KEY, next)
  } catch {
    // 저장소를 못 쓰면 주소에 언어를 붙여서 이번 방문 동안 유지한다.
    const url = new URL(window.location.href)
    url.searchParams.set('lang', next)
    window.location.assign(url.toString())
    return
  }
  const url = new URL(window.location.href)
  url.searchParams.delete('lang')
  window.location.assign(url.toString())
}

/** 날짜, 숫자 표기에 쓰는 BCP 47 태그 */
export const intlLocale = locale === 'en' ? 'en-US' : 'ko-KR'
