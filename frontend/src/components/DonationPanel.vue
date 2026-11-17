<script setup>
import { computed, onMounted, ref } from 'vue'
import { donationApi, fundApi } from '../api/interactions'
import { errorMessage } from '../api/http'
import { requestCardPayment } from '../lib/toss'
import { formatDate, formatWon } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

/**
 * 후원 패널. 두 가지 후원을 지원한다.
 * - 방송자 후원: 방송 화면에서. 편파방이면 응원팀 랭킹 점수에도 들어간다.
 * - 응원금: 구단/선수 페이지 또는 편파방에서 그 팀 앞으로. 실제로 구단에 전달되지 않는 가상 기금이다.
 * 후원 대상은 서버가 방송 소유자/응원 대상 기준으로 다시 정하므로 화면 값은 신뢰하지 않는다.
 */
const props = defineProps({
  broadcast: { type: Object, default: null },
  target: { type: Object, default: null },
  events: { type: Object, default: null }
})

const PRESETS = [1000, 5000, 10000, 50000]
const auth = useAuthStore()
const toast = useToastStore()
const config = ref(null)
const amount = ref(5000)
const message = ref('')
const paying = ref(false)
const summary = ref({ totalAmount: 0, count: 0 })
const donations = ref([])
const highlight = ref(null)

const fundTarget = computed(() => props.target
  || (props.broadcast?.side ? { id: props.broadcast.side.targetId, name: props.broadcast.side.targetName } : null))
const mode = ref(props.broadcast ? 'BROADCAST' : 'FAN_TARGET')
const isOwner = computed(() => props.broadcast && auth.user?.id === props.broadcast.ownerId)
const canDonate = computed(() => {
  if (!auth.isLoggedIn || !config.value?.clientKey) return false
  if (mode.value === 'BROADCAST') return !isOwner.value
  return !!fundTarget.value
})
const returnTo = computed(() => (props.broadcast ? `/broadcasts/${props.broadcast.id}` : `/targets/${fundTarget.value?.id}`))

async function load() {
  try {
    const [cfg, sum, list] = await Promise.all([
      donationApi.config(),
      props.broadcast ? donationApi.summary(props.broadcast.id) : fundApi.summary(fundTarget.value.id),
      props.broadcast ? donationApi.list(props.broadcast.id, { size: 10 }) : fundApi.list(fundTarget.value.id, { size: 10 })
    ])
    config.value = cfg
    summary.value = sum
    donations.value = list.content
  } catch (e) {
    toast.error(errorMessage(e, '후원 정보를 불러오지 못했습니다.'))
  }
}

async function donate() {
  if (paying.value) return
  const value = Number(amount.value)
  if (!Number.isInteger(value) || value < config.value.minAmount || value > config.value.maxAmount) {
    toast.error(`후원 금액은 ${formatWon(config.value.minAmount)} ~ ${formatWon(config.value.maxAmount)} 사이로 입력해 주세요.`)
    return
  }
  paying.value = true
  try {
    const body = mode.value === 'BROADCAST'
      ? { targetType: 'BROADCAST', broadcastId: props.broadcast.id, amount: value, message: message.value }
      : { targetType: 'FAN_TARGET', targetId: fundTarget.value.id, amount: value, message: message.value }
    const order = await donationApi.createOrder(body)
    await requestCardPayment({ clientKey: config.value.clientKey, customerKey: order.customerKey, order, returnTo: returnTo.value })
  } catch (e) {
    if (e?.code !== 'USER_CANCEL') toast.error(errorMessage(e, '결제를 시작하지 못했습니다.'))
  } finally {
    paying.value = false
  }
}

if (props.events) {
  props.events.on('donation.completed', (d) => {
    donations.value = [d, ...donations.value].slice(0, 10)
    highlight.value = d
    setTimeout(() => { if (highlight.value === d) highlight.value = null }, 6000)
  })
  props.events.on('donation.summary', (s) => { summary.value = s })
}

onMounted(load)
</script>

