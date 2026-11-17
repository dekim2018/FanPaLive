<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { broadcastApi, recordingApi } from '../api/broadcasts'
import { fandomApi, viewerApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import BroadcastCard from '../components/BroadcastCard.vue'
import DonationPanel from '../components/DonationPanel.vue'
import HeartButton from '../components/HeartButton.vue'
import TeamCrest from '../components/TeamCrest.vue'
import { useViewerStats } from '../composables/useViewerStats'
import { formatCount, formatDate, formatDuration, formatWon } from '../lib/format'
import { KIND_LABEL, PERIOD_LABEL, teamStyle } from '../lib/team'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'

/** 구단/선수 채널: 팬 등록, 하트, 응원기금, 편파방, 서포터 순위, 순위 변화 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const fandom = useFandomStore()
const toast = useToastStore()
const targetId = Number(route.params.id)
const { stats, refresh: refreshViewers } = useViewerStats()

const detail = ref(null)
const error = ref('')
const tab = ref('live')
const lives = ref([])
const recordings = ref([])
const supporters = ref([])
const supporterPeriod = ref('SEASON')
const history = ref([])
const monthlyViewers = ref(0)
const busy = ref(false)

const t = computed(() => detail.value?.target)
const myFan = computed(() => detail.value?.myFan)
const maxRank = computed(() => Math.max(10, ...history.value.map((h) => h.rank)))

async function load() {
  try {
    detail.value = await fandomApi.target(targetId)
  } catch (e) {
    error.value = e?.response?.status === 404 ? '존재하지 않거나 숨겨진 응원 대상입니다.' : errorMessage(e)
  }
}

async function loadTab() {
  try {
    if (tab.value === 'live') {
      lives.value = (await broadcastApi.list({ target: targetId, status: 'LIVE', size: 12 })).content
      refreshViewers(lives.value)
    } else if (tab.value === 'vod') {
      recordings.value = (await recordingApi.list({ target: targetId, size: 12 })).content
    } else if (tab.value === 'supporters') {
      supporters.value = await fandomApi.supporters(targetId, supporterPeriod.value, 20)
    } else if (tab.value === 'history') {
      history.value = [...(await fandomApi.rankHistory(targetId, 8))].reverse()
    }
  } catch {
    // 탭 내용은 비어 있는 상태로 둔다.
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
    toast.success(primary ? `${t.value.name}을(를) 주 응원팀으로 정했습니다.` : `${t.value.name}을(를) 팔로우합니다.`)
    await Promise.all([load(), fandom.loadMyFans(true)])
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    busy.value = false
  }
}

async function leave() {
  if (!confirm('팬 등록을 해제할까요?')) return
  busy.value = true
  try {
    await fandomApi.leaveFan(targetId)
    toast.success('팬 등록을 해제했습니다.')
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
          <span class="chip">{{ t.leagueName }}</span>
          <span class="chip">{{ KIND_LABEL[t.kind] }}</span>
          <span v-if="!t.official" class="chip">팬 신청으로 등록</span>
        </div>
        <h1 class="name">{{ t.name }}</h1>
        <div v-if="t.nameEn" class="small en">{{ t.nameEn }}</div>
      </div>
      <div class="banner-actions">
        <template v-if="myFan?.fan">
          <span class="chip mine">{{ myFan.primary ? '★ 내 주 응원팀' : '팔로우 중' }}</span>
          <button v-if="!myFan.primary" class="btn sm" :disabled="busy" @click="join(true)">주 응원팀으로</button>
          <button class="btn sm ghost" :disabled="busy" @click="leave">해제</button>
        </template>
        <template v-else>
          <button class="btn team" :disabled="busy" @click="join(true)">주 응원팀 등록</button>
          <button class="btn ghost" :disabled="busy" @click="join(false)">팔로우</button>
        </template>
        <HeartButton v-if="fandom.feature('hearts')" :target-id="t.id" @sent="load" />
      </div>
    </section>
    <div v-if="myFan?.primary && myFan.primaryChangeAvailableAt" class="small muted note">
      주 응원팀은 {{ formatDate(myFan.primaryChangeAvailableAt) }} 이후에 바꿀 수 있습니다(30일에 한 번).
    </div>

    <section class="stats">
      <div class="stat"><span class="label">이번 주 순위</span><strong>{{ detail.weeklyRank ? detail.weeklyRank + '위' : '-' }}</strong></div>
      <div class="stat"><span class="label">주 응원 팬</span><strong class="num">{{ formatCount(t.primaryFans) }}</strong></div>
      <div class="stat"><span class="label">팔로워</span><strong class="num">{{ formatCount(t.followFans) }}</strong></div>
      <div class="stat"><span class="label">누적 하트</span><strong class="num">{{ formatCount(t.hearts) }}</strong></div>
      <div class="stat"><span class="label">응원기금 현황</span><strong class="num">{{ formatWon(t.donationTotal) }}</strong></div>
      <div class="stat"><span class="label">이번 달 편파방 시청자</span><strong class="num">약 {{ formatCount(monthlyViewers) }}명</strong></div>
    </section>
    <p v-if="t.description" class="muted">{{ t.description }}</p>

    <div class="layout-watch">
      <div>
        <div class="tabs">
          <button :class="{ active: tab === 'live' }" @click="changeTab('live')">편파방 LIVE</button>
          <button :class="{ active: tab === 'supporters' }" @click="changeTab('supporters')">서포터 순위</button>
          <button :class="{ active: tab === 'history' }" @click="changeTab('history')">순위 변화</button>
          <button :class="{ active: tab === 'vod' }" @click="changeTab('vod')">다시보기</button>
        </div>

        <div v-if="tab === 'live'">
          <div v-if="!lives.length" class="empty">
            지금 {{ t.name }} 편파중계가 없습니다.
            <div style="margin-top:12px"><RouterLink to="/broadcasts/new" class="btn primary sm">내가 편파중계 하기</RouterLink></div>
          </div>
          <div v-else class="grid">
            <BroadcastCard v-for="b in lives" :key="b.id" :broadcast="b" :viewers="stats[b.id]" />
          </div>
        </div>

        <div v-else-if="tab === 'supporters'" class="stack">
          <div class="pill-tabs">
            <button v-for="p in ['WEEKLY', 'MONTHLY', 'SEASON']" :key="p" :class="{ active: supporterPeriod === p }"
                    @click="supporterPeriod = p; loadTab()">{{ PERIOD_LABEL[p] }}</button>
          </div>
          <table v-if="supporters.length" class="table">
            <thead><tr><th>순위</th><th>팬</th><th>기여 점수</th><th>하트</th><th>후원</th></tr></thead>
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
          <div v-else class="empty">아직 기여한 팬이 없습니다. 첫 하트를 보내 보세요.</div>
        </div>

        <div v-else-if="tab === 'history'" class="card">
          <div v-if="!history.length" class="muted small">주간 순위 기록은 매주 월요일 0시에 저장됩니다.</div>
          <div v-else class="history">
            <div v-for="h in history" :key="h.periodKey" class="hbar">
              <span class="xsmall num">{{ h.rank }}위</span>
              <div class="bar" :style="{ height: `${Math.max(8, 100 - ((h.rank - 1) / maxRank) * 100)}%` }"></div>
              <span class="xsmall dim">{{ h.periodKey.replace(/^\d{4}-/, '') }}</span>
            </div>
          </div>
        </div>

        <div v-else-if="tab === 'vod'">
          <div v-if="!recordings.length" class="empty">이 팀의 다시보기가 없습니다.</div>
          <div v-else class="stack">
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
</style>
