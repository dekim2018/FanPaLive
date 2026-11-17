<script setup>
import { tr } from '../i18n'
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { q, syncQuery } from '../lib/query'
import { latestOnly } from '../lib/latest'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import RankingTable from '../components/RankingTable.vue'
import { PERIOD_LABEL } from '../lib/team'
import { useFandomStore } from '../stores/fandom'

/**
 * 리그별 응원 랭킹. 점수 = 주 팬 x100 + 팔로우 x20 + 하트 점수 + 후원금/100.
 * 실시간 순위는 Redis 정렬 집합에서 읽고, 매일 0시에 DB 기준으로 다시 만든다.
 */
const route = useRoute()
const fandom = useFandomStore()
const router = useRouter()
const league = ref(q(route, 'league', 'KBO'))
const period = ref(q(route, 'period', 'WEEKLY', ['WEEKLY', 'MONTHLY', 'SEASON']))
const sort = ref(q(route, 'sort', 'SCORE', ['SCORE', 'FANS', 'HEARTS', 'DONATIONS']))
watch([league, period, sort], () => syncQuery(router, route,
  { league: league.value, period: period.value, sort: sort.value }, { league: 'KBO', period: 'WEEKLY', sort: 'SCORE' }))
const data = ref(null)
const loading = ref(true)
const error = ref('')
// 필터를 빠르게 바꿔도 마지막 요청의 응답만 쓴다
const latest = latestOnly()
const SORTS = { SCORE: tr('응원 점수'), FANS: tr('팬 수'), HEARTS: tr('하트'), DONATIONS: tr('후원') }

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { value, stale } = await latest(fandomApi.ranking(league.value, period.value, sort.value))
    if (stale) return
    data.value = value
    loading.value = false
  } catch (e) {
    error.value = errorMessage(e, tr('랭킹을 불러오지 못했습니다.'))
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>{{ tr('응원 랭킹') }}</h1>
    <div class="pill-tabs">
      <button v-for="l in fandom.leagues" :key="l.code" type="button" :class="{ active: league === l.code }" :aria-pressed="league === l.code" @click="league = l.code; load()">{{ tr(l.name) }}</button>
    </div>
    <div class="row filters">
      <div class="seg">
        <button v-for="p in ['WEEKLY', 'MONTHLY', 'SEASON']" :key="p" type="button" :class="{ on: period === p }" :aria-pressed="period === p" @click="period = p; load()">{{ PERIOD_LABEL[p] }}</button>
      </div>
      <div class="seg">
        <button v-for="(label, key) in SORTS" :key="key" type="button" :class="{ on: sort === key }" :aria-pressed="sort === key" @click="sort = key; load()">{{ label }}</button>
      </div>
      <span class="spacer"></span>
      <span v-if="data" class="small muted">{{ data.from }} ~ {{ data.to }}</span>
    </div>
    <div v-if="error" class="empty">{{ error }} <button type="button" class="btn sm" @click="load">{{ tr('다시 시도') }}</button></div>
    <!-- 다시 불러오는 동안에는 이전 순위를 두고 aria-busy 로만 알린다 -->
    <section v-else-if="data" class="card" :aria-busy="loading">
      <RankingTable :rows="data.rows" :show-heart="fandom.feature('hearts')" />
    </section>
    <div v-else-if="loading" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
    <p class="xsmall dim">
      {{ tr('응원 점수는 주 응원 팬 1명당 100점, 팔로우 1명당 20점, 하트 점수, 후원금 100원당 1점을 더한 값입니다. 점수가 같으면 그 점수에 먼저 도달한 대상이 위에 옵니다. 같은 기기나 네트워크에서 몰아준 하트는 점수에서 빠질 수 있습니다.') }}
    </p>
  </div>
</template>

<style scoped>
.filters { margin-bottom: 16px; }
.seg { display: inline-flex; background: var(--bg-elev); border: 1px solid var(--border); border-radius: 8px; padding: 3px; gap: 2px; }
.seg button { border: none; background: none; color: var(--text-sub); font-weight: 700; font-size: 13px; padding: 5px 10px; border-radius: 6px; cursor: pointer; }
.seg button.on { background: var(--bg-elev-2); color: var(--text); }
</style>
