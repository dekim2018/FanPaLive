import { defineStore } from 'pinia'
import { notificationApi } from '../api/fandom'
import { tr, locale } from '../i18n'

/**
 * 사이트 안 알림. 로그인하면 1회용 티켓을 받아 SSE 로 연결하고, 끊기면 점점 늦게(최대 60초) 다시 연결한다.
 * 브라우저 EventSource 는 Authorization 헤더를 못 보내서 토큰 대신 60초짜리 티켓을 주소에 싣는다.
 */
let source = null
let retryTimer = null
let retryDelay = 2000

/** 영어 화면에서는 서버가 만든 한국어 제목 대신 종류별 영어 문장을 쓴다(본문은 사용자 글이라 그대로). */
export function notificationTitle(n) {
  if (locale !== 'en') return n.title
  if (n.type === 'BROADCAST_LIVE') return tr('응원팀 편파방이 시작됐어요')
  if (n.type === 'PREDICTION_RESULT') return n.title.includes('적중') ? tr('승부 예측 적중!') : tr('승부 예측 결과')
  return tr(n.title)
}

export function notificationBody(n) {
  if (locale !== 'en' || n.type !== 'PREDICTION_RESULT') return n.body
  return tr(n.body)
}

export const useNotificationStore = defineStore('notifications', {
  state: () => ({ unread: 0, items: [], loaded: false, live: false }),
  actions: {
    async load() {
      try {
        const res = await notificationApi.list({ size: 20 })
        this.items = res.content
        this.unread = res.unreadCount
        this.loaded = true
      } catch {
        // 알림은 부가 기능이라 실패해도 화면은 그대로 쓴다.
      }
    },
    async markAllRead() {
      try {
        const res = await notificationApi.markRead([])
        this.unread = res.unreadCount
        this.items = this.items.map((n) => ({ ...n, read: true }))
      } catch {
        // 다음에 다시 시도한다.
      }
    },
    async markRead(id) {
      const item = this.items.find((n) => n.id === id)
      if (!item || item.read) return
      item.read = true
      this.unread = Math.max(0, this.unread - 1)
      try {
        await notificationApi.markRead([id])
      } catch {
        // 화면 표시는 이미 바꿨다. 실패하면 다음 목록 조회 때 맞춰진다.
      }
    },
    async connect(onNew) {
      this.disconnect()
      if (typeof EventSource === 'undefined') return
      let ticket
      try {
        ticket = await notificationApi.ticket()
      } catch {
        this.scheduleRetry(onNew)
        return
      }
      source = new EventSource(`/api/notifications/stream?ticket=${encodeURIComponent(ticket)}`)
      source.addEventListener('open', () => {
        this.live = true
        retryDelay = 2000
      })
      source.addEventListener('unread', (e) => {
        this.unread = JSON.parse(e.data).count
      })
      source.addEventListener('notification', (e) => {
        const data = JSON.parse(e.data)
        this.unread += 1
        this.loaded = false
        onNew?.(data)
      })
      source.addEventListener('error', () => {
        // 티켓은 1회용이라 브라우저 자동 재연결은 실패한다. 닫고 새 티켓으로 다시 연결한다.
        this.live = false
        source?.close()
        source = null
        this.scheduleRetry(onNew)
      })
    },
    scheduleRetry(onNew) {
      clearTimeout(retryTimer)
      retryTimer = setTimeout(() => this.connect(onNew), retryDelay)
      retryDelay = Math.min(retryDelay * 2, 60000)
    },
    disconnect() {
      clearTimeout(retryTimer)
      source?.close()
      source = null
      this.live = false
    },
    reset() {
      this.disconnect()
      this.unread = 0
      this.items = []
      this.loaded = false
    }
  }
})
