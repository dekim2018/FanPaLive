<script setup>
import { computed, ref } from 'vue'
import { tr } from '../i18n'
import { clipApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import { formatDuration } from '../lib/format'
import { useToastStore } from '../stores/toast'

/**
 * 다시보기에서 지금 보고 있는 장면부터 클립을 만든다.
 * 파일을 새로 자르지 않고 "녹화 시작부터 몇 초, 몇 초 동안"만 저장해서 바로 만들어진다.
 */
const props = defineProps({
  recording: { type: Object, required: true },
  /** 지금 재생 위치(녹화 시작부터 초) */
  position: { type: Number, default: 0 }
})
const emit = defineEmits(['created'])
const toast = useToastStore()
const LENGTHS = [15, 30, 60]
const length = ref(30)
const title = ref('')
const saving = ref(false)
const offset = computed(() => {
  const max = Math.max(0, (props.recording.durationSeconds || 0) - length.value)
  return Math.min(Math.max(0, Math.floor(props.position)), max)
})

async function create() {
  if (saving.value) return
  saving.value = true
  try {
    const clip = await clipApi.create(props.recording.id, { title: title.value, offsetSeconds: offset.value, durationSeconds: length.value })
    toast.success(tr('클립을 만들었습니다.'))
    title.value = ''
    emit('created', clip)
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <form class="maker" @submit.prevent="create">
    <div class="row">
      <strong>{{ tr('하이라이트 클립 만들기') }}</strong>
      <span class="xsmall muted">{{ tr('지금 보는 장면 {0}부터', formatDuration(offset)) }}</span>
    </div>
    <div class="row">
      <div class="seg" role="radiogroup" :aria-label="tr('클립 길이')">
        <button v-for="l in LENGTHS" :key="l" type="button" role="radio" :aria-checked="length === l"
                :class="{ on: length === l }" @click="length = l">{{ tr('{0}초', l) }}</button>
      </div>
      <input v-model="title" name="clip-title" class="input title" maxlength="60" autocomplete="off"
             :aria-label="tr('클립 제목')" :placeholder="tr('클립 제목 (예: 9회말 끝내기 홈런)…')" />
      <button type="submit" class="btn primary sm" :disabled="saving">{{ tr('클립 만들기') }}</button>
    </div>
  </form>
</template>

<style scoped>
.maker { display: flex; flex-direction: column; gap: 8px; padding: 12px; border-radius: 10px; background: var(--accent-soft); border: 1px solid var(--accent-border); }
.seg { display: inline-flex; border: 1px solid var(--border-strong); border-radius: 8px; overflow: hidden; background: var(--bg-elev); }
.seg button { border: none; background: none; padding: 0 12px; height: 34px; font: inherit; font-size: 13px; font-weight: 700; color: var(--text-sub); cursor: pointer; }
.seg button.on { background: var(--accent); color: var(--on-accent); }
.title { flex: 1; min-width: 180px; height: 36px; }
</style>
