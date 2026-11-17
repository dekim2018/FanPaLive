<script setup>
import { tr } from '../i18n'
import { onMounted, ref } from 'vue'
import { recordingApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import StatusBadge from '../components/StatusBadge.vue'
import { formatDate, formatDuration } from '../lib/format'
import { useFandomStore } from '../stores/fandom'
import { solid, teamFg } from '../lib/team'

const fandom = useFandomStore()
const league = ref('')

const items = ref([])
const loading = ref(true)
const error = ref('')
const page = ref(0)
const totalPages = ref(0)

async function load() {
  loading.value = true
  // 이전 실패 문구가 남아 있으면 다시 불러와도 목록 대신 오류만 보인다.
  error.value = ''
  try {
    const params = { page: page.value, size: 20 }
    if (league.value) params.league = league.value
    const res = await recordingApi.list(params)
    items.value = res.content
    totalPages.value = res.totalPages
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
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
  load()
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <h1>{{ tr('다시보기') }}</h1>
    <div class="pill-tabs" style="margin:0">
      <button :class="{ active: !league }" @click="changeLeague('')">{{ tr('전체') }}</button>
      <button v-for="l in fandom.leagues" :key="l.code" :class="{ active: league === l.code }" @click="changeLeague(l.code)">{{ tr(l.name) }}</button>
    </div>
    <div v-if="error" class="empty">{{ error }}</div>
    <div v-else-if="!loading && !items.length" class="empty">{{ tr('아직 녹화된 방송이 없습니다. 방송을 하면 자동으로 녹화됩니다.') }}</div>
    <div v-else class="grid">
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
