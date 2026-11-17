<script setup>
import { onMounted, ref } from 'vue'
import { fandomApi } from '../api/fandom'
import { fundApi } from '../api/interactions'
import TeamCrest from '../components/TeamCrest.vue'
import { formatDate, formatWon } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'

const auth = useAuthStore()
const fandom = useFandomStore()
const tab = ref('given')
const given = ref([])
const received = ref([])
const hearts = ref(null)

async function loadDonations() {
  try {
    if (tab.value === 'given') given.value = (await fundApi.mine({ size: 30 })).content
    else received.value = (await fundApi.received({ size: 30 })).content
  } catch {
    // 빈 목록으로 둔다.
  }
}

onMounted(async () => {
  fandom.loadMyFans(true)
  loadDonations()
  try { hearts.value = await fandomApi.myHearts() } catch { hearts.value = null }
})
</script>

<template>
  <div class="stack">
    <div class="row">
      <span class="avatar">{{ auth.user.nickname.slice(0, 1) }}</span>
      <div>
        <h1 style="margin:0">{{ auth.user.nickname }}</h1>
        <span class="small muted">@{{ auth.user.username }}<span v-if="auth.user.role === 'ADMIN'">, 운영자</span></span>
      </div>
      <span class="spacer"></span>
      <div v-if="hearts" class="card hearts">
        <span class="small muted">오늘 남은 하트</span>
        <strong class="num">{{ hearts.remainingToday }} / {{ hearts.dailyMax }}</strong>
        <span class="xsmall dim">한 팀에 하루 {{ hearts.perTargetMax }}개까지</span>
      </div>
    </div>

    <section class="card stack">
      <div class="row">
        <h2 style="margin:0">내 응원팀</h2>
        <span class="spacer"></span>
        <RouterLink to="/onboarding" class="btn sm">응원팀 설정</RouterLink>
      </div>
      <div v-if="!fandom.myFans.length" class="muted small">아직 응원팀이 없습니다.</div>
      <div class="grid dense">
        <RouterLink v-for="f in fandom.myFans" :key="f.target.id" :to="`/targets/${f.target.id}`" class="row nowrap fan">
          <TeamCrest :name="f.target.name" :short-name="f.target.shortName" :color="f.target.colorPrimary" :size="40" />
          <div style="min-width:0">
            <strong class="ellipsis" style="display:block">{{ f.target.name }}</strong>
            <span class="xsmall muted">{{ f.leagueName }}, {{ f.primary ? '★ 주 응원팀' : '팔로우' }}</span>
          </div>
        </RouterLink>
      </div>
    </section>

    <section class="card">
      <div class="tabs">
        <button :class="{ active: tab === 'given' }" @click="tab = 'given'; loadDonations()">내가 한 후원</button>
        <button :class="{ active: tab === 'received' }" @click="tab = 'received'; loadDonations()">받은 후원</button>
      </div>
      <table class="table">
        <thead><tr><th>일시</th><th>대상</th><th>메시지</th><th style="text-align:right">금액</th></tr></thead>
        <tbody>
          <tr v-for="d in (tab === 'given' ? given : received)" :key="d.id">
            <td class="small">{{ formatDate(d.approvedAt) }}</td>
            <td>
              <template v-if="tab === 'received'">{{ d.donorNickname }}</template>
              <template v-else-if="d.targetType === 'FAN_TARGET'"><span class="chip">응원기금</span> {{ d.targetName }}</template>
              <template v-else>{{ d.streamerNickname }}<span v-if="d.targetName" class="xsmall muted"> ({{ d.targetName }})</span></template>
            </td>
            <td class="small muted">{{ d.message }}</td>
            <td class="num" style="text-align:right">{{ formatWon(d.amount) }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="!(tab === 'given' ? given : received).length" class="muted small" style="padding:12px 0">내역이 없습니다.</div>
    </section>
  </div>
</template>

<style scoped>
.avatar { width: 56px; height: 56px; border-radius: 50%; background: var(--accent-soft); color: var(--accent-text); font-weight: 900; font-size: 22px; display: inline-flex; align-items: center; justify-content: center; }
.hearts { display: flex; flex-direction: column; gap: 2px; padding: 10px 14px; }
.hearts strong { color: var(--heart); font-size: 20px; }
.fan { gap: 10px; padding: 8px; border-radius: 10px; background: var(--bg); }
</style>
