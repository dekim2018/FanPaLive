<script setup>
import { ref } from 'vue'
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
  compact: { type: Boolean, default: false }
})
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
    toast.success(`하트를 보냈어요. 오늘 남은 하트 ${res.remainingToday}개`)
  } catch (e) {
    toast.error(errorMessage(e, '하트를 보내지 못했습니다.'))
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <button class="heart-btn" :class="{ compact }" :disabled="sending" @click="send">
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
      <path fill="currentColor" d="M12 21s-7.5-4.6-10-9.3C.4 8.5 2.2 4.5 6 4.5c2.1 0 3.4 1.1 4 2.1.6-1 1.9-2.1 4-2.1 3.8 0 5.6 4 4 7.2C19.5 16.4 12 21 12 21z"/>
    </svg>
    <span v-if="!compact">하트 보내기</span>
    <span v-if="!compact && fandom.heartsLeft !== null" class="left">{{ fandom.heartsLeft }}</span>
    <span v-for="b in bursts" :key="b" class="burst">+1</span>
  </button>
</template>

<style scoped>
.heart-btn {
  position: relative; display: inline-flex; align-items: center; gap: 6px;
  min-height: 38px; padding: 0 14px; border-radius: 999px; border: none; cursor: pointer;
  background: rgba(255, 79, 129, .14); color: var(--heart); font-weight: 800; font-size: 14px;
  transition: transform .1s, background .15s;
}
.heart-btn:hover { background: rgba(255, 79, 129, .24); }
.heart-btn:active { transform: scale(.94); }
.heart-btn.compact { min-height: 30px; padding: 0 9px; }
.left { background: var(--heart); color: #fff; border-radius: 999px; padding: 0 6px; font-size: 11px; }
.burst {
  position: absolute; left: 50%; top: -4px; pointer-events: none;
  color: var(--heart); font-weight: 900; font-size: 13px;
  animation: rise .9s ease-out forwards;
}
@keyframes rise { from { opacity: 1; transform: translate(-50%, 0); } to { opacity: 0; transform: translate(-50%, -28px); } }
</style>
