<script setup>
import { tr } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import { fandomApi } from '../api/fandom'
import { fundApi } from '../api/interactions'
import LevelBadge from '../components/LevelBadge.vue'
import TeamCrest from '../components/TeamCrest.vue'
import { ACHIEVEMENTS, LEVELS, levelInfo } from '../lib/level'
import { formatDate, formatWon } from '../lib/format'
import { latestOnly } from '../lib/latest'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'

const auth = useAuthStore()
const fandom = useFandomStore()
const tab = ref('given')
const given = ref([])
const received = ref([])
const donationsLoading = ref(true)
const donationsError = ref(false)
// 탭을 빠르게 바꾸면 늦게 온 이전 탭 응답이 로딩/오류 상태를 덮어쓰지 않게 한다.
const latestDonations = latestOnly()
const hearts = ref(null)
const profile = ref(null)
const predictions = ref([])

/** 지금 등급 안에서 다음 등급까지 얼마나 왔는지(0~100) */
const progress = computed(() => {
  const p = profile.value
  if (!p || !p.nextLevel) return 100
  const from = levelInfo(p.level).min
  const to = LEVELS[p.nextLevel].min
  return Math.round(((p.points - from) * 100) / (to - from))
})
const PICK = { HOME: tr('홈 승'), AWAY: tr('원정 승') }
const RESULT = { HIT: tr('적중'), MISS: tr('빗나감'), VOID: tr('무효') }

async function loadDonations() {
  const which = tab.value
  donationsLoading.value = true
  donationsError.value = false
  try {
    const { value, stale } = await latestDonations(which === 'given' ? fundApi.mine({ size: 30 }) : fundApi.received({ size: 30 }))
    if (stale) return
    if (which === 'given') given.value = value.content
    else received.value = value.content
  } catch {
    donationsError.value = true
  }
  donationsLoading.value = false
}

function changeTab(t) {
  tab.value = t
  loadDonations()
}

onMounted(async () => {
  fandom.loadMyFans(true)
  loadDonations()
  try { hearts.value = await fandomApi.myHearts() } catch { hearts.value = null }
  try { profile.value = await fandomApi.myProfile() } catch { profile.value = null }
  try { predictions.value = await fandomApi.myPredictions(10) } catch { predictions.value = [] }
})
</script>

