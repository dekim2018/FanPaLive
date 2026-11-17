<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { q, syncQuery } from '../lib/query'
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
const error = ref('')
const SORTS = { SCORE: '응원 점수', FANS: '팬 수', HEARTS: '하트', DONATIONS: '후원' }

async function load() {
  error.value = ''
  try {
    data.value = await fandomApi.ranking(league.value, period.value, sort.value)
  } catch (e) {
    error.value = errorMessage(e, '랭킹을 불러오지 못했습니다.')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>응원 랭킹</h1>
    <div class="pill-tabs">
      <button v-for="l in fandom.leagues" :key="l.code" :class="{ active: league === l.code }" @click="league = l.code; load()">{{ l.name }}</button>
    </div>
    <div class="row filters">
      <div class="seg">
        <button v-for="p in ['WEEKLY', 'MONTHLY', 'SEASON']" :key="p" :class="{ on: period === p }" @click="period = p; load()">{{ PERIOD_LABEL[p] }}</button>
      </div>
      <div class="seg">
        <button v-for="(label, key) in SORTS" :key="key" :class="{ on: sort === key }" @click="sort = key; load()">{{ label }}</button>
      </div>
      <span class="spacer"></span>
      <span v-if="data" class="small muted">{{ data.from }} ~ {{ data.to }}</span>
    </div>
    <div v-if="error" class="empty">{{ error }}</div>
    <section v-else-if="data" class="card">
      <RankingTable :rows="data.rows" :show-heart="fandom.feature('hearts')" />
    </section>
    <p class="xsmall dim">
      응원 점수는 주 응원 팬 1명당 100점, 팔로우 1명당 20점, 하트 점수, 후원금 100원당 1점을 더한 값입니다.
      점수가 같으면 그 점수에 먼저 도달한 대상이 위에 옵니다. 같은 기기나 네트워크에서 몰아준 하트는 점수에서 빠질 수 있습니다.
    </p>
  </div>
</template>

<style scoped>
.filters { margin-bottom: 16px; }
.seg { display: inline-flex; background: var(--bg-elev); border: 1px solid var(--border); border-radius: 8px; padding: 3px; gap: 2px; }
.seg button { border: none; background: none; color: var(--text-sub); font-weight: 700; font-size: 13px; padding: 5px 10px; border-radius: 6px; cursor: pointer; }
.seg button.on { background: var(--bg-elev-2); color: var(--text); }
</style>
