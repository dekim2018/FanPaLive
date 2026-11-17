<script setup>
import { tr } from '../i18n'
import { onBeforeUnmount, ref, watch } from 'vue'
import { NotPublishingError, startWhepPlay } from '../lib/whep'

/**
 * WHEP 플레이어. 방송 중(active)이면 연결을 시도하고, 송출이 아직 없거나 끊기면 3초마다 재시도한다.
 */
const props = defineProps({
  url: { type: String, required: true },
  active: { type: Boolean, default: true }
})

const video = ref(null)
const state = ref('idle') // idle | connecting | waiting | playing | error
const muted = ref(true)
const message = ref('')
let session = null
let retryTimer = null
let stopped = false

async function connect() {
  clearTimeout(retryTimer)
  if (!props.active || stopped) return
  state.value = 'connecting'
  try {
    session = await startWhepPlay({
      url: props.url,
      onTrack: (stream) => {
        if (video.value && video.value.srcObject !== stream) {
          video.value.srcObject = stream
          video.value.play().catch(() => {})
        }
        state.value = 'playing'
      },
      onStateChange: (s) => {
        if (s === 'failed' || s === 'disconnected' || s === 'closed') scheduleRetry(tr('연결이 끊겼습니다. 다시 연결하는 중…'))
      }
    })
  } catch (e) {
    scheduleRetry(e instanceof NotPublishingError ? tr('방송자의 송출을 기다리는 중…') : e.message)
  }
}

function scheduleRetry(text) {
  teardown()
  if (!props.active || stopped) return
  state.value = 'waiting'
  message.value = text
  retryTimer = setTimeout(connect, 3000)
}

function teardown() {
  if (session) {
    session.stop()
    session = null
  }
  if (video.value) video.value.srcObject = null
}

function toggleMute() {
  muted.value = !muted.value
  if (video.value) {
    video.value.muted = muted.value
    video.value.play().catch(() => {})
  }
}

watch(() => props.active, (active) => {
  if (active) connect()
  else { clearTimeout(retryTimer); teardown(); state.value = 'idle' }
}, { immediate: true })

onBeforeUnmount(() => {
  stopped = true
  clearTimeout(retryTimer)
  teardown()
})
</script>

<template>
  <div class="video-box">
    <video ref="video" autoplay playsinline :muted="muted" :aria-label="tr('라이브 방송 영상')"></video>
    <div v-if="state !== 'playing'" class="video-overlay" role="status" aria-live="polite">
      <slot name="overlay" :state="state">
        <div v-if="state === 'connecting'">{{ tr('연결 중…') }}</div>
        <div v-else-if="state === 'waiting'">{{ message }}</div>
      </slot>
    </div>
    <button v-if="state === 'playing'" type="button" class="btn sm sound" :aria-pressed="!muted" @click="toggleMute">
      {{ muted ? tr('소리 켜기') : tr('음소거') }}
    </button>
  </div>
</template>

<style scoped>
.sound { position: absolute; left: 12px; bottom: 12px; background: rgba(0,0,0,.7); color: #fff; }
.sound:hover { background: rgba(0,0,0,.85); }
</style>
