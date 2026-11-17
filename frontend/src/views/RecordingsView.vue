<script setup>
import { tr } from '../i18n'
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recordingApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import StatusBadge from '../components/StatusBadge.vue'
import { formatDate, formatDuration } from '../lib/format'
import { latestOnly } from '../lib/latest'
import { q, syncQuery } from '../lib/query'
import { useFandomStore } from '../stores/fandom'
import { solid, teamFg } from '../lib/team'

const route = useRoute()
const router = useRouter()
const fandom = useFandomStore()
// 새로고침이나 링크 공유를 해도 같은 리그와 페이지가 열리도록 주소에서 읽고 주소에 남긴다.
const league = ref(q(route, 'league', ''))

const items = ref([])
const loading = ref(true)
const error = ref('')
const page = ref(Math.max(0, (Number(q(route, 'page', '1')) || 1) - 1))
const totalPages = ref(0)
// 페이지 번호는 사람이 읽기 쉽게 1부터 주소에 남긴다.
watch([league, page], () => syncQuery(router, route,
  { league: league.value, page: page.value + 1 }, { page: 1 }))

// 리그나 페이지를 빠르게 바꿔도 마지막 요청의 응답만 쓴다
const latest = latestOnly()

async function load() {
  loading.value = true
  // 이전 실패 문구가 남아 있으면 다시 불러와도 목록 대신 오류만 보인다.
  error.value = ''
  try {
    const params = { page: page.value, size: 20 }
    if (league.value) params.league = league.value
    const { value, stale } = await latest(recordingApi.list(params))
    if (stale) return
    items.value = value.content
    totalPages.value = value.totalPages
    loading.value = false
  } catch (e) {
    error.value = errorMessage(e)
    loading.value = false
  }
}

function changeLeague(code) {
  league.value = code
  page.value = 0
  load()
}

function move(delta) {
  page.value += delta
  // 아래쪽 페이지 버튼을 누른 채로 새 목록의 끝이 보이지 않도록 맨 위로 올린다.
  window.scrollTo({ top: 0 })
  load()
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <h1>{{ tr('다시보기') }}</h1>
    <div class="pill-tabs" style="margin:0">
      <button type="button" :class="{ active: !league }" :aria-pressed="!league" @click="changeLeague('')">{{ tr('전체') }}</button>
      <button v-for="l in fandom.leagues" :key="l.code" type="button" :class="{ active: league === l.code }"
              :aria-pressed="league === l.code" @click="changeLeague(l.code)">{{ tr(l.name) }}</button>
    </div>
    <div v-if="error" class="empty">{{ error }} <button type="button" class="btn sm" @click="load">{{ tr('다시 시도') }}</button></div>
    <div v-else-if="loading && !items.length" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
    <div v-else-if="!items.length" class="empty">{{ tr('아직 녹화된 방송이 없습니다. 방송을 하면 자동으로 녹화됩니다.') }}</div>
    <!-- 다시 불러오는 동안에는 이전 목록을 두고 aria-busy 로만 알린다 -->
    <div v-else class="grid" :aria-busy="loading">
      <RouterLink v-for="r in items" :key="r.id" :to="`/recordings/${r.id}`" class="card stack rec">
        <div class="row">
          <StatusBadge :status="r.status === 'READY' ? 'VOD' : r.status" />
          <span class="spacer"></span>
          <span class="small muted">{{ r.status === 'READY' ? formatDuration(r.durationSeconds) : tr('녹화 중') }}</span>
        </div>
        <div class="title">{{ r.title }}</div>
        <div v-if="r.side" class="row"><span class="tchip" :style="{ background: solid(r.side.targetColor), color: teamFg(r.side.targetColor, r.side.targetTextColor) }">{{ tr(r.side.targetShortName || r.side.targetName) }}</span><span class="xsmall muted">{{ tr('{0} 편파중계', r.side.leagueCode) }}</span></div>
        <div class="small muted">{{ r.ownerNickname }}, {{ formatDate(r.startedAt) }}</div>
      </RouterLink>
    </div>
    <div v-if="totalPages > 1" class="pagination">
      <button class="btn sm" :disabled="page === 0" @click="move(-1)">{{ tr('이전') }}</button>
      <button class="btn sm" :disabled="page + 1 >= totalPages" @click="move(1)">{{ tr('다음') }}</button>
    </div>
  </div>
</template>

<style scoped>
.rec { gap: 8px; }
.tchip { font-size: 11px; font-weight: 800; padding: 0 6px; border-radius: 4px; line-height: 18px; }
.title { font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
