<script setup>
import { tr } from '../i18n'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { broadcastApi } from '../api/broadcasts'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import TeamCrest from '../components/TeamCrest.vue'
import { readable } from '../lib/team'
import { formatDate } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'

/**
 * 방송 만들기/수정. 편파방이면 리그와 응원팀(필요하면 경기)을 고른다.
 * 응원팀과 경기는 서버가 fandom-service 에 다시 확인하므로 화면에서 고른 값이 그대로 저장되지는 않는다.
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const fandom = useFandomStore()
const toast = useToastStore()
const isEdit = computed(() => !!route.params.id)
const form = ref({ title: '', description: '' })
const sideMode = ref('SIDE')
const leagueCode = ref('KBO')
const targetId = ref(null)
const matchId = ref(null)
const targets = ref([])
const matches = ref([])
const originalTargetId = ref(null)
const agreed = ref(false)
const error = ref('')
const submitting = ref(false)

const selectedTarget = computed(() => targets.value.find((t) => t.id === targetId.value))
const teamMatches = computed(() => matches.value.filter((m) =>
  m.status !== 'FINISHED' && m.status !== 'CANCELED' && (m.home.id === targetId.value || m.away.id === targetId.value)))

async function loadTargets() {
  try {
    targets.value = await fandomApi.leagueTargets(leagueCode.value)
  } catch {
    targets.value = []
  }
}

async function loadMatches() {
  try {
    matches.value = await fandomApi.matches({ league: leagueCode.value })
  } catch {
    matches.value = []
  }
}

watch(leagueCode, () => {
  targetId.value = null
  matchId.value = null
  loadTargets()
  loadMatches()
})
watch(targetId, () => {
  if (!teamMatches.value.some((m) => m.id === matchId.value)) matchId.value = null
})

onMounted(async () => {
  if (isEdit.value) {
    try {
      const b = await broadcastApi.get(route.params.id)
      if (b.ownerId !== auth.user?.id) error.value = tr('방송 소유자만 수정할 수 있습니다.')
      form.value = { title: b.title, description: b.description }
      if (b.side) {
        leagueCode.value = b.side.leagueCode
        await Promise.all([loadTargets(), loadMatches()])
        targetId.value = b.side.targetId
        matchId.value = b.side.matchId
        originalTargetId.value = b.side.targetId
      } else {
        sideMode.value = 'NONE'
        await Promise.all([loadTargets(), loadMatches()])
      }
      agreed.value = true
    } catch (e) {
      error.value = errorMessage(e)
    }
    return
  }
  // 새 방송은 내 주 응원팀을 기본으로 고른다.
  await fandom.loadMyFans()
  const primary = fandom.myFans.find((f) => f.primary)
  if (primary) leagueCode.value = primary.leagueCode
  await Promise.all([loadTargets(), loadMatches()])
  if (primary) targetId.value = primary.target.id
})

async function submit() {
  error.value = ''
  if (!agreed.value) {
    error.value = tr('중계권 안내를 확인해 주세요.')
    return
  }
  if (sideMode.value === 'SIDE' && !targetId.value) {
    error.value = tr('응원팀을 선택해 주세요. 일반 방송이면 위에서 "일반 방송"을 고르세요.')
    return
  }
  const body = { title: form.value.title, description: form.value.description }
  if (sideMode.value === 'SIDE') {
    body.targetId = targetId.value
    body.matchId = matchId.value
  } else if (isEdit.value && originalTargetId.value) {
    body.clearTarget = true
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await broadcastApi.update(route.params.id, body)
      toast.success(tr('방송 정보를 수정했습니다.'))
      router.replace(`/broadcasts/${route.params.id}`)
    } else {
      const created = await broadcastApi.create(body)
      if (sideMode.value === 'SIDE' && !created.side) {
        toast.show(tr('응원팀 정보를 확인하지 못해 일반 방송으로 만들었습니다. 잠시 후 정보 수정에서 다시 지정해 주세요.'))
      } else {
        toast.success(tr('방송을 만들었습니다. 송출 방식을 선택해 방송을 시작하세요.'))
      }
      router.replace(`/studio/${created.id}`)
    }
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="form-page">
    <form class="card stack" @submit.prevent="submit">
      <h1>{{ isEdit ? tr('방송 정보 수정') : tr('편파중계 만들기') }}</h1>

      <div class="mode">
        <button type="button" :class="{ on: sideMode === 'SIDE' }" @click="sideMode = 'SIDE'">
          <strong>{{ tr('편파중계') }}</strong><span class="xsmall muted">{{ tr('응원팀을 정하고 한쪽만 응원하는 방송') }}</span>
        </button>
        <button type="button" :class="{ on: sideMode === 'NONE' }" @click="sideMode = 'NONE'">
          <strong>{{ tr('일반 방송') }}</strong><span class="xsmall muted">{{ tr('응원팀 없이 자유 주제') }}</span>
        </button>
      </div>

      <template v-if="sideMode === 'SIDE'">
        <label class="field">{{ tr('리그') }}
          <select name="league-code" v-model="leagueCode" class="input">
            <option v-for="l in fandom.leagues" :key="l.code" :value="l.code">{{ tr(l.sportName) }} / {{ tr(l.name) }}</option>
          </select>
        </label>
        <div class="field">
          <span>{{ tr('응원팀') }}</span>
          <div class="team-grid">
            <button v-for="t in targets" :key="t.id" type="button" class="team" :class="{ on: targetId === t.id }" :aria-pressed="targetId === t.id"
                    :style="targetId === t.id ? { borderColor: readable(t.colorPrimary, t.colorSecondary) } : {}" @click="targetId = t.id">
              <TeamCrest :name="t.name" :short-name="t.shortName" :color="t.colorPrimary" :color2="t.colorSecondary" :size="34" />
              <span class="ellipsis small">{{ tr(t.name) }}</span>
            </button>
          </div>
          <div v-if="!targets.length" class="small muted">{{ tr('이 리그에 등록된 팀이 없습니다.') }} <RouterLink to="/requests" class="link">{{ tr('팀/선수 신청') }}</RouterLink></div>
        </div>
        <label v-if="selectedTarget" class="field">{{ tr('중계할 경기 (선택)') }}
          <select name="match-id" v-model="matchId" class="input">
            <option :value="null">{{ tr('경기 지정 안 함') }}</option>
            <option v-for="m in teamMatches" :key="m.id" :value="m.id">
              {{ formatDate(m.startsAt) }} {{ tr(m.home.shortName || m.home.name) }} vs {{ tr(m.away.shortName || m.away.name) }}
            </option>
          </select>
          <span class="xsmall dim">{{ tr('경기를 지정하면 매치룸에 이 방송이 응원 방송으로 소개됩니다.') }}</span>
        </label>
      </template>

      <label class="field">{{ tr('방송 제목') }}
        <input name="title" autocomplete="off" v-model="form.title" class="input" maxlength="100" required :placeholder="tr('예) 두산 베어스 편파중계, 오늘은 잠실 직관 분위기로…')" />
      </label>
      <label class="field">{{ tr('방송 설명') }}
        <textarea name="description" autocomplete="off" v-model="form.description" class="input" maxlength="1000" :placeholder="tr('시청자에게 보여줄 설명…')"></textarea>
      </label>

      <label class="check">
        <input v-model="agreed" type="checkbox" />
        <span>{{ tr('경기 영상(TV 중계 화면 등)을 그대로 송출하지 않고, 내 화면과 목소리로 진행하는 응원 방송임을 확인했습니다. 중계권이 있는 영상의 재송출은 금지됩니다.') }}</span>
      </label>

      <div v-if="error" class="error-text">{{ error }}</div>
      <div class="row">
        <button class="btn primary" :disabled="submitting">{{ isEdit ? tr('저장') : tr('만들고 스튜디오로 이동') }}</button>
        <button type="button" class="btn" @click="router.back()">{{ tr('취소') }}</button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.form-page { max-width: 720px; margin: 0 auto; }
.mode { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.mode button {
  display: flex; flex-direction: column; gap: 2px; align-items: flex-start; text-align: left;
  padding: 12px; border-radius: 10px; border: 1px solid var(--border); background: var(--bg); color: var(--text); cursor: pointer;
}
.mode button.on { border-color: var(--accent-border); background: var(--accent-soft); }
.team-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 6px; }
.team {
  display: flex; align-items: center; gap: 8px; padding: 8px; border-radius: 10px;
  border: 2px solid var(--border); background: var(--bg); color: var(--text); cursor: pointer; min-width: 0;
}
.team.on { background: var(--bg-elev-2); }
.link { color: var(--text); font-weight: 700; text-decoration: underline; text-underline-offset: 3px; }
</style>
