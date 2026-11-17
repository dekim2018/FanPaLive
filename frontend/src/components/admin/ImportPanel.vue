<script setup>
import { computed, ref } from 'vue'
import { tr } from '../../i18n'
import { adminApi } from '../../api/fandom'
import { errorMessage } from '../../api/http'
import { formatDate } from '../../lib/format'
import { useFandomStore } from '../../stores/fandom'
import { useToastStore } from '../../stores/toast'

/**
 * 경기 일정 가져오기. CSV 파일을 고르거나 엑셀에서 복사해 붙여 넣는다.
 * 먼저 미리보기로 각 줄이 새 경기, 중복, 오류 중 무엇인지 확인하고 등록한다(같은 파일을 다시 올려도 중복은 건너뜀).
 */
const fandom = useFandomStore()
const toast = useToastStore()
const leagueCode = ref('KBO')
const payload = ref('')
const result = ref(null)
const busy = ref(false)
const SAMPLE = '날짜,시각,홈팀,원정팀,구장\n2026-04-01,18:30,두산 베어스,LG 트윈스,잠실\n2026-04-02,18:30,두산,LG,잠실'
const STATUS = { NEW: tr('새 경기'), DUPLICATE: tr('중복'), ERROR: tr('오류'), CREATED: tr('등록함') }
const newCount = computed(() => result.value?.rows.filter((r) => r.status === 'NEW').length || 0)

async function readFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 100_000) {
    toast.error(tr('파일이 너무 큽니다. 100KB 이하 CSV 를 올려 주세요.'))
    return
  }
  payload.value = await file.text()
  preview()
}

async function preview() {
  if (!payload.value.trim()) return
  busy.value = true
  try {
    result.value = await adminApi.previewImport(leagueCode.value, payload.value)
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    busy.value = false
  }
}

async function run() {
  if (!confirm(tr('새 경기 {0}개를 등록할까요?', newCount.value))) return
  busy.value = true
  try {
    result.value = await adminApi.runImport(leagueCode.value, payload.value)
    toast.success(tr('{0}경기를 등록했습니다.', result.value.created))
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="stack">
    <div class="card stack">
      <h3 style="margin:0">{{ tr('경기 일정 가져오기') }}</h3>
      <p class="small muted" style="margin:0">{{ tr('한 줄에 한 경기씩 "날짜, 시각, 홈팀, 원정팀, 구장" 순서로 적습니다. 팀은 정식 이름이나 약칭 모두 됩니다. 한 번에 500경기까지 올릴 수 있어요.') }}</p>
      <div class="row">
        <select v-model="leagueCode" name="import-league" class="input w" :aria-label="tr('리그')">
          <option v-for="l in fandom.leagues" :key="l.code" :value="l.code">{{ tr(l.name) }}</option>
        </select>
        <label class="btn sm ghost file">
          {{ tr('CSV 파일 고르기') }}
          <input type="file" accept=".csv,.tsv,.txt,text/csv,text/plain" class="sr-only" @change="readFile" />
        </label>
        <button type="button" class="btn sm" @click="payload = SAMPLE; preview()">{{ tr('예시 넣기') }}</button>
      </div>
      <textarea v-model="payload" name="import-payload" class="input mono" rows="7" :aria-label="tr('일정 내용')"
                :placeholder="tr('2026-04-01,18:30,두산 베어스,LG 트윈스,잠실…')"></textarea>
      <div class="row">
        <button type="button" class="btn" :disabled="busy || !payload.trim()" @click="preview">{{ tr('미리보기') }}</button>
        <button type="button" class="btn primary" :disabled="busy || !newCount" @click="run">{{ tr('새 경기 {0}개 등록', newCount) }}</button>
      </div>
    </div>
    <div v-if="result" class="card stack">
      <div class="row small">
        <strong>{{ tr('{0}에서 읽은 결과', tr(result.source)) }}</strong>
        <span class="spacer"></span>
        <span>{{ tr('등록 {0}, 중복 {1}, 오류 {2}', result.created, result.duplicates, result.errors) }}</span>
      </div>
      <table class="table">
        <thead><tr><th>{{ tr('줄') }}</th><th>{{ tr('일시') }}</th><th>{{ tr('경기') }}</th><th>{{ tr('상태') }}</th></tr></thead>
        <tbody>
          <tr v-for="r in result.rows" :key="r.line">
            <td class="num">{{ r.line }}</td>
            <td class="small">{{ r.startsAt ? formatDate(r.startsAt) : '-' }}</td>
            <td class="small">{{ r.home ? `${tr(r.home)} vs ${tr(r.away)}` : '-' }}<span v-if="r.venue" class="muted">, {{ r.venue }}</span></td>
            <td class="small">
              <span class="st" :class="r.status.toLowerCase()">{{ STATUS[r.status] }}</span>
              <span v-if="r.message" class="muted"> {{ tr(r.message) }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.w { width: auto; min-width: 160px; }
.file { cursor: pointer; }
.file:focus-within { box-shadow: var(--focus); }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 13px; }
.st { font-weight: 700; }
.st.new, .st.created { color: var(--ok); }
.st.duplicate { color: var(--text-sub); }
.st.error { color: var(--danger); }
</style>
