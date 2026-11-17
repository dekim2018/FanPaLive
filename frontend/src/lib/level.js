import { tr } from '../i18n'

/**
 * 팬 등급 표시. 포인트 기준은 서버(FanLevel)와 같다. 루키는 표시하지 않는다(채팅이 지저분해지지 않게).
 * 색은 흰 배경에서 4.5:1 이상인 글자색 + 옅은 배경.
 */
export const LEVELS = {
  ROOKIE: { label: tr('루키'), min: 0, fg: '#4b5563', bg: '#eef0f3' },
  REGULAR: { label: tr('일반'), min: 100, fg: '#1f5fbf', bg: '#e6effc' },
  DEVOTED: { label: tr('진성'), min: 1000, fg: '#4b4bc9', bg: '#eeeefc' },
  LEGEND: { label: tr('영구결번'), min: 5000, fg: '#8a4b00', bg: '#fdf1dc' }
}

export function levelInfo(code) {
  return LEVELS[code] || LEVELS.ROOKIE
}

export const ACHIEVEMENTS = {
  PREDICTOR: { label: tr('예언자'), desc: tr('승부 예측 5번 적중'), icon: '◎' },
  HEART_KEEPER: { label: tr('개근 응원단'), desc: tr('7일 이상 하트 응원'), icon: '♥' },
  SUPPORTER: { label: tr('서포터'), desc: tr('후원 또는 응원금 참여'), icon: '★' }
}
