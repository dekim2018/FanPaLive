<script setup>
import { intlLocale, tr } from '../i18n'
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminApi, fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import MatchCard from '../components/MatchCard.vue'
import ImportPanel from '../components/admin/ImportPanel.vue'
import ReportsPanel from '../components/admin/ReportsPanel.vue'
import TeamCrest from '../components/TeamCrest.vue'
import { formatDate } from '../lib/format'
import { q, syncQuery } from '../lib/query'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'

/** 운영자 화면: 신청 승인/반려, 경기 등록과 점수 입력, 운영 지표. 권한은 서버가 다시 확인한다. */
const fandom = useFandomStore()

// 운영 지표는 '1.2만'처럼 줄이지 않고 정확한 값에 천 단위 구분만 넣는다.
function exact(n) {
  return Number(n || 0).toLocaleString(intlLocale, { maximumFractionDigits: 1 })
}
const toast = useToastStore()
const route = useRoute()
const router = useRouter()
// 탭과 신청 상태를 주소에 남겨 새로고침이나 링크 공유 뒤에도 같은 화면이 열리게 한다.
const tab = ref(q(route, 'tab', 'requests', ['requests', 'reports', 'matches', 'import', 'seasons', 'ops']))
const status = ref(q(route, 'status', 'PENDING', ['PENDING', 'APPROVED', 'REJECTED']))
watch([tab, status], () => syncQuery(router, route,
  { tab: tab.value, status: status.value }, { tab: 'requests', status: 'PENDING' }))
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

// 처리 중인 신청. 응답 전에 또 누르면 같은 신청을 두 번 처리하려다 오류 토스트가 뜬다.
const requestBusy = ref(new Set())

function setBusy(busySet, id, on) {
  const next = new Set(busySet.value)
  if (on) next.add(id)
  else next.delete(id)
  busySet.value = next
}

async function approve(r) {
  if (requestBusy.value.has(r.id)) return
  setBusy(requestBusy, r.id, true)
  try {
    await adminApi.approve(r.id)
    toast.success(tr('{0} 승인', tr(r.name)))
    loadRequests()
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    setBusy(requestBusy, r.id, false)
  }
}

