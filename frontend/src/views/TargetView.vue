<script setup>
import { tr } from '../i18n'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { broadcastApi, clipApi, recordingApi } from '../api/broadcasts'
import { fandomApi, viewerApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import BroadcastCard from '../components/BroadcastCard.vue'
import ClipCard from '../components/ClipCard.vue'
import DonationPanel from '../components/DonationPanel.vue'
import HeartButton from '../components/HeartButton.vue'
import TeamCrest from '../components/TeamCrest.vue'
import { useViewerStats } from '../composables/useViewerStats'
import { latestOnly } from '../lib/latest'
import { q, syncQuery } from '../lib/query'
import { formatCount, formatDate, formatDuration, formatWon } from '../lib/format'
import { KIND_LABEL, PERIOD_LABEL, teamStyle } from '../lib/team'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'

/** 구단/선수 채널: 팬 등록, 하트, 응원금, 편파방, 서포터 순위, 순위 변화 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const fandom = useFandomStore()
const toast = useToastStore()
const targetId = Number(route.params.id)
const { stats, refresh: refreshViewers } = useViewerStats()

const detail = ref(null)
const error = ref('')
const TABS = ['live', 'supporters', 'streamers', 'history', 'vod', 'clips']
const tab = ref(q(route, 'tab', 'live', TABS))
const lives = ref([])
const recordings = ref([])
const supporters = ref([])
const supporterPeriod = ref(q(route, 'period', 'SEASON', ['WEEKLY', 'MONTHLY', 'SEASON']))
// 새로고침이나 링크 공유를 해도 같은 탭과 기간이 열리도록 주소에 남긴다.
watch([tab, supporterPeriod], () => syncQuery(router, route,
  { tab: tab.value, period: supporterPeriod.value }, { tab: 'live', period: 'SEASON' }))
const history = ref([])
const monthlyViewers = ref(0)
const streamers = ref(null)
const clips = ref([])
const busy = ref(false)
const tabLoading = ref(false)
const tabError = ref('')

const t = computed(() => detail.value?.target)
const myFan = computed(() => detail.value?.myFan)
const maxRank = computed(() => Math.max(10, ...history.value.map((h) => h.rank)))

async function load() {
  try {
    detail.value = await fandomApi.target(targetId)
  } catch (e) {
    error.value = e?.response?.status === 404 ? tr('존재하지 않거나 숨겨진 응원 대상입니다.') : errorMessage(e)
  }
}

// 탭을 빠르게 바꾸면 이전 탭의 늦은 응답(실패 포함)이 지금 탭의 로딩/오류 상태를 덮어쓰므로 마지막 요청만 반영한다.
const latestTab = latestOnly()

function fetchTab() {
  if (tab.value === 'live') return broadcastApi.list({ target: targetId, status: 'LIVE', size: 12 })
  if (tab.value === 'vod') return recordingApi.list({ target: targetId, size: 12 })
  if (tab.value === 'supporters') return fandomApi.supporters(targetId, supporterPeriod.value, 20)
  if (tab.value === 'history') return fandomApi.rankHistory(targetId, 8)
  if (tab.value === 'streamers') return viewerApi.streamerRanking(targetId)
  return clipApi.list({ target: targetId, size: 12 })
}

async function loadTab() {
  const current = tab.value
  tabLoading.value = true
  tabError.value = ''
  try {
    const { value, stale } = await latestTab(fetchTab())
    if (stale) return
    if (current === 'live') {
      lives.value = value.content
      refreshViewers(lives.value)
    } else if (current === 'vod') {
      recordings.value = value.content
    } else if (current === 'supporters') {
      supporters.value = value
    } else if (current === 'history') {
      history.value = [...value].reverse()
    } else if (current === 'streamers') {
      streamers.value = value
    } else {
      clips.value = value.content
    }
    tabLoading.value = false
  } catch (e) {
    // 실패를 숨기면 '없습니다' 빈 상태로 보여 오해하게 되므로 오류와 다시 시도를 보여 준다.
    tabError.value = errorMessage(e)
    tabLoading.value = false
  }
}

function changeTab(next) {
  tab.value = next
  loadTab()
}

async function join(primary) {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  busy.value = true
  try {
    await fandomApi.joinFan(targetId, primary)
    toast.success(primary ? tr('{0}을(를) 주 응원팀으로 정했습니다.', tr(t.value.name)) : tr('{0}을(를) 팔로우합니다.', tr(t.value.name)))
    await Promise.all([load(), fandom.loadMyFans(true)])
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    busy.value = false
  }
}

async function leave() {
  if (!confirm(tr('팬 등록을 해제할까요?'))) return
  busy.value = true
  try {
    await fandomApi.leaveFan(targetId)
    toast.success(tr('팬 등록을 해제했습니다.'))
    await Promise.all([load(), fandom.loadMyFans(true)])
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  await load()
  loadTab()
  try {
    monthlyViewers.value = (await viewerApi.target(targetId)).monthlyUniqueViewers
  } catch {
    monthlyViewers.value = 0
  }
})
</script>

<template>
  <div v-if="error" class="empty">{{ error }}</div>
  <div v-else-if="t" :style="teamStyle(t.colorPrimary, t.colorSecondary)">
    <section class="banner">
      <TeamCrest :name="t.name" :short-name="t.shortName" :color="t.colorPrimary" :color2="t.colorSecondary" :size="84" />
      <div class="banner-text">
        <div class="row">
          <span class="chip">{{ tr(t.leagueName) }}</span>
          <span class="chip">{{ KIND_LABEL[t.kind] }}</span>
          <span v-if="!t.official" class="chip">{{ tr('팬 신청으로 등록') }}</span>
        </div>
        <h1 class="name">{{ tr(t.name) }}</h1>
        <div v-if="t.nameEn" class="small en">{{ t.nameEn }}</div>
      </div>
      <div class="banner-actions">
        <template v-if="myFan?.fan">
          <span class="chip mine">{{ myFan.primary ? tr('★ 내 주 응원팀') : tr('팔로우 중') }}</span>
          <button v-if="!myFan.primary" class="btn sm" :disabled="busy" @click="join(true)">{{ tr('주 응원팀으로') }}</button>
          <button class="btn sm ghost" :disabled="busy" @click="leave">{{ tr('해제') }}</button>
        </template>
        <template v-else>
          <button class="btn team" :disabled="busy" @click="join(true)">{{ tr('주 응원팀 등록') }}</button>
          <button class="btn ghost" :disabled="busy" @click="join(false)">{{ tr('팔로우') }}</button>
        </template>
        <HeartButton v-if="fandom.feature('hearts')" :target-id="t.id" @sent="load" />
      </div>
    </section>
    <div v-if="myFan?.primary && myFan.primaryChangeAvailableAt" class="small muted note">
      {{ tr('주 응원팀은 {0} 이후에 바꿀 수 있습니다(30일에 한 번).', formatDate(myFan.primaryChangeAvailableAt)) }}
    </div>

    <section class="stats">
      <div class="stat"><span class="label">{{ tr('이번 주 순위') }}</span><strong>{{ detail.weeklyRank ? tr('{0}위', detail.weeklyRank) : '-' }}</strong></div>
      <div class="stat"><span class="label">{{ tr('주 응원 팬') }}</span><strong class="num">{{ formatCount(t.primaryFans) }}</strong></div>
      <div class="stat"><span class="label">{{ tr('팔로워') }}</span><strong class="num">{{ formatCount(t.followFans) }}</strong></div>
      <div class="stat"><span class="label">{{ tr('누적 하트') }}</span><strong class="num">{{ formatCount(t.hearts) }}</strong></div>
      <div class="stat"><span class="label">{{ tr('응원금 현황') }}</span><strong class="num">{{ formatWon(t.donationTotal) }}</strong></div>
      <div class="stat"><span class="label">{{ tr('이번 달 편파방 시청자') }}</span><strong class="num">{{ tr('약 {0}명', formatCount(monthlyViewers)) }}</strong></div>
    </section>
    <p v-if="t.description" class="muted">{{ t.description }}</p>

    <div class="layout-watch">
      <div>
        <div class="tabs">
          <button type="button" :class="{ active: tab === 'live' }" :aria-pressed="tab === 'live'" @click="changeTab('live')">{{ tr('편파방 LIVE') }}</button>
          <button type="button" :class="{ active: tab === 'supporters' }" :aria-pressed="tab === 'supporters'" @click="changeTab('supporters')">{{ tr('서포터 순위') }}</button>
          <button type="button" :class="{ active: tab === 'streamers' }" :aria-pressed="tab === 'streamers'" @click="changeTab('streamers')">{{ tr('스트리머 순위') }}</button>
          <button type="button" :class="{ active: tab === 'history' }" :aria-pressed="tab === 'history'" @click="changeTab('history')">{{ tr('순위 변화') }}</button>
          <button type="button" :class="{ active: tab === 'vod' }" :aria-pressed="tab === 'vod'" @click="changeTab('vod')">{{ tr('다시보기') }}</button>
          <button type="button" :class="{ active: tab === 'clips' }" :aria-pressed="tab === 'clips'" @click="changeTab('clips')">{{ tr('하이라이트') }}</button>
        </div>

        <div v-if="tabError" class="empty">{{ tabError }} <button type="button" class="btn sm" @click="loadTab">{{ tr('다시 시도') }}</button></div>

        <div v-else-if="tab === 'live'">
          <div v-if="tabLoading && !lives.length" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
          <div v-else-if="!lives.length" class="empty">
            {{ tr('지금 {0} 편파중계가 없습니다.', tr(t.name)) }}
            <div style="margin-top:12px"><RouterLink to="/broadcasts/new" class="btn primary sm">{{ tr('내가 편파중계 하기') }}</RouterLink></div>
          </div>
          <div v-else class="grid" :aria-busy="tabLoading">
            <BroadcastCard v-for="b in lives" :key="b.id" :broadcast="b" :viewers="stats[b.id]" />
          </div>
        </div>

        <div v-else-if="tab === 'supporters'" class="stack">
          <div class="pill-tabs">
            <button v-for="p in ['WEEKLY', 'MONTHLY', 'SEASON']" :key="p" type="button" :class="{ active: supporterPeriod === p }"
                    :aria-pressed="supporterPeriod === p"
                    @click="supporterPeriod = p; loadTab()">{{ PERIOD_LABEL[p] }}</button>
          </div>
          <table v-if="supporters.length" class="table" :aria-busy="tabLoading">
            <thead><tr><th>{{ tr('순위') }}</th><th>{{ tr('팬') }}</th><th>{{ tr('기여 점수') }}</th><th>{{ tr('하트') }}</th><th>{{ tr('후원') }}</th></tr></thead>
            <tbody>
              <tr v-for="(s, i) in supporters" :key="s.userId">
                <td class="num"><strong>{{ i + 1 }}</strong></td>
                <td>{{ s.nickname }}</td>
                <td class="num">{{ formatCount(s.points) }}</td>
                <td class="num">{{ formatCount(s.hearts) }}</td>
                <td class="num">{{ formatWon(s.donationAmount) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else-if="tabLoading" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
          <div v-else class="empty">{{ tr('아직 기여한 팬이 없습니다. 첫 하트를 보내 보세요.') }}</div>
        </div>

        <div v-else-if="tab === 'streamers'" class="stack">
          <p class="small muted" style="margin:0">{{ tr('이번 달 {0} 편파방을 연 스트리머 순위입니다. 점수는 순시청자 1명당 1점, 받은 후원 1,000원당 1점이에요.', tr(t.name)) }}</p>
          <table v-if="streamers?.rows.length" class="table" :aria-busy="tabLoading">
            <thead><tr><th>{{ tr('순위') }}</th><th>{{ tr('스트리머') }}</th><th>{{ tr('순시청자') }}</th><th>{{ tr('받은 후원') }}</th><th>{{ tr('점수') }}</th></tr></thead>
            <tbody>
              <tr v-for="r in streamers.rows" :key="r.streamerId">
                <td class="num"><strong>{{ r.rank }}</strong></td>
                <td>{{ r.nickname }}</td>
                <td class="num">{{ tr('약 {0}명', formatCount(r.uniqueViewers)) }}</td>
                <td class="num">{{ formatWon(r.donationAmount) }}</td>
                <td class="num"><strong>{{ formatCount(r.score) }}</strong></td>
              </tr>
            </tbody>
          </table>
          <div v-else-if="tabLoading" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
          <div v-else class="empty">{{ tr('이번 달에는 아직 {0} 편파방이 없었어요.', tr(t.name)) }}</div>
        </div>

        <div v-else-if="tab === 'clips'">
          <div v-if="tabLoading && !clips.length" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
          <div v-else-if="!clips.length" class="empty">{{ tr('아직 이 팀의 하이라이트가 없습니다.') }}</div>
          <div v-else class="grid dense" :aria-busy="tabLoading">
            <ClipCard v-for="c in clips" :key="c.id" :clip="c" />
          </div>
        </div>

        <div v-else-if="tab === 'history'" class="card">
          <div v-if="tabLoading && !history.length" class="muted small" aria-busy="true">{{ tr('불러오는 중…') }}</div>
          <div v-else-if="!history.length" class="muted small">{{ tr('주간 순위 기록은 매주 월요일 0시에 저장됩니다.') }}</div>
          <div v-else class="history" :aria-busy="tabLoading">
            <div v-for="h in history" :key="h.periodKey" class="hbar">
              <span class="xsmall num">{{ tr('{0}위', h.rank) }}</span>
              <div class="bar" :style="{ height: `${Math.max(8, 100 - ((h.rank - 1) / maxRank) * 100)}%` }"></div>
              <span class="xsmall dim">{{ h.periodKey.replace(/^\d{4}-/, '') }}</span>
            </div>
          </div>
        </div>

        <div v-else-if="tab === 'vod'">
          <div v-if="tabLoading && !recordings.length" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
          <div v-else-if="!recordings.length" class="empty">{{ tr('이 팀의 다시보기가 없습니다.') }}</div>
          <div v-else class="stack" :aria-busy="tabLoading">
            <RouterLink v-for="r in recordings" :key="r.id" :to="`/recordings/${r.id}`" class="card row rec">
              <span class="badge vod">VOD</span>
              <strong class="ellipsis">{{ r.title }}</strong>
              <span class="spacer"></span>
              <span class="small muted">{{ r.ownerNickname }}, {{ formatDate(r.startedAt) }}, {{ formatDuration(r.durationSeconds) }}</span>
            </RouterLink>
          </div>
        </div>
      </div>

      <DonationPanel :target="{ id: t.id, name: t.name }" />
    </div>
  </div>
</template>

<style scoped>
/* 팀 색 배너: 진한 배경색(--team) 위에 연한 글자색(--team-on), 보조 글자는 --team-sub. 값은 teamStyle() 이 대비를 확인해서 넣는다. */
.banner {
  display: flex; align-items: center; gap: 20px; padding: 24px; border-radius: 14px; margin-bottom: 8px; flex-wrap: wrap;
  background: linear-gradient(110deg, var(--team) 55%, color-mix(in srgb, var(--team) 80%, #000) 100%);
  color: var(--team-on);
}
.banner .chip { background: transparent; border: 1px solid var(--team-sub); color: var(--team-sub); }
.banner-text { flex: 1; min-width: 200px; }
.name { font-size: 30px; margin: 6px 0 0; }
.en { color: var(--team-sub); }
.banner-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.banner-actions .btn.team { background: var(--team-on); color: var(--team); }
.banner-actions .btn.team:hover { filter: brightness(.94); }
.banner-actions .btn.ghost { background: transparent; border-color: currentColor; color: inherit; }
.banner-actions .btn.ghost:hover { background: color-mix(in srgb, currentColor 12%, transparent); }
.mine { font-weight: 800; }
.note { margin: 6px 0 0; }
.stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; margin: 16px 0; }
.stat { background: var(--bg-elev); border: 1px solid var(--border); border-radius: 10px; padding: 12px; display: flex; flex-direction: column; gap: 2px; box-shadow: var(--shadow); }
.stat .label { font-size: 12px; color: var(--text-sub); }
.stat strong { font-size: 19px; }
.history { display: flex; align-items: flex-end; gap: 10px; height: 180px; }
.hbar { flex: 1; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; gap: 4px; }
.bar { width: 100%; max-width: 36px; border-radius: 6px 6px 0 0; background: var(--team); }
.rec { gap: 10px; }
/* 줄바꿈되는 flex 안에서도 긴 제목이 카드 밖으로 넘치지 않고 말줄임되게 줄어들 수 있게 한다 */
.rec strong { flex: 1; min-width: 0; }
</style>
