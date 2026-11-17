<script setup>
import { tr } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import TeamCrest from '../components/TeamCrest.vue'
import { contrast } from '../lib/team'
import { formatDate } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'

/**
 * 해외 구단/선수 신청. 같은 리그에 같은 이름(공백, 대소문자, 전각 문자 차이 무시)으로 대기 중인 신청이 있으면
 * 새로 만들지 않고 그 신청에 "나도 원해요" 투표로 합친다. 운영자가 승인하면 응원 대상으로 공개된다.
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const fandom = useFandomStore()
const toast = useToastStore()

const openLeagues = computed(() => fandom.leagues.filter((l) => l.openForRequest))
const form = ref({
  leagueCode: typeof route.query.league === 'string' ? route.query.league : '',
  name: '', nameEn: '', shortName: '', colorPrimary: '#1d428a', colorSecondary: '#ffffff', description: '', referenceUrl: ''
})
const filterLeague = ref('')
const colorRatio = computed(() => contrast(form.value.colorPrimary, form.value.colorSecondary))
const list = ref([])
const mine = ref([])
const submitting = ref(false)
const error = ref('')
const STATUS = { PENDING: tr('심사 중'), APPROVED: tr('승인'), REJECTED: tr('반려') }

async function load() {
  try {
    const params = { size: 30 }
    if (filterLeague.value) params.league = filterLeague.value
    list.value = (await fandomApi.openRequests(params)).content
  } catch {
    list.value = []
  }
  if (auth.isLoggedIn) {
    try { mine.value = await fandomApi.myRequests() } catch { mine.value = [] }
  }
}

async function submit() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  error.value = ''
  submitting.value = true
  try {
    const res = await fandomApi.requestTarget(form.value)
    toast.success(res.data.merged ? tr('같은 신청이 이미 있어 "나도 원해요"로 합쳤습니다.') : tr('신청했습니다. 운영자 승인 후 공개됩니다.'))
    form.value = { ...form.value, name: '', nameEn: '', shortName: '', description: '', referenceUrl: '' }
    load()
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}

async function vote(r) {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const updated = await fandomApi.voteRequest(r.id)
    r.voteCount = updated.voteCount
    toast.success(tr('함께 신청했습니다.'))
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

onMounted(() => {
  if (!form.value.leagueCode && openLeagues.value.length) form.value.leagueCode = openLeagues.value[0].code
  load()
})
</script>

<template>
  <div>
    <h1>{{ tr('팀/선수 신청') }}</h1>
    <p class="muted small">{{ tr('목록에 없는 해외 구단이나 선수를 신청하세요. 운영자가 확인 후 공개합니다. KBO 10개 구단은 이미 등록되어 있어요.') }}</p>
    <div v-if="!fandom.feature('targetRequests')" class="empty">{{ tr('신청 기능이 잠시 꺼져 있습니다.') }}</div>
    <div v-else class="layout">
      <form class="card stack" @submit.prevent="submit">
        <h2 style="margin:0">{{ tr('새로 신청하기') }}</h2>
        <label class="field">{{ tr('리그') }}
          <select name="league-code" v-model="form.leagueCode" class="input" required>
            <option v-for="l in openLeagues" :key="l.code" :value="l.code">{{ tr(l.sportName) }} / {{ tr(l.name) }}</option>
          </select>
        </label>
        <label class="field">{{ tr('이름 (한글)') }}
          <input name="name" autocomplete="off" v-model="form.name" class="input" required minlength="2" maxlength="40" :placeholder="tr('예) LA 다저스, 손흥민…')" />
        </label>
        <div class="row nowrap">
          <label class="field" style="flex:2">{{ tr('영문 이름') }}
            <input name="name-en" autocomplete="off" v-model="form.nameEn" class="input" maxlength="60" placeholder="Los Angeles Dodgers" />
          </label>
          <label class="field" style="flex:1">{{ tr('약칭 (6자)') }}
            <input name="short-name" autocomplete="off" v-model="form.shortName" class="input" maxlength="6" :placeholder="tr('다저스')" />
          </label>
        </div>
        <div class="row nowrap">
          <label class="field" style="flex:1">{{ tr('배경색 (진한 색)') }}
            <input name="color-primary" autocomplete="off" v-model="form.colorPrimary" class="input color" type="color" />
          </label>
          <label class="field" style="flex:1">{{ tr('글자색 (연한 색)') }}
            <input name="color-secondary" autocomplete="off" v-model="form.colorSecondary" class="input color" type="color" />
          </label>
          <div class="preview">
            <TeamCrest :name="form.name" :short-name="form.shortName || form.name" :color="form.colorPrimary" :color2="form.colorSecondary" :size="48" />
          </div>
        </div>
        <div class="xsmall" :class="colorRatio >= 4.5 ? 'muted' : 'warn-text'" role="status">
          {{ tr('배경과 글자 대비 {0}:1', colorRatio.toFixed(1)) }}
          <template v-if="colorRatio < 4.5">. {{ tr('4.5:1 보다 낮아 글자가 잘 안 보입니다. 공개될 때는 흰색이나 검정 글자로 바뀝니다.') }}</template>
        </div>
        <label class="field">{{ tr('소개') }}
          <textarea name="description" autocomplete="off" v-model="form.description" class="input" maxlength="300" :placeholder="tr('응원하는 이유, 간단한 소개…')"></textarea>
        </label>
        <label class="field">{{ tr('참고 링크 (공식 사이트, 위키 등)') }}
          <input name="reference-url" autocomplete="off" type="url" inputmode="url" spellcheck="false" v-model="form.referenceUrl" class="input" maxlength="300" placeholder="https://…" />
        </label>
        <div v-if="error" class="error-text">{{ error }}</div>
        <button class="btn primary" :disabled="submitting">{{ auth.isLoggedIn ? tr('신청하기') : tr('로그인하고 신청하기') }}</button>
      </form>

      <div class="stack">
        <div class="row">
          <h2 style="margin:0">{{ tr('심사 중인 신청') }}</h2>
          <span class="spacer"></span>
          <select name="filter-league" :aria-label="tr('리그로 거르기')" v-model="filterLeague" class="input sel" @change="load">
            <option value="">{{ tr('전체 리그') }}</option>
            <option v-for="l in openLeagues" :key="l.code" :value="l.code">{{ tr(l.name) }}</option>
          </select>
        </div>
        <div v-if="!list.length" class="empty">{{ tr('심사 중인 신청이 없습니다.') }}</div>
        <div v-for="r in list" :key="r.id" class="card row nowrap req">
          <TeamCrest :name="r.name" :short-name="r.shortName || r.name" :color="r.colorPrimary || '#3a3d44'" :color2="r.colorSecondary" :size="40" />
          <div style="min-width:0; flex:1">
            <strong class="ellipsis" style="display:block">{{ tr(r.name) }}</strong>
            <span class="xsmall muted">{{ tr('{0}, {1}님 신청, {2}', tr(r.leagueName), r.requesterNickname, formatDate(r.createdAt)) }}</span>
          </div>
          <button class="btn sm" @click="vote(r)">{{ tr('나도 원해요 {0}', r.voteCount) }}</button>
        </div>

        <template v-if="mine.length">
          <h2 style="margin:12px 0 0">{{ tr('내 신청') }}</h2>
          <div v-for="r in mine" :key="'m' + r.id" class="card row nowrap req">
            <span class="badge" :class="r.status === 'APPROVED' ? 'live' : r.status === 'REJECTED' ? 'ended' : 'ready'">{{ STATUS[r.status] }}</span>
            <div style="min-width:0; flex:1">
              <strong class="ellipsis" style="display:block">{{ tr(r.name) }}</strong>
              <span v-if="r.rejectReason" class="xsmall muted">{{ tr('반려 사유: {0}', r.rejectReason) }}</span>
            </div>
            <RouterLink v-if="r.targetId" :to="`/targets/${r.targetId}`" class="btn sm primary">{{ tr('보러 가기') }}</RouterLink>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout { display: grid; grid-template-columns: minmax(0, 440px) minmax(0, 1fr); gap: 20px; align-items: start; }
.color { padding: 4px; height: 42px; }
.warn-text { color: var(--warn); font-weight: 700; }
.preview { display: flex; align-items: flex-end; padding-bottom: 2px; }
.sel { width: auto; min-height: 34px; padding: 4px 8px; font-size: 13px; }
.req { gap: 12px; }
@media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
</style>