<template>
  <div class="stack">
    <div class="row">
      <span class="avatar">{{ auth.user.nickname.slice(0, 1) }}</span>
      <div>
        <h1 style="margin:0">{{ auth.user.nickname }}</h1>
        <span class="small muted">@{{ auth.user.username }}<span v-if="auth.user.role === 'ADMIN'">, {{ tr('운영자') }}</span></span>
      </div>
      <span class="spacer"></span>
      <div v-if="hearts" class="card hearts">
        <span class="small muted">{{ tr('오늘 남은 하트') }}</span>
        <strong class="num">{{ hearts.remainingToday }} / {{ hearts.dailyMax }}</strong>
        <span class="xsmall dim">{{ tr('한 팀에 하루 {0}개까지', hearts.perTargetMax) }}</span>
      </div>
    </div>

    <section v-if="profile" class="card stack level-card" aria-labelledby="level-title">
      <div class="row">
        <h2 id="level-title" style="margin:0">{{ tr('팬 등급') }}</h2>
        <LevelBadge :level="profile.level" />
        <span class="spacer"></span>
        <strong class="num points">{{ tr('{0}점', profile.points.toLocaleString()) }}</strong>
      </div>
      <div class="progress" role="progressbar" :aria-valuenow="progress" aria-valuemin="0" aria-valuemax="100"
           :aria-label="tr('다음 등급까지 진행률')">
        <div :style="{ width: progress + '%' }"></div>
      </div>
      <div class="small muted">
        <template v-if="profile.nextLevel">{{ tr('{0}까지 {1}점 남았어요.', levelInfo(profile.nextLevel).label, profile.pointsToNext.toLocaleString()) }}</template>
        <template v-else>{{ tr('최고 등급이에요.') }}</template>
        {{ tr('하트 1개 2점, 후원 100원당 1점, 승부 예측 적중 30점이 쌓여요. 포인트는 돈으로 바꿀 수 없어요.') }}
      </div>
      <div class="row stats-row">
        <span class="mini"><span class="xsmall muted">{{ tr('하트 응원한 날') }}</span><strong class="num">{{ tr('{0}일', profile.heartDays) }}</strong></span>
        <span class="mini"><span class="xsmall muted">{{ tr('후원 횟수') }}</span><strong class="num">{{ tr('{0}번', profile.donationCount) }}</strong></span>
        <span class="mini"><span class="xsmall muted">{{ tr('승부 예측') }}</span><strong class="num">{{ tr('{0}번 중 {1}번 적중', profile.predictionTotal, profile.predictionHits) }}</strong></span>
      </div>
      <div>
        <h3 class="sub">{{ tr('업적 배지') }}</h3>
        <ul class="achievements">
          <li v-for="(a, code) in ACHIEVEMENTS" :key="code" :class="{ got: profile.achievements.some((x) => x.code === code) }">
            <span class="icon" aria-hidden="true">{{ a.icon }}</span>
            <span><strong>{{ a.label }}</strong><span class="xsmall muted">{{ a.desc }}</span></span>
            <span class="sr-only">{{ profile.achievements.some((x) => x.code === code) ? tr('받음') : tr('아직 없음') }}</span>
          </li>
        </ul>
      </div>
      <div v-if="predictions.length">
        <h3 class="sub">{{ tr('최근 승부 예측') }}</h3>
        <div class="row small" v-for="p in predictions" :key="p.matchId">
          <RouterLink :to="`/matches/${p.matchId}`" class="link">{{ tr('경기 #{0}', p.matchId) }}</RouterLink>
          <span>{{ PICK[p.pick] }}</span>
          <span class="spacer"></span>
          <span :class="['res', (p.result || 'wait').toLowerCase()]">{{ p.result ? RESULT[p.result] : tr('결과 대기') }}</span>
        </div>
      </div>
    </section>

    <section class="card stack">
      <div class="row">
        <h2 style="margin:0">{{ tr('내 응원팀') }}</h2>
        <span class="spacer"></span>
        <RouterLink to="/onboarding" class="btn sm">{{ tr('응원팀 설정') }}</RouterLink>
      </div>
      <div v-if="!fandom.myFans.length" class="muted small">{{ tr('아직 응원팀이 없습니다.') }}</div>
      <div class="grid dense">
        <RouterLink v-for="f in fandom.myFans" :key="f.target.id" :to="`/targets/${f.target.id}`" class="row nowrap fan">
          <TeamCrest :name="f.target.name" :short-name="f.target.shortName" :color="f.target.colorPrimary" :color2="f.target.colorSecondary" :size="40" />
          <div style="min-width:0">
            <strong class="ellipsis" style="display:block">{{ tr(f.target.name) }}</strong>
            <span class="xsmall muted">{{ tr(f.leagueName) }}, {{ f.primary ? tr('★ 주 응원팀') : tr('팔로우') }}</span>
          </div>
        </RouterLink>
      </div>
    </section>

    <section class="card">
      <div class="tabs">
        <button type="button" :class="{ active: tab === 'given' }" :aria-pressed="tab === 'given'" @click="changeTab('given')">{{ tr('내가 한 후원') }}</button>
        <button type="button" :class="{ active: tab === 'received' }" :aria-pressed="tab === 'received'" @click="changeTab('received')">{{ tr('받은 후원') }}</button>
      </div>
      <table class="table" :aria-busy="donationsLoading">
        <thead><tr><th>{{ tr('일시') }}</th><th>{{ tr('대상') }}</th><th>{{ tr('메시지') }}</th><th>{{ tr('금액') }}</th></tr></thead>
        <tbody>
          <tr v-for="d in (tab === 'given' ? given : received)" :key="d.id">
            <td class="small">{{ formatDate(d.approvedAt) }}</td>
            <td>
              <template v-if="tab === 'received'">{{ d.donorNickname }}</template>
              <template v-else-if="d.targetType === 'FAN_TARGET'"><span class="chip">{{ tr('응원금') }}</span> {{ tr(d.targetName) }}</template>
              <template v-else>{{ d.streamerNickname }}<span v-if="d.targetName" class="xsmall muted"> ({{ tr(d.targetName) }})</span></template>
            </td>
            <td class="small muted">{{ d.message }}</td>
            <td class="num">{{ formatWon(d.amount) }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="donationsError" class="empty">{{ tr('후원 내역을 불러오지 못했습니다.') }} <button type="button" class="btn sm" @click="loadDonations">{{ tr('다시 시도') }}</button></div>
      <div v-else-if="donationsLoading && !(tab === 'given' ? given : received).length" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
      <div v-else-if="!(tab === 'given' ? given : received).length" class="muted small" style="padding:12px 0">{{ tr('내역이 없습니다.') }}</div>
    </section>
  </div>
</template>

<style scoped>
.avatar { width: 56px; height: 56px; border-radius: 50%; background: var(--accent-soft); color: var(--accent-text); font-weight: 900; font-size: 22px; display: inline-flex; align-items: center; justify-content: center; }
.hearts { display: flex; flex-direction: column; gap: 2px; padding: 10px 14px; }
.hearts strong { color: var(--heart); font-size: 20px; }
.fan { gap: 10px; padding: 8px; border-radius: 10px; background: var(--bg); }
.points { font-size: 20px; }
.progress { height: 10px; border-radius: 999px; background: var(--bg-elev-2); overflow: hidden; }
.progress > div { height: 100%; background: var(--accent); border-radius: 999px; transition: width .4s ease; }
.stats-row { gap: 10px; }
.mini { display: flex; flex-direction: column; gap: 2px; padding: 8px 12px; border-radius: 8px; background: var(--bg); }
.sub { font-size: 15px; margin: 4px 0 8px; }
.achievements { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 8px; }
.achievements li { display: flex; gap: 10px; align-items: center; padding: 10px; border-radius: 10px; border: 1px dashed var(--border-strong); color: var(--text-sub); }
.achievements li > span:nth-child(2) { display: flex; flex-direction: column; gap: 2px; }
.achievements li.got { border-style: solid; border-color: var(--accent-border); background: var(--accent-soft); color: var(--text); }
.icon { width: 32px; height: 32px; border-radius: 50%; display: grid; place-items: center; background: var(--bg-elev-2); font-weight: 900; }
.got .icon { background: var(--accent); color: var(--on-accent); }
.link { color: var(--text); font-weight: 600; text-decoration: underline; text-underline-offset: 3px; }
.res { font-weight: 700; }
.res.hit { color: var(--ok); }
.res.miss, .res.void, .res.wait { color: var(--text-sub); }
</style>
