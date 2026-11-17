<script setup>
import { tr } from '../i18n'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { q, syncQuery } from '../lib/query'
import { latestOnly } from '../lib/latest'
import { broadcastApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import BroadcastCard from '../components/BroadcastCard.vue'
import { useViewerStats } from '../composables/useViewerStats'
import { useFandomStore } from '../stores/fandom'

const route = useRoute()
const fandom = useFandomStore()
const { stats, refresh: refreshViewers } = useViewerStats()
const router = useRouter()
const tab = ref(q(route, 'tab', 'LIVE', ['LIVE', 'ALL', 'ENDED']))
const league = ref(q(route, 'league', ''))
const items = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(Math.max(0, Number(q(route, 'page', '1')) - 1) || 0)
const totalPages = ref(0)
let timer = null
const heading = ref(null)
// 15초 자동 새로고침과 탭/리그/페이지 변경이 겹쳐도 마지막 요청의 응답만 쓴다
const latest = latestOnly()

function remember() {
  syncQuery(router, route, { tab: tab.value, league: league.value, page: page.value + 1 }, { tab: 'LIVE', page: 1 })
}

async function load() {
  remember()
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: 24 }
    if (tab.value !== 'ALL') params.status = tab.value
    if (league.value) params.league = league.value
    const { value: res, stale } = await latest(broadcastApi.list(params))
    if (stale) return
    items.value = res.content
    totalPages.value = res.totalPages
    refreshViewers(items.value)
    loading.value = false
  } catch (e) {
    error.value = errorMessage(e, tr('방송 목록을 불러오지 못했습니다.'))
    loading.value = false
  }
}

function changeTab(next) {
  tab.value = next
  page.value = 0
  load()
}

function changeLeague(code) {
  league.value = code
  page.value = 0
  load()
}

async function move(delta) {
  page.value += delta
  await load()
  // 하단 페이지 버튼에 머물지 않도록 목록 위로 올리고 제목으로 초점을 옮긴다(부드러운 스크롤은 쓰지 않는다)
  await nextTick()
  heading.value?.scrollIntoView({ block: 'start' })
  heading.value?.focus({ preventScroll: true })
}

onMounted(() => {
  load()
  timer = setInterval(load, 15000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div>
    <h1 ref="heading" tabindex="-1">{{ tr('전체 방송') }}</h1>
    <div class="tabs">
      <button type="button" :class="{ active: tab === 'LIVE' }" :aria-pressed="tab === 'LIVE'" @click="changeTab('LIVE')">{{ tr('방송 중') }}</button>
      <button type="button" :class="{ active: tab === 'ALL' }" :aria-pressed="tab === 'ALL'" @click="changeTab('ALL')">{{ tr('전체') }}</button>
      <button type="button" :class="{ active: tab === 'ENDED' }" :aria-pressed="tab === 'ENDED'" @click="changeTab('ENDED')">{{ tr('종료된 방송') }}</button>
    </div>
    <div class="pill-tabs">
      <button type="button" :class="{ active: !league }" :aria-pressed="!league" @click="changeLeague('')">{{ tr('전체 리그') }}</button>
      <button v-for="l in fandom.leagues" :key="l.code" type="button" :class="{ active: league === l.code }" :aria-pressed="league === l.code" @click="changeLeague(l.code)">{{ tr(l.name) }}</button>
    </div>

    <div v-if="error" class="empty">{{ error }}</div>
    <div v-else-if="!loading && !items.length" class="empty">
      {{ tab === 'LIVE' ? tr('지금 방송 중인 채널이 없습니다.') : tr('방송이 없습니다.') }}
      <div style="margin-top:12px"><RouterLink to="/broadcasts/new" class="btn primary sm">{{ tr('방송 만들기') }}</RouterLink></div>
    </div>
    <div v-else class="grid" :aria-busy="loading">
      <BroadcastCard v-for="b in items" :key="b.id" :broadcast="b" :viewers="stats[b.id]" />
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button type="button" class="btn sm" :disabled="page === 0" @click="move(-1)">{{ tr('이전') }}</button>
      <span class="muted small" style="align-self:center">{{ page + 1 }} / {{ totalPages }}</span>
      <button type="button" class="btn sm" :disabled="page + 1 >= totalPages" @click="move(1)">{{ tr('다음') }}</button>
    </div>
  </div>
</template>