async function reject(r) {
  if (requestBusy.value.has(r.id)) return
  const reason = prompt(tr('반려 사유'))
  if (!reason) return
  setBusy(requestBusy, r.id, true)
  try {
    await adminApi.reject(r.id, reason)
    toast.success(tr('반려했습니다.'))
    loadRequests()
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    setBusy(requestBusy, r.id, false)
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

// 리그를 바꾸면 이전 리그의 팀 id 가 남아 다른 리그 팀으로 등록되지 않도록 비운다.
function changeLeague() {
  matchForm.value.homeTargetId = null
  matchForm.value.awayTargetId = null
  loadMatches()
}

// 등록 중 연타하면 같은 경기가 여러 번 등록된다.
const creating = ref(false)

async function createMatch() {
  if (creating.value) return
  creating.value = true
  try {
    await adminApi.createMatch({ ...matchForm.value, startsAt: new Date(matchForm.value.startsAt).toISOString() })
    toast.success(tr('경기를 등록했습니다.'))
    loadMatches()
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    creating.value = false
  }
}

// 요청 중인 경기. 점수 +1 은 화면의 점수에 1을 더한 값을 보내므로, 응답 전에 또 누르면 같은 값이 두 번 가서 1점만 오른다.
const matchBusy = ref(new Set())

async function updateMatch(m, patch) {
  if (matchBusy.value.has(m.id)) return
  matchBusy.value = new Set(matchBusy.value).add(m.id)
  try {
    // 응답의 경기 정보로 바로 바꿔 두어야 목록을 다시 불러오기 전에 눌러도 최신 점수에 더한다.
    Object.assign(m, await adminApi.updateMatch(m.id, patch))
    loadMatches()
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    const next = new Set(matchBusy.value)
    next.delete(m.id)
    matchBusy.value = next
  }
}

// 종료하면 승부 예측이 그 점수로 정산되고 점수를 바꿀 수 없으므로 최종 점수를 한 번 더 확인한다.
function finishMatch(m) {
  if (!confirm(tr('최종 점수 {0}:{1}로 종료할까요? 종료하면 점수를 바꿀 수 없고 승부 예측이 정산됩니다.', m.homeScore, m.awayScore))) return
  updateMatch(m, { status: 'FINISHED' })
}

// 취소한 경기는 되돌릴 수 없으므로 한 번 더 확인한다.
function cancelMatch(m) {
  if (!confirm(tr('이 경기를 취소할까요? 취소하면 되돌릴 수 없습니다.'))) return
  updateMatch(m, { status: 'CANCELED' })
}

const seasonDate = ref('')
const seasonBusy = ref(false)

async function closeSeason() {
  if (!confirm(tr('고른 날짜가 속한 시즌의 최종 순위와 MVP 팬을 명예의 전당에 기록할까요? 이미 기록했다면 최신 값으로 덮어씁니다.'))) return
  seasonBusy.value = true
  try {
    const res = await adminApi.closeSeason(seasonDate.value || undefined)
    toast.success(tr('{0}개 리그의 시즌 기록을 남겼습니다.', res.leagues))
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    seasonBusy.value = false
  }
}

// 지표를 불러오는 동안이나 실패했을 때 탭이 통째로 비어 보이지 않도록 상태를 둔다.
const opsLoading = ref(false)
const opsError = ref('')

async function loadOps() {
  opsLoading.value = true
  opsError.value = ''
  try {
    ops.value = await adminApi.ops()
  } catch (e) {
    opsError.value = errorMessage(e)
  } finally {
    opsLoading.value = false
  }
}

function changeTab(next) {
  tab.value = next
  if (next === 'requests') loadRequests()
  if (next === 'matches') loadMatches()
  if (next === 'ops') loadOps()
}

onMounted(() => changeTab(tab.value))
</script>

<template>
  <div>
    <h1>{{ tr('운영 관리') }}</h1>
    <div class="tabs">
      <button type="button" :class="{ active: tab === 'requests' }" :aria-pressed="tab === 'requests'" @click="changeTab('requests')">{{ tr('팀/선수 신청') }}</button>
      <button type="button" :class="{ active: tab === 'reports' }" :aria-pressed="tab === 'reports'" @click="changeTab('reports')">{{ tr('신고 처리') }}</button>
      <button type="button" :class="{ active: tab === 'matches' }" :aria-pressed="tab === 'matches'" @click="changeTab('matches')">{{ tr('경기 관리') }}</button>
      <button type="button" :class="{ active: tab === 'import' }" :aria-pressed="tab === 'import'" @click="changeTab('import')">{{ tr('일정 가져오기') }}</button>
      <button type="button" :class="{ active: tab === 'seasons' }" :aria-pressed="tab === 'seasons'" @click="changeTab('seasons')">{{ tr('시즌 마감') }}</button>
      <button type="button" :class="{ active: tab === 'ops' }" :aria-pressed="tab === 'ops'" @click="changeTab('ops')">{{ tr('운영 지표') }}</button>
    </div>

    <template v-if="tab === 'requests'">
      <div class="pill-tabs">
        <button v-for="s in ['PENDING', 'APPROVED', 'REJECTED']" :key="s" type="button" :class="{ active: status === s }" :aria-pressed="status === s" @click="status = s; loadRequests()">
          {{ { PENDING: tr('심사 중'), APPROVED: tr('승인됨'), REJECTED: tr('반려됨') }[s] }}
        </button>
      </div>
      <div v-if="!requests.length" class="empty">{{ tr('신청이 없습니다.') }}</div>
      <div v-for="r in requests" :key="r.id" class="card row req">
        <TeamCrest :name="r.name" :short-name="r.shortName || r.name" :color="r.colorPrimary || '#3a3d44'" :color2="r.colorSecondary" :size="44" />
        <div style="flex:1; min-width:200px">
          <strong>{{ tr(r.name) }}</strong> <span class="small muted">{{ r.nameEn }}</span>
          <div class="xsmall muted">{{ tr('{0}, {1}, {2}님, 함께 신청 {3}명, {4}', tr(r.leagueName), r.kind === 'PLAYER' ? tr('선수') : tr('구단'), r.requesterNickname, r.voteCount, formatDate(r.createdAt)) }}</div>
          <div v-if="r.description" class="small">{{ r.description }}</div>
          <a v-if="r.referenceUrl" :href="r.referenceUrl" target="_blank" rel="noopener noreferrer" class="xsmall link">{{ r.referenceUrl }}</a>
          <div v-if="r.rejectReason" class="xsmall muted">{{ tr('반려 사유: {0}', r.rejectReason) }}</div>
        </div>
        <template v-if="r.status === 'PENDING'">
          <button type="button" class="btn sm primary" :disabled="requestBusy.has(r.id)" @click="approve(r)">{{ tr('승인') }}</button>
          <button type="button" class="btn sm ghost" :disabled="requestBusy.has(r.id)" @click="reject(r)">{{ tr('반려') }}</button>
        </template>
        <RouterLink v-else-if="r.targetId" :to="`/targets/${r.targetId}`" class="btn sm">{{ tr('보기') }}</RouterLink>
      </div>
    </template>

    <template v-else-if="tab === 'matches'">
      <form class="card stack" @submit.prevent="createMatch">
        <h2 style="margin:0; font-size:16px">{{ tr('경기 등록') }}</h2>
        <div class="row">
          <select name="league-code" :aria-label="tr('리그')" v-model="matchForm.leagueCode" class="input w" @change="changeLeague">
            <option v-for="l in fandom.leagues" :key="l.code" :value="l.code">{{ tr(l.name) }}</option>
          </select>
          <select name="home-target-id" :aria-label="tr('홈 팀')" v-model="matchForm.homeTargetId" class="input w" required>
            <option :value="null" disabled>{{ tr('홈 팀') }}</option>
            <option v-for="t in leagueTargets" :key="t.id" :value="t.id">{{ tr(t.name) }}</option>
          </select>
          <select name="away-target-id" :aria-label="tr('원정 팀')" v-model="matchForm.awayTargetId" class="input w" required>
            <option :value="null" disabled>{{ tr('원정 팀') }}</option>
            <option v-for="t in leagueTargets" :key="t.id" :value="t.id">{{ tr(t.name) }}</option>
          </select>
          <input name="starts-at" autocomplete="off" :aria-label="tr('경기 시작 시각')" v-model="matchForm.startsAt" class="input w" type="datetime-local" required />
          <input name="venue" autocomplete="off" :aria-label="tr('경기장')" v-model="matchForm.venue" class="input w" maxlength="40" :placeholder="tr('경기장 (예: 잠실)…')" />
          <button class="btn primary" :disabled="creating">{{ creating ? tr('등록하는 중…') : tr('등록') }}</button>
        </div>
      </form>
      <div class="grid dense" style="margin-top:16px">
        <div v-for="m in matches" :key="m.id" class="stack">
          <MatchCard :match="m" />
          <div class="row">
            <button v-if="m.status === 'SCHEDULED'" class="btn sm" :disabled="matchBusy.has(m.id)" @click="updateMatch(m, { status: 'LIVE' })">{{ tr('경기 시작') }}</button>
            <template v-if="m.status === 'LIVE'">
              <button class="btn sm" :disabled="matchBusy.has(m.id)" @click="updateMatch(m, { homeScore: m.homeScore + 1 })">{{ tr('홈 +1') }}</button>
              <button class="btn sm" :disabled="matchBusy.has(m.id)" @click="updateMatch(m, { awayScore: m.awayScore + 1 })">{{ tr('원정 +1') }}</button>
              <button class="btn sm" :disabled="matchBusy.has(m.id)" @click="finishMatch(m)">{{ tr('종료') }}</button>
            </template>
            <button v-if="m.status === 'SCHEDULED'" class="btn sm ghost" :disabled="matchBusy.has(m.id)" @click="cancelMatch(m)">{{ tr('취소') }}</button>
          </div>
        </div>
      </div>
    </template>

    <ReportsPanel v-else-if="tab === 'reports'" />
    <ImportPanel v-else-if="tab === 'import'" />

    <form v-else-if="tab === 'seasons'" class="card stack" style="max-width:560px" @submit.prevent="closeSeason">
      <h2 style="margin:0; font-size:16px">{{ tr('시즌 마감') }}</h2>
      <p class="small muted" style="margin:0">{{ tr('새 시즌 첫날 0시 20분에 지난 시즌을 자동으로 마감합니다. 개막일이 바뀌었거나 다시 기록해야 할 때 여기서 직접 실행하세요.') }}</p>
      <label class="field">{{ tr('기준 날짜 (비우면 오늘)') }}
        <input v-model="seasonDate" name="season-date" type="date" class="input" />
      </label>
      <div class="row">
        <button class="btn primary" :disabled="seasonBusy">{{ seasonBusy ? tr('기록하는 중…') : tr('시즌 기록 남기기') }}</button>
        <RouterLink to="/hall-of-fame" class="btn ghost">{{ tr('명예의 전당 보기') }}</RouterLink>
      </div>
    </form>

    <template v-else-if="tab === 'ops'">
      <div v-if="!ops && opsLoading" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
      <div v-else-if="!ops && opsError" class="empty">{{ opsError }} <button type="button" class="btn sm" @click="loadOps">{{ tr('다시 시도') }}</button></div>
      <template v-else-if="ops">
        <div v-if="opsError" class="error-text" role="alert">{{ opsError }}</div>
        <div class="ops" :aria-busy="opsLoading">
          <div class="card"><span class="small muted">{{ tr('반영된 하트') }}</span><strong class="num">{{ exact(ops.flushedHearts) }}</strong></div>
          <div class="card"><span class="small muted">{{ tr('DB 반영 횟수') }}</span><strong class="num">{{ exact(ops.flushCount) }}</strong></div>
          <div class="card"><span class="small muted">{{ tr('반영 1번당 하트 수 (쓰기 병합 효과)') }}</span><strong class="num">{{ exact(ops.heartsPerFlush) }}</strong></div>
          <div class="card"><span class="small muted">{{ tr('대기 중 하트') }}</span><strong class="num">{{ exact(ops.pendingHearts) }}</strong></div>
          <div class="card"><span class="small muted">{{ tr('유실된 하트') }}</span><strong class="num">{{ exact(ops.droppedHearts) }}</strong></div>
          <div class="card"><span class="small muted">{{ tr('매치룸 실시간 구독') }}</span><strong class="num">{{ exact(ops.matchSubscribers) }}</strong></div>
        </div>
        <button type="button" class="btn sm" style="margin-top:12px" :disabled="opsLoading" @click="loadOps">{{ opsLoading ? tr('불러오는 중…') : tr('새로고침') }}</button>
      </template>
    </template>
  </div>
</template>

<style scoped>
.req { gap: 12px; margin-bottom: 8px; align-items: flex-start; }
.w { width: auto; flex: 1; min-width: 140px; }
.link { color: var(--text); font-weight: 700; text-decoration: underline; text-underline-offset: 3px; word-break: break-all; }
.ops { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
.ops .card { display: flex; flex-direction: column; gap: 4px; }
.ops strong { font-size: 24px; }
</style>
