<script setup>
import { tr } from '../i18n'
import { computed, ref } from 'vue'
import { solid, teamFg } from '../lib/team'
import { useRouter } from 'vue-router'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'

/**
 * 하트 보내기. 누를 때마다 서버에 1개씩 보낸다(하루 5개, 한 대상에 3개까지).
 * 서버는 받은 하트를 모아서 200ms 마다 반영하므로 랭킹 숫자는 잠시 뒤에 바뀐다.
 */
const props = defineProps({
  targetId: { type: Number, required: true },
  source: { type: String, default: 'TARGET_PAGE' },
  matchId: { type: Number, default: null },
  compact: { type: Boolean, default: false },
  /** 매치룸처럼 팀 색 버튼이 필요할 때: 배경색과 글자색 */
  color: { type: String, default: '' },
  color2: { type: String, default: '' }
})
const teamLook = computed(() => (props.color ? { background: solid(props.color), color: teamFg(props.color, props.color2) } : null))
const emit = defineEmits(['sent'])

const auth = useAuthStore()
const fandom = useFandomStore()
const toast = useToastStore()
const router = useRouter()
const sending = ref(false)
const bursts = ref([])
let seq = 0

async function send() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (sending.value) return
  sending.value = true
  const id = ++seq
  bursts.value.push(id)
  setTimeout(() => { bursts.value = bursts.value.filter((b) => b !== id) }, 900)
  try {
    const res = await fandomApi.giveHeart({ targetId: props.targetId, amount: 1, source: props.source, matchId: props.matchId })
    fandom.heartsLeft = res.remainingToday
    emit('sent', res)
    toast.success(tr('하트를 보냈어요. 오늘 남은 하트 {0}개', res.remainingToday))
  } catch (e) {
    toast.error(errorMessage(e, tr('하트를 보내지 못했습니다.')))
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <button type="button" class="heart-btn" :class="{ compact, 'team-look': !!teamLook }" :style="teamLook" :disabled="sending"
          :aria-label="compact ? tr('하트 보내기') : undefined" @click="send">
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
      <path fill="currentColor" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
    </svg>
    <span v-if="!compact">{{ tr('하트 보내기') }}</span>
    <span v-if="!compact && fandom.heartsLeft !== null" class="left" :aria-label="tr('오늘 남은 하트 {0}개', fandom.heartsLeft)">{{ fandom.heartsLeft }}</span>
    <span v-for="b in bursts" :key="b" class="burst" aria-hidden="true">+1</span>
  </button>
</template>

<style scoped>
.heart-btn {
  position: relative; display: inline-flex; align-items: center; gap: 6px;
  min-height: 38px; padding: 0 14px; border-radius: 999px; border: none; cursor: pointer;
  background: var(--heart-soft); color: var(--heart); font-weight: 800; font-size: 14px;
  transition: transform .1s, background-color .15s; touch-action: manipulation;
}
.heart-btn:hover { background: #fbd5e3; }
.heart-btn.team-look:hover { filter: brightness(1.12); }
.heart-btn:active { transform: scale(.94); }
.heart-btn svg { flex-shrink: 0; display: block; }
.heart-btn.compact { width: 36px; height: 36px; padding: 0; justify-content: center; }
.left { background: var(--heart); color: #fff; border-radius: 999px; padding: 0 6px; font-size: 11px; }
.burst {
  position: absolute; left: 50%; top: -4px; pointer-events: none;
  color: var(--heart); font-weight: 900; font-size: 13px;
  animation: rise .9s ease-out forwards;
}
@keyframes rise { from { opacity: 1; transform: translate(-50%, 0); } to { opacity: 0; transform: translate(-50%, -28px); } }
</style>
