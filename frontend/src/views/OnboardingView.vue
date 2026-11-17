<script setup>
import { tr } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import TeamCrest from '../components/TeamCrest.vue'
import { readable, tint } from '../lib/team'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'
import TrText from '../components/TrText.vue'

/**
 * 가입 직후 응원팀 고르기. 리그마다 주 응원팀은 하나, 팔로우는 여러 개 가능하다.
 * 주 응원팀은 랭킹 점수(100점)와 채팅 배지에 쓰이고, 30일에 한 번만 바꿀 수 있다.
 */
const router = useRouter()
const fandom = useFandomStore()
const toast = useToastStore()
const league = ref('KBO')
const targets = ref([])
const saving = ref(false)
// 리그별 선택 { 리그 코드: { primary, follows } }. 탭을 옮겨도 다른 리그에서 고른 팀이 사라지지 않게 리그마다 따로 둔다.
const picks = ref({})

const currentPrimary = computed(() => fandom.myFans.find((f) => f.primary && f.leagueCode === league.value))

/** 서버에 저장된 그 리그의 응원 상태(저장할 때 무엇이 바뀌었는지 비교하는 기준) */
function saved(code) {
  const mine = fandom.myFans.filter((f) => f.leagueCode === code)
  return {
    primary: mine.find((f) => f.primary)?.target.id ?? null,
    follows: new Set(mine.filter((f) => !f.primary).map((f) => f.target.id))
  }
}

const current = computed(() => picks.value[league.value] ?? saved(league.value))
const primaryId = computed(() => current.value.primary)
const follows = computed(() => current.value.follows)

async function loadTargets() {
  if (!picks.value[league.value]) picks.value[league.value] = saved(league.value)
  try {
    targets.value = await fandomApi.leagueTargets(league.value)
  } catch {
    targets.value = []
  }
}

function toggle(t) {
  const pick = picks.value[league.value]
  if (pick.primary === null) {
    pick.primary = t.id
  } else if (pick.primary === t.id) {
    pick.primary = null
  } else if (pick.follows.has(t.id)) {
    pick.follows.delete(t.id)
  } else {
    pick.follows.add(t.id)
  }
  pick.follows.delete(pick.primary)
}

/**
 * 고른 모든 리그를 저장한다. 새로 고른 팀은 등록하고, 다시 눌러 해제한 팀은 탈퇴한다.
 * 주 응원팀을 정한 지 30일이 안 됐으면 서버가 바꾸기와 해제를 막고 이유를 알려 준다.
 */
async function save() {
  saving.value = true
  const failures = []
  try {
    for (const [code, pick] of Object.entries(picks.value)) {
      const before = saved(code)
      try {
        if (pick.primary && pick.primary !== before.primary) await fandomApi.joinFan(pick.primary, true)
        for (const id of pick.follows) {
          if (!before.follows.has(id)) await fandomApi.joinFan(id, false)
        }
        const kept = new Set([pick.primary, ...pick.follows])
        for (const id of [before.primary, ...before.follows]) {
          if (id && !kept.has(id)) await fandomApi.leaveFan(id)
        }
      } catch (e) {
        failures.push(errorMessage(e))
      }
    }
    await fandom.loadMyFans(true)
    if (failures.length) {
      // 일부만 저장됐을 수 있으니 서버 상태로 다시 맞춰 보여 준다.
      toast.error(failures[0])
      picks.value = {}
      await loadTargets()
      return
    }
    toast.success(tr('응원팀을 저장했습니다.'))
    router.replace('/')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await fandom.loadMyFans(true)
  loadTargets()
})
</script>

<template>
  <div class="onb">
    <h1>{{ tr('어느 팀을 응원하세요?') }}</h1>
    <p class="muted"><TrText :text="tr('처음 누른 팀이 {0}(★), 그다음부터는 팔로우가 됩니다. 다시 누르면 해제돼요.')"><template #p0><strong>{{ tr('주 응원팀') }}</strong></template></TrText></p>
    <div class="pill-tabs">
      <button v-for="l in fandom.leagues" :key="l.code" :class="{ active: league === l.code }" @click="league = l.code; loadTargets()">{{ tr(l.name) }}</button>
    </div>
    <div v-if="currentPrimary" class="small muted note">
      <TrText :text="tr('이 리그의 주 응원팀은 {0}입니다.')"><template #p0><strong>{{ tr(currentPrimary.target.name) }}</strong></template></TrText>
      <span v-if="currentPrimary.primaryChangeAvailableAt">{{ tr('바꾸려면 30일이 지나야 합니다.') }}</span>
    </div>
    <div class="teams">
      <button v-for="t in targets" :key="t.id" class="team"
              :class="{ primary: primaryId === t.id, follow: follows.has(t.id) }" :aria-pressed="primaryId === t.id || follows.has(t.id)"
              :style="primaryId === t.id || follows.has(t.id) ? { borderColor: readable(t.colorPrimary, t.colorSecondary), background: tint(t.colorPrimary, 0.12) } : {}"
              @click="toggle(t)">
        <span v-if="primaryId === t.id" class="mark">{{ tr('★ 주 응원') }}</span>
        <span v-else-if="follows.has(t.id)" class="mark f">{{ tr('팔로우') }}</span>
        <TeamCrest :name="t.name" :short-name="t.shortName" :color="t.colorPrimary" :color2="t.colorSecondary" :size="64" />
        <strong class="ellipsis">{{ tr(t.name) }}</strong>
      </button>
    </div>
    <div v-if="!targets.length" class="empty">
      {{ tr('이 리그에는 아직 등록된 팀이 없어요.') }} <RouterLink :to="{ path: '/requests', query: { league } }" class="link">{{ tr('신청하기') }}</RouterLink>
    </div>
    <div class="row actions">
      <button class="btn primary" :disabled="saving" @click="save">{{ tr('저장하고 시작하기') }}</button>
      <RouterLink to="/" class="btn ghost">{{ tr('나중에') }}</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.onb { max-width: 960px; margin: 0 auto; }
.note { margin-bottom: 12px; }
.teams { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; }
.team {
  position: relative; display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 20px 10px 14px;
  border-radius: 12px; border: 2px solid var(--border); background: var(--bg-elev); color: var(--text); cursor: pointer; min-width: 0;
  transition: transform .1s;
}
.team:hover { transform: translateY(-2px); }
.team strong { max-width: 100%; font-size: 14px; }
.mark { position: absolute; top: 6px; right: 8px; font-size: 11px; font-weight: 900; color: var(--gold); }
.mark.f { color: var(--text-sub); }
.actions { margin-top: 24px; justify-content: center; }
.link { color: var(--text); font-weight: 700; text-decoration: underline; text-underline-offset: 3px; }
</style>
