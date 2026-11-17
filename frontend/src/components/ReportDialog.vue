<script setup>
import { nextTick, ref, watch } from 'vue'
import { tr } from '../i18n'
import { reportApi } from '../api/interactions'
import { errorMessage } from '../api/http'
import { useToastStore } from '../stores/toast'

/**
 * 방송, 댓글 신고 창. 같은 대상을 두 번 신고하면 서버가 막는다.
 * 운영자가 확인 후 댓글 숨김, 방송 중단으로 처리한다.
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  type: { type: String, required: true },
  targetId: { type: [Number, String], default: null },
  summary: { type: String, default: '' }
})
const emit = defineEmits(['close'])
const toast = useToastStore()
const REASONS = [
  { code: 'RIGHTS_VIOLATION', label: tr('중계권 영상 재송출'), hint: tr('TV 중계 화면을 그대로 보여 주는 방송') },
  { code: 'ABUSE', label: tr('욕설, 비하, 혐오 표현'), hint: tr('상대 팀이나 선수를 모욕하는 말') },
  { code: 'SPAM', label: tr('도배, 광고'), hint: tr('같은 말 반복, 외부 홍보') },
  { code: 'ETC', label: tr('기타'), hint: tr('그 밖의 운영 정책 위반') }
]
const reason = ref('')
const detail = ref('')
const sending = ref(false)
const dialog = ref(null)

watch(() => props.open, async (v) => {
  if (v) {
    reason.value = props.type === 'BROADCAST' ? 'RIGHTS_VIOLATION' : 'ABUSE'
    detail.value = ''
    await nextTick()
    dialog.value?.querySelector('input')?.focus()
  }
})

async function submit() {
  if (!reason.value || sending.value) return
  sending.value = true
  try {
    await reportApi.report(props.type, Number(props.targetId), reason.value, detail.value)
    toast.success(tr('신고했습니다. 운영자가 확인 후 조치합니다.'))
    emit('close')
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div v-if="open" class="modal-scrim" @click.self="emit('close')" @keydown.esc="emit('close')">
    <form ref="dialog" class="modal card stack" role="dialog" aria-modal="true" aria-labelledby="report-title" @submit.prevent="submit">
      <h2 id="report-title" style="margin:0; font-size:18px">{{ type === 'BROADCAST' ? tr('방송 신고') : tr('댓글 신고') }}</h2>
      <p v-if="summary" class="small muted quote">{{ summary }}</p>
      <fieldset class="reasons">
        <legend class="small muted">{{ tr('신고 사유') }}</legend>
        <label v-for="r in REASONS" :key="r.code" class="reason" :class="{ on: reason === r.code }">
          <input v-model="reason" type="radio" name="report-reason" :value="r.code" />
          <span><strong>{{ r.label }}</strong><span class="xsmall muted">{{ r.hint }}</span></span>
        </label>
      </fieldset>
      <label class="field">{{ tr('자세한 내용 (선택)') }}
        <textarea v-model="detail" name="report-detail" class="input" maxlength="200" rows="3" :placeholder="tr('어떤 장면이나 말이 문제인지 적어 주세요…')"></textarea>
      </label>
      <div class="row">
        <span class="spacer"></span>
        <button type="button" class="btn" @click="emit('close')">{{ tr('취소') }}</button>
        <button type="submit" class="btn danger" :disabled="sending">{{ tr('신고하기') }}</button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.modal-scrim { position: fixed; inset: 0; background: var(--overlay); display: grid; place-items: center; z-index: 90; padding: 16px; }
.modal { width: min(440px, 100%); max-height: 90vh; overflow-y: auto; }
.quote { border-left: 3px solid var(--border-strong); padding-left: 10px; margin: 0; overflow-wrap: anywhere; }
.reasons { border: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 6px; }
.reasons legend { margin-bottom: 6px; }
.reason { display: flex; gap: 10px; align-items: flex-start; padding: 10px; border: 1px solid var(--border); border-radius: 10px; cursor: pointer; }
.reason.on { border-color: var(--accent-border); background: var(--accent-soft); }
.reason input { margin-top: 3px; accent-color: var(--accent); }
.reason span { display: flex; flex-direction: column; gap: 2px; }
</style>
