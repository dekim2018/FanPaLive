<script setup>
import { tr } from '../i18n'
import { onMounted, ref } from 'vue'
import { broadcastApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import StatusBadge from '../components/StatusBadge.vue'
import { formatDate } from '../lib/format'
import { useToastStore } from '../stores/toast'

const toast = useToastStore()
const items = ref([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    items.value = (await broadcastApi.mine({ size: 50 })).content
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    loading.value = false
  }
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
    <div v-if="!loading && !items.length" class="empty">{{ tr('아직 만든 방송이 없습니다.') }}</div>
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
</template>

<style scoped>
.item { gap: 12px; }
.info { min-width: 0; }
.title { font-weight: 700; }
</style>
