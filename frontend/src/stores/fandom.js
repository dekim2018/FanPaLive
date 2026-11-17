import { defineStore } from 'pinia'
import { fandomApi } from '../api/fandom'

/**
 * 여러 화면이 같이 쓰는 팬덤 정보(종목/리그 목록, 내 응원팀, 기능 스위치, 채팅 응원 배지 캐시).
 */
export const useFandomStore = defineStore('fandom', {
  state: () => ({
    config: null,
    sports: [],
    myFans: [],
    myFansLoaded: false,
    badges: {},
    levels: {},
    heartsLeft: null
  }),
  getters: {
    leagues: (s) => s.sports.flatMap((sp) => sp.leagues.map((l) => ({ ...l, sportName: sp.name, kind: sp.kind }))),
    primaryFans: (s) => s.myFans.filter((f) => f.primary),
    feature: (s) => (name) => s.config?.features?.[name] !== false
  },
  actions: {
    async init() {
      const [config, sports] = await Promise.allSettled([fandomApi.config(), fandomApi.sports()])
      if (config.status === 'fulfilled') this.config = config.value
      if (sports.status === 'fulfilled') this.sports = sports.value
    },
    leagueByCode(code) {
      return this.leagues.find((l) => l.code === code)
    },
    async loadMyFans(force = false) {
      if (this.myFansLoaded && !force) return this.myFans
      try {
        this.myFans = await fandomApi.myFans()
      } catch {
        this.myFans = []
      }
      this.myFansLoaded = true
      return this.myFans
    },
    clearMine() {
      this.myFans = []
      this.myFansLoaded = false
      this.heartsLeft = null
    },
    isFan(targetId) {
      return this.myFans.some((f) => f.target.id === targetId)
    },
    /** 채팅 작성자들의 대표 응원팀 배지. 처음 보는 사용자만 모아서 한 번에 조회한다. */
    async fetchBadges(userIds) {
      const missing = [...new Set(userIds)].filter((id) => id && !(id in this.badges))
      if (!missing.length) return
      missing.forEach((id) => { this.badges[id] = [] })
      try {
        const res = await fandomApi.badges(missing.slice(0, 100))
        Object.entries(res).forEach(([id, list]) => { this.badges[id] = list })
      } catch {
        // 배지는 꾸밈 요소라 실패해도 채팅은 그대로 보인다.
      }
    },
    /** 채팅 작성자 팬 등급. 배지와 같은 방식으로 처음 보는 사용자만 묶어서 조회한다. */
    async fetchLevels(userIds) {
      const missing = [...new Set(userIds)].filter((id) => id && !(id in this.levels))
      if (!missing.length) return
      missing.forEach((id) => { this.levels[id] = 'ROOKIE' })
      try {
        const res = await fandomApi.levels(missing.slice(0, 100))
        res.forEach((r) => { this.levels[r.userId] = r.level })
      } catch {
        // 등급도 꾸밈 요소라 실패해도 채팅은 그대로 보인다.
      }
    }
  }
})
