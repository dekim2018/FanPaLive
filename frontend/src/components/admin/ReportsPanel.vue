<script setup>
import { onMounted, ref } from 'vue'
import { tr } from '../../i18n'
import { reportApi } from '../../api/interactions'
import { errorMessage } from '../../api/http'
import { formatDate } from '../../lib/format'
import { useToastStore } from '../../stores/toast'

/**
 * 신고 처리. 같은 대상 신고는 한 번 처리하면 모두 닫힌다.
 * 댓글: 숨김(삭제 + 실시간으로 화면에서 내림). 방송: 중단(종료 + 목록에서 숨김 + 재시작 막기).
 */
const toast = useToastStore()
const status = ref('OPEN')
const items = ref([])
const loading = ref(true)
const STATUS = { OPEN: tr('처리 대기'), ACTIONED: tr('조치함'), DISMISSED: tr('문제없음') }

async function load() {
  loading.value = true
  try {
    items.value = (await reportApi.list({ status: status.value, size: 50 })).content
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    loading.value = false
  }
}

async function resolve(r, action) {
  const ask = {
    HIDE_COMMENT: tr('이 댓글을 숨길까요? 모든 시청자 화면에서 사라집니다.'),
    END_BROADCAST: tr('이 방송을 중단할까요? 방송이 종료되고 목록에서 사라지며 다시 시작할 수 없습니다.'),
    DISMISS: tr('문제없음으로 닫을까요?')
  }[action]
  if (!confirm(ask)) return
  try {
    const res = await reportApi.resolve(r.id, action, '')
    toast.success(tr('처리했습니다. 같은 대상 신고 {0}건을 닫았습니다.', res.closed))
    load()
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <div class="pill-tabs" style="margin:0">
      <button v-for="s in ['OPEN', 'ACTIONED', 'DISMISSED']" :key="s" :class="{ active: status === s }" @click="status = s; load()">{{ STATUS[s] }}</button>
    </div>
    <div v-if="!loading && !items.length" class="empty">{{ tr('신고가 없습니다.') }}</div>
    <div v-for="r in items" :key="r.id" class="card report">
      <div class="row">
        <span class="chip">{{ r.type === 'BROADCAST' ? tr('방송') : tr('댓글') }}</span>
        <strong>{{ tr(r.reasonLabel) }}</strong>
        <span v-if="r.openCount > 1" class="badge unknown">{{ tr('같은 대상 신고 {0}건', r.openCount) }}</span>
        <span class="spacer"></span>
        <span class="xsmall muted">{{ tr('{0}님 신고, {1}', r.reporterNickname, formatDate(r.createdAt)) }}</span>
      </div>
      <p class="quote small">{{ r.snapshot }}</p>
      <p v-if="r.detail" class="small muted" style="margin:0">{{ tr('신고 내용: {0}', r.detail) }}</p>
      <div class="row">
        <RouterLink v-if="r.broadcastId" :to="`/broadcasts/${r.broadcastId}`" class="btn sm ghost">{{ tr('방송 보기') }}</RouterLink>
        <span class="spacer"></span>
        <template v-if="r.status === 'OPEN'">
          <button v-if="r.type === 'COMMENT'" type="button" class="btn sm danger" @click="resolve(r, 'HIDE_COMMENT')">{{ tr('댓글 숨김') }}</button>
          <button v-else type="button" class="btn sm danger" @click="resolve(r, 'END_BROADCAST')">{{ tr('방송 중단') }}</button>
          <button type="button" class="btn sm" @click="resolve(r, 'DISMISS')">{{ tr('문제없음') }}</button>
        </template>
        <span v-else class="small muted">{{ STATUS[r.status] }}, {{ formatDate(r.resolvedAt) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.report { display: flex; flex-direction: column; gap: 8px; }
.quote { margin: 0; padding: 8px 10px; border-left: 3px solid var(--border-strong); background: var(--bg); border-radius: 0 8px 8px 0; overflow-wrap: anywhere; }
</style>