<template>
  <section class="card stack">
    <div class="row">
      <h3 style="margin:0">{{ broadcast ? '후원' : '응원금' }}</h3>
      <span class="spacer"></span>
      <span class="small muted">총 {{ summary.count }}건</span>
      <strong class="total num">{{ formatWon(summary.totalAmount) }}</strong>
    </div>

    <transition name="pop">
      <div v-if="highlight" class="alert" role="status">
        <strong>{{ highlight.donorNickname }}</strong>님이 {{ formatWon(highlight.amount) }} 후원!
        <div v-if="highlight.message" class="small">"{{ highlight.message }}"</div>
      </div>
    </transition>

    <div v-if="broadcast && fundTarget && auth.isLoggedIn" class="mode" role="group" aria-label="후원 대상 선택">
      <button type="button" :class="{ on: mode === 'BROADCAST' }" :aria-pressed="mode === 'BROADCAST'" @click="mode = 'BROADCAST'">
        방송자 후원<span class="xsmall">{{ broadcast.ownerNickname }}</span>
      </button>
      <button type="button" :class="{ on: mode === 'FAN_TARGET' }" :aria-pressed="mode === 'FAN_TARGET'" @click="mode = 'FAN_TARGET'">
        응원금<span class="xsmall">{{ fundTarget.name }}</span>
      </button>
    </div>

    <div v-if="canDonate" class="stack">
      <div class="small muted">
        <template v-if="mode === 'BROADCAST'">
          후원 대상: <strong>{{ broadcast.ownerNickname }}</strong>
          <span v-if="fundTarget"> ({{ fundTarget.name }} 응원 점수에도 반영)</span>
        </template>
        <template v-else>
          <strong>{{ fundTarget.name }}</strong> 응원금. 랭킹 점수(후원금 100원당 1점)에 반영되는 팬 활동 기록이며 실제 구단에 전달되지 않습니다.
        </template>
      </div>
      <div class="row">
        <button v-for="p in PRESETS" :key="p" type="button" class="btn sm" :class="{ on: amount === p }" :aria-pressed="amount === p" @click="amount = p">
          {{ formatWon(p) }}
        </button>
      </div>
      <label class="field">후원 금액(원)
        <input v-model.number="amount" class="input num" type="number" name="amount" inputmode="numeric" autocomplete="off"
               :min="config.minAmount" :max="config.maxAmount" step="1000" />
      </label>
      <label class="field">응원 메시지 (선택)
        <input v-model="message" class="input" name="donation-message" autocomplete="off" maxlength="200" placeholder="오늘 꼭 이기자…" />
      </label>
      <button type="button" class="btn primary block" :disabled="paying" @click="donate">
        {{ paying ? '결제창 여는 중…' : `${formatWon(amount)} ${mode === 'BROADCAST' ? '후원하기' : '응원금 보내기'} (테스트 결제)` }}
      </button>
      <div class="xsmall muted">토스페이먼츠 테스트 모드라 실제로 결제되지 않습니다.</div>
    </div>
    <div v-else-if="!auth.isLoggedIn" class="small muted">로그인하면 후원할 수 있습니다.</div>
    <div v-else-if="mode === 'BROADCAST' && isOwner" class="small muted">내 방송에는 후원할 수 없습니다.</div>
    <div v-else-if="config && !config.clientKey" class="small muted">결제 키가 설정되지 않아 후원을 사용할 수 없습니다.</div>

    <div v-if="donations.length" class="stack list">
      <div v-for="d in donations" :key="d.id" class="item small">
        <div class="row">
          <strong>{{ d.donorNickname }}</strong>
          <span class="accent num">{{ formatWon(d.amount) }}</span>
          <span v-if="d.targetType === 'FAN_TARGET'" class="chip">응원금</span>
          <span class="spacer"></span>
          <span class="muted">{{ formatDate(d.approvedAt) }}</span>
        </div>
        <div v-if="d.message" class="muted">{{ d.message }}</div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.total { color: var(--accent-text); }
.accent { color: var(--accent-text); font-weight: 800; }
.alert { background: var(--accent-soft); border: 1px solid var(--accent-border); padding: 10px 12px; border-radius: 10px; }
.mode { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.mode button {
  display: flex; flex-direction: column; align-items: flex-start; gap: 2px; padding: 8px 10px;
  border-radius: 8px; border: 1px solid var(--border-strong); background: var(--bg-elev); color: var(--text-sub); min-height: 44px;
  font-weight: 800; font-size: 13px; cursor: pointer; text-align: left;
}
.mode button:hover { background: var(--bg-elev-2); }
.mode button.on { border-color: var(--accent-border); color: var(--accent-text); background: var(--accent-soft); }
.mode .xsmall { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; }
.list { gap: 8px; max-height: 240px; overflow-y: auto; }
.item { padding: 8px 0; border-top: 1px solid var(--border); }
.pop-enter-active, .pop-leave-active { transition: opacity .25s, transform .25s; }
.pop-enter-from, .pop-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
