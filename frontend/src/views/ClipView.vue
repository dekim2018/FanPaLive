<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tr } from '../i18n'
import { clipApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import TeamCrest from '../components/TeamCrest.vue'
import { formatDate, formatDuration } from '../lib/format'
import { teamStyle } from '../lib/team'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { useBlobVideo } from '../composables/useBlobVideo'

/** 클립 한 개 재생. 원본 녹화의 구간을 MediaMTX Playback 서버에서 바로 받아 재생한다. */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const clip = ref(null)
const error = ref('')
// 클립 파일을 통째로 받아 재생해야 전체 길이가 보이고 원하는 위치로 이동할 수 있다(composables/useBlobVideo).
const { src: videoSrc, loading: videoLoading, failed: videoFailed, retry: retryVideo } = useBlobVideo(computed(() => clip.value?.playbackUrl))
const canDelete = computed(() => clip.value && auth.user
  && (auth.user.id === clip.value.creatorId || auth.user.role === 'ADMIN'))

async function load() {
  try {
    clip.value = await clipApi.get(route.params.id)
  } catch (e) {
    error.value = e?.response?.status === 404 ? tr('클립을 찾을 수 없습니다.') : errorMessage(e)
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    toast.success(tr('클립 링크를 복사했습니다.'))
  } catch {
    toast.error(tr('복사하지 못했습니다. 주소창의 링크를 직접 복사해 주세요.'))
  }
}

// 삭제 요청 중 또 누르면 이미 지운 클립을 다시 지우려다 오류 토스트가 뜬다.
const removing = ref(false)

async function remove() {
  if (removing.value) return
  if (!confirm(tr('클립을 삭제할까요?'))) return
  removing.value = true
  try {
    await clipApi.remove(clip.value.id)
    toast.success(tr('삭제했습니다.'))
    router.replace('/clips')
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    removing.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-if="error" class="empty">{{ error }}</div>
  <div v-else-if="clip" class="stack clip-page" :style="teamStyle(clip.side?.targetColor, clip.side?.targetTextColor)">
    <div class="video-box">
      <video v-if="videoSrc" :src="videoSrc" controls playsinline autoplay muted></video>
      <div v-else class="video-overlay" :aria-busy="videoLoading">
        <div v-if="videoFailed">
          {{ tr('영상을 불러오지 못했습니다.') }}
          <button type="button" class="btn sm" @click="retryVideo">{{ tr('다시 시도') }}</button>
        </div>
        <div v-else>{{ tr('영상을 불러오는 중…') }}</div>
      </div>
    </div>
    <div class="card stack" :class="{ side: clip.side }">
      <h1 style="margin:0; font-size:20px">{{ clip.title }}</h1>
      <div class="row small muted">
        <span>{{ tr('{0} 클립', clip.creatorNickname) }}</span>
        <span>{{ tr('길이 {0}', formatDuration(clip.durationSeconds)) }}</span>
        <span>{{ formatDate(clip.createdAt) }}</span>
      </div>
      <RouterLink v-if="clip.side" :to="`/targets/${clip.side.targetId}`" class="row nowrap team">
        <TeamCrest :name="clip.side.targetName" :short-name="clip.side.targetShortName" :color="clip.side.targetColor" :color2="clip.side.targetTextColor" :size="32" />
        <strong>{{ tr(clip.side.targetName) }}</strong>
      </RouterLink>
      <div class="row">
        <RouterLink :to="`/recordings/${clip.recordingId}`" class="btn sm">{{ tr('전체 다시보기') }}</RouterLink>
        <button type="button" class="btn sm" @click="copyLink">{{ tr('링크 복사') }}</button>
        <button v-if="canDelete" type="button" class="btn sm danger" :disabled="removing" @click="remove">{{ removing ? tr('삭제 중…') : tr('삭제') }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.clip-page { max-width: 960px; margin: 0 auto; }
.card.side { border-top: 3px solid var(--team); }
.team { width: fit-content; }
</style>
