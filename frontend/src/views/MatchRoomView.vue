<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { broadcastApi } from '../api/broadcasts'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import BroadcastCard from '../components/BroadcastCard.vue'
import HeartButton from '../components/HeartButton.vue'
import HeartGauge from '../components/HeartGauge.vue'
import TeamCrest from '../components/TeamCrest.vue'
import { useViewerStats } from '../composables/useViewerStats'
import { formatDate } from '../lib/format'
import { readable } from '../lib/team'

/**
 * 매치룸: 한 경기의 양 팀 편파방과 실시간 응원 게이지.
 * 게이지는 서버가 0.5초마다 바뀐 경기만 모아 SSE 로 보낸다(하트마다 보내지 않는다).
 */
const route = useRoute()
const matchId = Number(route.params.id)
const { stats, refresh: refreshViewers } = useViewerStats()
const match = ref(null)
const gauge = ref({ home: 0, away: 0 })
const homeLives = ref([])
const awayLives = ref([])
const error = ref('')
const connected = ref(false)
let source = null
let timer = null
const STATUS = { SCHEDULED: '경기 전', LIVE: '경기 중', FINISHED: '경기 종료', CANCELED: '취소' }

async function load() {
  try {
    match.value = await fandomApi.match(matchId)
    gauge.value = { home: match.value.homeHearts, away: match.value.awayHearts }
  } catch (e) {
    error.value = e?.response?.status === 404 ? '존재하지 않는 경기입니다.' : errorMessage(e)
  }
}

async function loadLives() {
  try {
    const all = (await broadcastApi.list({ match: matchId, status: 'LIVE', size: 30 })).content
    homeLives.value = all.filter((b) => b.side?.targetId === match.value.home.id)
    awayLives.value = all.filter((b) => b.side?.targetId === match.value.away.id)
    refreshViewers(all)
  } catch {
    homeLives.value = []
    awayLives.value = []
  }
}

onMounted(async () => {
  await load()
  if (!match.value) return
  loadLives()
  source = new EventSource(`/api/matches/${matchId}/events`)
  source.addEventListener('open', () => { connected.value = true })
  source.addEventListener('error', () => { connected.value = false })
  source.addEventListener('gauge', (e) => {
    try {
      const g = JSON.parse(e.data)
      gauge.value = { home: g.home, away: g.away }
    } catch { /* 형식 오류 무시 */ }
  })
  timer = setInterval(() => { load(); loadLives() }, 30000)
})
onBeforeUnmount(() => {
  source?.close()
  clearInterval(timer)
})
</script>

<template>
  <div v-if="error" class="empty">{{ error }}</div>
  <div v-else-if="match">
    <section class="board card">
      <div class="row small muted">
        <span>{{ match.league.name }}</span>
        <span>{{ formatDate(match.startsAt) }}</span>
        <span v-if="match.venue">{{ match.venue }}</span>
        <span class="spacer"></span>
        <span class="badge" :class="match.status.toLowerCase()">{{ STATUS[match.status] }}</span>
        <span class="xsmall" :style="{ color: connected ? 'var(--ok)' : 'var(--text-dim)' }">{{ connected ? '● 실시간' : '○ 연결 중' }}</span>
      </div>
      <div class="teams">
        <RouterLink :to="`/targets/${match.home.id}`" class="team">
          <TeamCrest :name="match.home.name" :short-name="match.home.shortName" :color="match.home.colorPrimary" :color2="match.home.colorSecondary" :size="72" />
          <strong>{{ match.home.name }}</strong>
          <span class="xsmall muted">홈</span>
        </RouterLink>
        <div class="score num">
          <template v-if="match.status === 'SCHEDULED'">VS</template>
          <template v-else>{{ match.homeScore }} : {{ match.awayScore }}</template>
        </div>
        <RouterLink :to="`/targets/${match.away.id}`" class="team">
          <TeamCrest :name="match.away.name" :short-name="match.away.shortName" :color="match.away.colorPrimary" :color2="match.away.colorSecondary" :size="72" />
          <strong>{{ match.away.name }}</strong>
          <span class="xsmall muted">원정</span>
        </RouterLink>
      </div>
      <HeartGauge :home="match.home" :away="match.away" :home-hearts="gauge.home" :away-hearts="gauge.away" />
      <div v-if="match.status === 'SCHEDULED' || match.status === 'LIVE'" class="row cheer">
        <HeartButton :target-id="match.home.id" source="MATCH" :match-id="match.id" />
        <span class="spacer"></span>
        <HeartButton :target-id="match.away.id" source="MATCH" :match-id="match.id" />
      </div>
      <div v-else class="small muted center">경기가 끝나 응원 게이지가 확정되었습니다.</div>
    </section>

    <div class="sides">
      <section>
        <div class="section-head"><h2 :style="{ color: readable(match.home.colorPrimary, match.home.colorSecondary) }">{{ match.home.shortName || match.home.name }} 편파방</h2></div>
        <div v-if="!homeLives.length" class="empty small">아직 편파방이 없습니다.</div>
        <div v-else class="stack">
          <BroadcastCard v-for="b in homeLives" :key="b.id" :broadcast="b" :viewers="stats[b.id]" />
        </div>
      </section>
      <section>
        <div class="section-head"><h2 :style="{ color: readable(match.away.colorPrimary, match.away.colorSecondary) }">{{ match.away.shortName || match.away.name }} 편파방</h2></div>
        <div v-if="!awayLives.length" class="empty small">아직 편파방이 없습니다.</div>
        <div v-else class="stack">
          <BroadcastCard v-for="b in awayLives" :key="b.id" :broadcast="b" :viewers="stats[b.id]" />
        </div>
      </section>
    </div>
    <div class="center" style="margin-top:20px">
      <RouterLink to="/broadcasts/new" class="btn primary">이 경기 편파중계 하기</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.board { display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px; }
.teams { display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 12px; }
.team { display: flex; flex-direction: column; align-items: center; gap: 6px; text-align: center; }
.score { font-size: 40px; font-weight: 900; }
.cheer { gap: 12px; }
.sides { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.center { text-align: center; }
@media (max-width: 800px) { .sides { grid-template-columns: 1fr; } .score { font-size: 28px; } }
</style>
