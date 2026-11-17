<script setup>
import { onMounted, ref } from 'vue'
import { adminApi, fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import MatchCard from '../components/MatchCard.vue'
import TeamCrest from '../components/TeamCrest.vue'
import { formatDate } from '../lib/format'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'

/** 운영자 화면: 신청 승인/반려, 경기 등록과 점수 입력, 운영 지표. 권한은 서버가 다시 확인한다. */
const fandom = useFandomStore()
const toast = useToastStore()
const tab = ref('requests')
const status = ref('PENDING')
const requests = ref([])
const ops = ref(null)
const matches = ref([])
const matchForm = ref({ leagueCode: 'KBO', homeTargetId: null, awayTargetId: null, startsAt: '', venue: '' })
const leagueTargets = ref([])

async function loadRequests() {
  try {
    requests.value = (await adminApi.requests({ status: status.value, size: 50 })).content
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

async function approve(r) {
  try {
    await adminApi.approve(r.id)
    toast.success(`${r.name} 승인`)
    loadRequests()
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

async function reject(r) {
  const reason = prompt('반려 사유')
  if (!reason) return
  try {
    await adminApi.reject(r.id, reason)
    toast.success('반려했습니다.')
    loadRequests()
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

async function loadMatches() {
  try {
    matches.value = await fandomApi.matches({ league: matchForm.value.leagueCode })
    leagueTargets.value = await fandomApi.leagueTargets(matchForm.value.leagueCode, 'name')
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

async function createMatch() {
  try {
    await adminApi.createMatch({ ...matchForm.value, startsAt: new Date(matchForm.value.startsAt).toISOString() })
    toast.success('경기를 등록했습니다.')
    loadMatches()
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

async function updateMatch(m, patch) {
  try {
    await adminApi.updateMatch(m.id, patch)
    loadMatches()
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

async function loadOps() {
  try { ops.value = await adminApi.ops() } catch (e) { toast.error(errorMessage(e)) }
}

function changeTab(next) {
  tab.value = next
  if (next === 'requests') loadRequests()
  if (next === 'matches') loadMatches()
  if (next === 'ops') loadOps()
}

onMounted(loadRequests)
</script>

<template>
  <div>
    <h1>운영 관리</h1>
    <div class="tabs">
      <button :class="{ active: tab === 'requests' }" @click="changeTab('requests')">팀/선수 신청</button>
      <button :class="{ active: tab === 'matches' }" @click="changeTab('matches')">경기 관리</button>
      <button :class="{ active: tab === 'ops' }" @click="changeTab('ops')">운영 지표</button>
    </div>

    <template v-if="tab === 'requests'">
      <div class="pill-tabs">
        <button v-for="s in ['PENDING', 'APPROVED', 'REJECTED']" :key="s" :class="{ active: status === s }" @click="status = s; loadRequests()">
          {{ { PENDING: '심사 중', APPROVED: '승인됨', REJECTED: '반려됨' }[s] }}
        </button>
      </div>
      <div v-if="!requests.length" class="empty">신청이 없습니다.</div>
      <div v-for="r in requests" :key="r.id" class="card row req">
        <TeamCrest :name="r.name" :short-name="r.shortName || r.name" :color="r.colorPrimary || '#3a3d44'" :size="44" />
        <div style="flex:1; min-width:200px">
          <strong>{{ r.name }}</strong> <span class="small muted">{{ r.nameEn }}</span>
          <div class="xsmall muted">{{ r.leagueName }}, {{ r.kind === 'PLAYER' ? '선수' : '구단' }}, {{ r.requesterNickname }}님, 함께 신청 {{ r.voteCount }}명, {{ formatDate(r.createdAt) }}</div>
          <div v-if="r.description" class="small">{{ r.description }}</div>
          <a v-if="r.referenceUrl" :href="r.referenceUrl" target="_blank" rel="noopener noreferrer" class="xsmall link">{{ r.referenceUrl }}</a>
          <div v-if="r.rejectReason" class="xsmall muted">반려 사유: {{ r.rejectReason }}</div>
        </div>
        <template v-if="r.status === 'PENDING'">
          <button class="btn sm primary" @click="approve(r)">승인</button>
          <button class="btn sm ghost" @click="reject(r)">반려</button>
        </template>
        <RouterLink v-else-if="r.targetId" :to="`/targets/${r.targetId}`" class="btn sm">보기</RouterLink>
      </div>
    </template>

    <template v-else-if="tab === 'matches'">
      <form class="card stack" @submit.prevent="createMatch">
        <h3 style="margin:0">경기 등록</h3>
        <div class="row">
          <select name="league-code" aria-label="리그" v-model="matchForm.leagueCode" class="input w" @change="loadMatches">
            <option v-for="l in fandom.leagues" :key="l.code" :value="l.code">{{ l.name }}</option>
          </select>
          <select name="home-target-id" aria-label="홈 팀" v-model="matchForm.homeTargetId" class="input w" required>
            <option :value="null" disabled>홈 팀</option>
            <option v-for="t in leagueTargets" :key="t.id" :value="t.id">{{ t.name }}</option>
          </select>
          <select name="away-target-id" aria-label="원정 팀" v-model="matchForm.awayTargetId" class="input w" required>
            <option :value="null" disabled>원정 팀</option>
            <option v-for="t in leagueTargets" :key="t.id" :value="t.id">{{ t.name }}</option>
          </select>
          <input name="starts-at" autocomplete="off" aria-label="경기 시작 시각" v-model="matchForm.startsAt" class="input w" type="datetime-local" required />
          <input name="venue" autocomplete="off" aria-label="경기장" v-model="matchForm.venue" class="input w" maxlength="40" placeholder="경기장 (예: 잠실)…" />
          <button class="btn primary">등록</button>
        </div>
      </form>
      <div class="grid dense" style="margin-top:16px">
        <div v-for="m in matches" :key="m.id" class="stack">
          <MatchCard :match="m" />
          <div class="row">
            <button v-if="m.status === 'SCHEDULED'" class="btn sm" @click="updateMatch(m, { status: 'LIVE' })">경기 시작</button>
            <template v-if="m.status === 'LIVE'">
              <button class="btn sm" @click="updateMatch(m, { homeScore: m.homeScore + 1 })">홈 +1</button>
              <button class="btn sm" @click="updateMatch(m, { awayScore: m.awayScore + 1 })">원정 +1</button>
              <button class="btn sm" @click="updateMatch(m, { status: 'FINISHED' })">종료</button>
            </template>
            <button v-if="m.status === 'SCHEDULED'" class="btn sm ghost" @click="updateMatch(m, { status: 'CANCELED' })">취소</button>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="tab === 'ops' && ops">
      <div class="ops">
        <div class="card"><span class="small muted">반영된 하트</span><strong class="num">{{ ops.flushedHearts }}</strong></div>
        <div class="card"><span class="small muted">DB 반영 횟수</span><strong class="num">{{ ops.flushCount }}</strong></div>
        <div class="card"><span class="small muted">반영 1번당 하트 수 (쓰기 병합 효과)</span><strong class="num">{{ ops.heartsPerFlush }}</strong></div>
        <div class="card"><span class="small muted">대기 중 하트</span><strong class="num">{{ ops.pendingHearts }}</strong></div>
        <div class="card"><span class="small muted">유실된 하트</span><strong class="num">{{ ops.droppedHearts }}</strong></div>
        <div class="card"><span class="small muted">매치룸 실시간 구독</span><strong class="num">{{ ops.matchSubscribers }}</strong></div>
      </div>
      <button class="btn sm" style="margin-top:12px" @click="loadOps">새로고침</button>
    </template>
  </div>
</template>

<style scoped>
.req { gap: 12px; margin-bottom: 8px; align-items: flex-start; }
.w { width: auto; flex: 1; min-width: 140px; }
.link { color: var(--accent-text); word-break: break-all; }
.ops { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
.ops .card { display: flex; flex-direction: column; gap: 4px; }
.ops strong { font-size: 24px; }
</style>
