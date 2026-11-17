<script setup>
import { tr } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clipApi, recordingApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import ClipCard from '../components/ClipCard.vue'
import ClipMaker from '../components/ClipMaker.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { formatDate, formatDuration } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import TrText from '../components/TrText.vue'

/**
 * 녹화 영상은 MediaMTX 가 일정 길이의 세그먼트로 저장한다.
 * 서버가 내려준 구간 목록을 순서대로 이어서 재생한다.
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const recording = ref(null)
const error = ref('')
const current = ref(0)
const videoEl = ref(null)
const deleting = ref(false)

const segments = computed(() => recording.value?.segments || [])
const currentUrl = computed(() => segments.value[current.value]?.url)
const isOwner = computed(() => recording.value && auth.user?.id === recording.value.ownerId)
const clips = ref([])
const playhead = ref(0)

/** 지금 재생 위치를 "녹화 시작부터 몇 초"로 바꾼다(구간 파일이 여러 개라 구간 시작 시각을 더한다). */
function onTime() {
  const seg = segments.value[current.value]
  if (!seg || !recording.value) return
  const segStart = (new Date(seg.start).getTime() - new Date(recording.value.startedAt).getTime()) / 1000
  playhead.value = Math.max(0, segStart + (videoEl.value?.currentTime || 0))
}

async function loadClips() {
  try {
    clips.value = await clipApi.byRecording(route.params.id)
  } catch {
    clips.value = []
  }
}

async function load() {
  try {
    recording.value = await recordingApi.get(route.params.id)
  } catch (e) {
    error.value = e?.response?.status === 404 ? tr('녹화 영상을 찾을 수 없습니다.') : errorMessage(e)
  }
}

function playSegment(index) {
  current.value = index
  requestAnimationFrame(() => videoEl.value?.play().catch(() => {}))
}

function onEnded() {
  if (current.value + 1 < segments.value.length) playSegment(current.value + 1)
}

async function remove() {
  if (deleting.value || !confirm(tr('녹화 영상을 삭제할까요?'))) return
  // 요청 중에 다시 누르면 같은 삭제가 두 번 나가므로 끝날 때까지 버튼을 막는다.
  deleting.value = true
  try {
    await recordingApi.remove(recording.value.id)
    toast.success(tr('삭제했습니다.'))
    router.replace('/recordings')
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  load()
  loadClips()
})
</script>

<template>
  <div v-if="error" class="empty">{{ error }}</div>
  <div v-else-if="recording" class="stack">
    <div class="video-box">
      <video v-if="currentUrl" ref="videoEl" :src="currentUrl" controls playsinline @ended="onEnded" @timeupdate="onTime"></video>
      <div v-else class="video-overlay">
        <div v-if="recording.status === 'RECORDING'">{{ tr('아직 방송 중이라 녹화가 진행되고 있습니다.') }}</div>
        <div v-else>{{ tr('재생할 수 있는 녹화 파일이 없습니다.') }}</div>
      </div>
    </div>
    <div class="card stack">
      <div class="row">
        <StatusBadge :status="recording.status === 'READY' ? 'VOD' : recording.status" />
        <h1 style="margin:0; font-size:20px">{{ recording.title }}</h1>
      </div>
      <div class="small muted">
        {{ tr('{0}, {1} ~ {2}, 길이 {3}', recording.ownerNickname, formatDate(recording.startedAt), formatDate(recording.endedAt), formatDuration(recording.durationSeconds)) }}
      </div>
      <div class="small muted"><TrText :text="tr('저장 위치: {0}')"><template #p0><code>{{ recording.storageLocation }}</code></template></TrText></div>
      <div v-if="segments.length > 1" class="row">
        <button v-for="(s, i) in segments" :key="s.start" type="button" class="btn sm" :class="{ primary: i === current }"
                :aria-pressed="i === current" @click="playSegment(i)">
          {{ tr('구간 {0} ({1})', i + 1, formatDuration(s.durationSeconds)) }}
        </button>
      </div>
      <div class="row">
        <RouterLink :to="`/broadcasts/${recording.broadcastId}`" class="btn sm">{{ tr('방송 페이지') }}</RouterLink>
        <button v-if="isOwner" type="button" class="btn sm danger" :disabled="deleting" @click="remove">{{ deleting ? tr('삭제 중…') : tr('녹화 삭제') }}</button>
      </div>
      <ClipMaker v-if="auth.isLoggedIn && recording.status === 'READY' && segments.length" :recording="recording" :position="playhead"
                 @created="(c) => clips.unshift(c)" />
    </div>
    <section v-if="clips.length" class="stack">
      <div class="section-head" style="margin:0"><h2>{{ tr('이 녹화의 하이라이트') }}</h2></div>
      <div class="grid dense">
        <ClipCard v-for="c in clips" :key="c.id" :clip="c" />
      </div>
    </section>
  </div>
</template>
