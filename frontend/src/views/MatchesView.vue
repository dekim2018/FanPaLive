<script setup>
import { intlLocale, tr } from '../i18n'
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { q, syncQuery } from '../lib/query'
import { latestOnly } from '../lib/latest'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import MatchCard from '../components/MatchCard.vue'
import { useFandomStore } from '../stores/fandom'

const fandom = useFandomStore()
const today = new Date()
const toIso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const route = useRoute()
const router = useRouter()
const date = ref(/^\d{4}-\d{2}-\d{2}$/.test(q(route, 'date', '')) ? q(route, 'date', '') : toIso(today))
const league = ref(q(route, 'league', ''))
watch([date, league], () => syncQuery(router, route, { date: date.value, league: league.value }, { date: toIso(today) }))
const items = ref([])
const loading = ref(true)
const error = ref('')
// 날짜/리그를 빠르게 바꿔도 마지막 요청의 응답만 쓴다
const latest = latestOnly()
// 날짜 탭은 화면 언어에 맞춘 형식으로 쓴다(한국어 "11. 17. (화)", 영어 "Tue, 11/17")
const dayFormatter = new Intl.DateTimeFormat(intlLocale, { month: 'numeric', day: 'numeric', weekday: 'short' })
const days = Array.from({ length: 7 }, (_, i) => {
  const d = new Date(today)
  d.setDate(d.getDate() + i - 1)
  return { iso: toIso(d), label: i === 1 ? tr('오늘') : dayFormatter.format(d) }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { date: date.value }
    if (league.value) params.league = league.value
    const { value, stale } = await latest(fandomApi.matches(params))
    if (stale) return
    items.value = value
    loading.value = false
  } catch (e) {
    error.value = errorMessage(e, tr('경기 일정을 불러오지 못했습니다.'))
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>{{ tr('경기 일정') }}</h1>
    <div v-if="!fandom.feature('matches')" class="empty">{{ tr('경기 응원 기능이 잠시 꺼져 있습니다.') }}</div>
    <template v-else>
      <div class="pill-tabs">
        <button v-for="d in days" :key="d.iso" type="button" :class="{ active: date === d.iso }" :aria-pressed="date === d.iso" @click="date = d.iso; load()">{{ d.label }}</button>
      </div>
      <div class="pill-tabs">
        <button type="button" :class="{ active: !league }" :aria-pressed="!league" @click="league = ''; load()">{{ tr('전체 리그') }}</button>
        <button v-for="l in fandom.leagues" :key="l.code" type="button" :class="{ active: league === l.code }" :aria-pressed="league === l.code" @click="league = l.code; load()">{{ tr(l.name) }}</button>
      </div>
      <div v-if="error" class="empty">{{ error }} <button type="button" class="btn sm" @click="load">{{ tr('다시 시도') }}</button></div>
      <div v-else-if="loading && !items.length" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
      <div v-else-if="!items.length" class="empty">{{ tr('이 날짜에 등록된 경기가 없습니다.') }}</div>
      <!-- 다시 불러오는 동안에는 이전 목록을 두고 aria-busy 로만 알린다 -->
      <div v-else class="grid dense" :aria-busy="loading">
        <MatchCard v-for="m in items" :key="m.id" :match="m" />
      </div>
    </template>
  </div>
</template>
