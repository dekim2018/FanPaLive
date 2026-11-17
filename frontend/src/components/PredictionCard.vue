<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { tr } from '../i18n'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import { gaugeColors, readable, teamFg } from '../lib/team'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import TrText from './TrText.vue'

/**
 * 승부 예측. 경기 시작 전까지 이길 팀을 고르고 바꿀 수 있다.
 * 맞히면 팬 포인트 30점, 무승부와 취소는 무효. 돈이 오가지 않는 무료 예측이다.
 */
const props = defineProps({
  match: { type: Object, required: true }
})
const auth = useAuthStore()
const toast = useToastStore()
const view = ref(null)
const saving = ref(false)

const colors = computed(() => gaugeColors(props.match.home, props.match.away))
const total = computed(() => (view.value ? view.value.home + view.value.away : 0))
const mine = computed(() => view.value?.mine)
const RESULT = { HIT: tr('적중! 팬 포인트 30점을 받았어요.'), MISS: tr('아쉽게 빗나갔어요.'), VOID: tr('무승부나 취소라 무효 처리됐어요.') }

async function load() {
  try {
    view.value = await fandomApi.prediction(props.match.id)
  } catch {
    view.value = null
  }
}

async function pick(side) {
  if (saving.value || !view.value?.open) return
  saving.value = true
  try {
    view.value = await fandomApi.predict(props.match.id, side)
    toast.success(tr('{0} 승리를 예측했어요. 경기 시작 전까지 바꿀 수 있어요.', side === 'HOME' ? tr(props.match.home.name) : tr(props.match.away.name)))
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    saving.value = false
  }
}

function btnStyle(team, side) {
  const bg = side === 'HOME' ? colors.value.home : colors.value.away
  return mine.value?.pick === side ? { background: bg, color: teamFg(bg, team.colorSecondary), borderColor: bg } : { borderColor: bg }
}

onMounted(load)
watch(() => props.match.status, load)
</script>

<template>
  <section class="predict card" :aria-labelledby="`predict-${match.id}`">
    <div class="row">
      <h2 :id="`predict-${match.id}`" class="title">{{ tr('승부 예측') }}</h2>
      <span class="xsmall muted">{{ tr('맞히면 팬 포인트 +30, 돈은 오가지 않아요') }}</span>
    </div>
    <template v-if="view">
      <div class="picks">
        <button type="button" class="pick" :style="btnStyle(match.home, 'HOME')" :aria-pressed="mine?.pick === 'HOME'"
                :disabled="!view.open || !auth.isLoggedIn || saving" @click="pick('HOME')">
          <span class="xsmall">{{ tr('홈') }}</span><strong>{{ tr('{0} 승', tr(match.home.shortName || match.home.name)) }}</strong>
        </button>
        <button type="button" class="pick" :style="btnStyle(match.away, 'AWAY')" :aria-pressed="mine?.pick === 'AWAY'"
                :disabled="!view.open || !auth.isLoggedIn || saving" @click="pick('AWAY')">
          <span class="xsmall">{{ tr('원정') }}</span><strong>{{ tr('{0} 승', tr(match.away.shortName || match.away.name)) }}</strong>
        </button>
      </div>
      <div v-if="total" class="dist">
        <div class="bar" role="img" :aria-label="tr('예측 분포 홈 {0}퍼센트, 원정 {1}퍼센트', view.homePercent, 100 - view.homePercent)">
          <div :style="{ width: view.homePercent + '%', background: colors.home }"></div>
          <div :style="{ flex: 1, background: colors.away }"></div>
        </div>
        <div class="row nowrap xsmall muted num">
          <span :style="{ color: readable(colors.home) }">{{ view.homePercent }}%</span>
          <span class="spacer"></span>
          <span>{{ tr('{0}명 참여', total) }}</span>
          <span class="spacer"></span>
          <span :style="{ color: readable(colors.away) }">{{ 100 - view.homePercent }}%</span>
        </div>
      </div>
      <p v-if="mine?.result" class="result small" :class="mine.result.toLowerCase()" role="status">{{ RESULT[mine.result] }}</p>
      <p v-else-if="!auth.isLoggedIn" class="small muted">
        <TrText :text="tr('{0}하면 예측에 참여할 수 있어요.')"><template #p0><RouterLink :to="{ name: 'login', query: { redirect: $route.fullPath } }" class="link">{{ tr('로그인') }}</RouterLink></template></TrText>
      </p>
      <p v-else-if="!view.open" class="small muted">{{ mine ? tr('예측이 마감됐어요. 경기가 끝나면 결과를 알려 드려요.') : tr('경기가 시작되어 예측이 마감됐어요.') }}</p>
    </template>
    <p v-else class="small muted">{{ tr('예측 정보를 불러오지 못했습니다.') }}</p>
  </section>
</template>

<style scoped>
.predict { display: flex; flex-direction: column; gap: 12px; }
.title { margin: 0; font-size: 17px; font-weight: 800; }
.picks { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.pick {
  display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 12px; border-radius: 10px;
  border: 2px solid var(--border); background: var(--bg-elev); color: var(--text); cursor: pointer; font-family: inherit; font-size: 15px;
}
.pick:disabled { cursor: default; }
.pick:not(:disabled):hover { background: var(--bg-elev-2); }
.pick[aria-pressed="true"]:not(:disabled):hover { filter: brightness(1.05); }
.dist { display: flex; flex-direction: column; gap: 4px; }
.bar { display: flex; height: 10px; border-radius: 999px; overflow: hidden; background: var(--bg-elev-2); }
.bar > div:first-child { border-right: 2px solid var(--bg-elev); transition: width .4s ease; }
.result { margin: 0; padding: 8px 10px; border-radius: 8px; font-weight: 700; }
.result.hit { background: #e7f6ee; color: var(--ok); }
.result.miss, .result.void { background: var(--bg-elev-2); color: var(--text-sub); }
.link { color: var(--text); font-weight: 700; text-decoration: underline; text-underline-offset: 3px; }
</style>
