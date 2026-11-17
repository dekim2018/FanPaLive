<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { donationApi } from '../api/interactions'
import { errorMessage } from '../api/http'
import { formatWon } from '../lib/format'
import { useAuthStore } from '../stores/auth'

/**
 * 토스 결제창에서 돌아오는 페이지.
 * success: paymentKey, orderId, amount 를 서버로 보내 승인(서버가 토스에 직접 승인 요청 후 검증)
 *          결제사 응답을 받지 못하면 서버는 202 + UNKNOWN 을 준다. 이때는 "확인 중"으로 안내하고 서버가 조회로 확정한다.
 * fail   : 주문을 실패 처리
 */
const props = defineProps({ result: { type: String, required: true } })
const route = useRoute()
const auth = useAuthStore()
const state = ref('processing') // processing | done | unknown | failed
const message = ref('')
const donation = ref(null)
const returnTo = computed(() => {
  const v = route.query.returnTo
  // 외부 주소로 이동시키지 않도록 사이트 안 경로만 허용한다.
  return typeof v === 'string' && v.startsWith('/') && !v.startsWith('//') ? v : ''
})

onMounted(async () => {
  const { paymentKey, orderId, amount, code } = route.query
  if (props.result === 'success') {
    try {
      donation.value = await donationApi.confirm({ paymentKey, orderId, amount: Number(amount) })
      state.value = donation.value.status === 'UNKNOWN' ? 'unknown' : 'done'
    } catch (e) {
      state.value = 'failed'
      message.value = errorMessage(e, '후원을 확정하지 못했습니다.')
    }
  } else {
    state.value = 'failed'
    message.value = route.query.message || '결제가 취소되었거나 실패했습니다.'
    if (orderId && auth.isLoggedIn) {
      donationApi.cancel(orderId, code || 'PAYMENT_FAILED').catch(() => {})
    }
  }
})
</script>

<template>
  <div class="result card stack">
    <template v-if="state === 'processing'">
      <h1>후원을 확정하는 중입니다…</h1>
      <p class="muted">서버에서 결제 승인 결과와 금액을 확인하고 있습니다.</p>
    </template>
    <template v-else-if="state === 'done'">
      <h1>후원이 완료되었습니다</h1>
      <p v-if="donation.targetType === 'FAN_TARGET'">
        <strong>{{ donation.targetName }}</strong> 응원기금으로 <strong class="accent">{{ formatWon(donation.amount) }}</strong>을 보냈습니다.
      </p>
      <p v-else>
        <strong>{{ donation.streamerNickname }}</strong>님에게 <strong class="accent">{{ formatWon(donation.amount) }}</strong>을 후원했습니다.
        <span v-if="donation.targetName" class="muted">({{ donation.targetName }} 응원 점수에도 반영)</span>
      </p>
      <p v-if="donation.message" class="muted">"{{ donation.message }}"</p>
    </template>
    <template v-else-if="state === 'unknown'">
      <span class="badge unknown" style="align-self:flex-start">결제 확인 중</span>
      <h1>결제 결과를 확인하고 있습니다</h1>
      <p class="muted">결제사 응답이 늦어 아직 확정하지 못했습니다. 결제가 실제로 됐다면 1~2분 안에 자동으로 후원에 반영되고, 중복으로 결제되지 않습니다. 마이페이지의 후원 내역에서 확인할 수 있습니다.</p>
    </template>
    <template v-else>
      <h1>후원에 실패했습니다</h1>
      <p class="muted">{{ message }}</p>
    </template>
    <div class="row">
      <RouterLink v-if="returnTo" :to="returnTo" class="btn primary">돌아가기</RouterLink>
      <RouterLink to="/me" class="btn">후원 내역</RouterLink>
      <RouterLink to="/" class="btn">홈으로</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.result { max-width: 560px; margin: 40px auto; }
.accent { color: var(--accent-text); }
</style>
