<script setup>
import { tr } from '../i18n'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { broadcastApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import StatusBadge from '../components/StatusBadge.vue'
import { formatDate } from '../lib/format'
import { latestOnly } from '../lib/latest'
import { q, syncQuery } from '../lib/query'
import { useToastStore } from '../stores/toast'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const items = ref([])
const loading = ref(true)
const error = ref('')
// 주소의 페이지 번호는 1부터, API 는 0부터
const page = ref(Math.max(0, (parseInt(q(route, 'page', '1'), 10) || 1) - 1))
const totalPages = ref(0)
const latest = latestOnly()

async function load() {
  loading.value = true
  // 이전 실패 문구가 남아 있으면 다시 불러와도 목록 대신 오류만 보인다.
  error.value = ''
  try {
    const { value: res, stale } = await latest(broadcastApi.mine({ page: page.value, size: 20 }))
    if (stale) return
    // 마지막 방송을 지워 지금 페이지가 비면 앞 페이지로 간다.
    if (!res.content.length && page.value > 0) {
      page.value = Math.max(0, res.totalPages - 1)
      syncQuery(router, route, { page: page.value + 1 }, { page: 1 })
      return load()
    }
    items.value = res.content
    totalPages.value = res.totalPages
    loading.value = false
  } catch (e) {
    error.value = errorMessage(e)
    loading.value = false
  }
}

function move(delta) {
  page.value += delta
  syncQuery(router, route, { page: page.value + 1 }, { page: 1 })
  load()
}

async function remove(b) {
  if (!confirm(tr('"{0}" 방송을 삭제할까요?', b.title))) return
  try {
    await broadcastApi.remove(b.id)
    toast.success(tr('삭제했습니다.'))
    load()
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <div class="row">
      <h1 style="margin:0">{{ tr('내 방송') }}</h1>
      <span class="spacer"></span>
      <RouterLink to="/broadcasts/new" class="btn primary sm">{{ tr('새 방송') }}</RouterLink>
    </div>
    <div v-if="error" class="empty">{{ error }} <button type="button" class="btn sm" @click="load">{{ tr('다시 시도') }}</button></div>
    <div v-else-if="loading && !items.length" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
    <div v-else-if="!items.length" class="empty">{{ tr('아직 만든 방송이 없습니다.') }}</div>
    <div v-else class="stack" :aria-busy="loading">
      <div v-for="b in items" :key="b.id" class="card row item">
        <StatusBadge :status="b.status" :online="b.streamOnline" />
        <div class="info">
          <RouterLink :to="`/broadcasts/${b.id}`" class="title">{{ b.title }}</RouterLink>
          <div class="muted small">{{ tr('등록 {0}, 최근 방송 {1}', formatDate(b.createdAt), formatDate(b.startedAt)) }}</div>
        </div>
        <span class="spacer"></span>
        <RouterLink :to="`/studio/${b.id}`" class="btn sm primary">{{ tr('스튜디오') }}</RouterLink>
        <RouterLink :to="`/broadcasts/${b.id}/edit`" class="btn sm">{{ tr('수정') }}</RouterLink>
        <button class="btn sm danger" :disabled="b.status === 'LIVE'" @click="remove(b)">{{ tr('삭제') }}</button>
      </div>
    </div>
    <div v-if="totalPages > 1" class="pagination">
      <button type="button" class="btn sm" :disabled="loading || page === 0" @click="move(-1)">{{ tr('이전') }}</button>
      <button type="button" class="btn sm" :disabled="loading || page + 1 >= totalPages" @click="move(1)">{{ tr('다음') }}</button>
    </div>
  </div>
</template>

<style scoped>
.item { gap: 12px; }
.info { min-width: 0; }
.title { font-weight: 700; }
</style>
