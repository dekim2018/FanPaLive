<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import TeamCrest from '../components/TeamCrest.vue'
import { readable, tint } from '../lib/team'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'

/**
 * 가입 직후 응원팀 고르기. 리그마다 주 응원팀은 하나, 팔로우는 여러 개 가능하다.
 * 주 응원팀은 랭킹 점수(100점)와 채팅 배지에 쓰이고, 30일에 한 번만 바꿀 수 있다.
 */
const router = useRouter()
const fandom = useFandomStore()
const toast = useToastStore()
const league = ref('KBO')
const targets = ref([])
const primaryId = ref(null)
const follows = ref(new Set())
const saving = ref(false)

const currentPrimary = computed(() => fandom.myFans.find((f) => f.primary && f.leagueCode === league.value))

async function loadTargets() {
  try {
    targets.value = await fandomApi.leagueTargets(league.value)
  } catch {
    targets.value = []
  }
  primaryId.value = currentPrimary.value?.target.id || null
  follows.value = new Set(fandom.myFans.filter((f) => !f.primary && f.leagueCode === league.value).map((f) => f.target.id))
}

function toggle(t) {
  if (primaryId.value === null) {
    primaryId.value = t.id
  } else if (primaryId.value === t.id) {
    primaryId.value = null
  } else if (follows.value.has(t.id)) {
    follows.value.delete(t.id)
  } else {
    follows.value.add(t.id)
  }
  follows.value = new Set(follows.value)
  follows.value.delete(primaryId.value)
}

async function save() {
  saving.value = true
  try {
    if (primaryId.value && primaryId.value !== currentPrimary.value?.target.id) {
      await fandomApi.joinFan(primaryId.value, true)
    }
    for (const id of follows.value) {
      if (!fandom.isFan(id)) await fandomApi.joinFan(id, false)
    }
    await fandom.loadMyFans(true)
    toast.success('응원팀을 저장했습니다.')
    router.replace('/')
  } catch (e) {
    toast.error(errorMessage(e))
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
    <h1>어느 팀을 응원하세요?</h1>
    <p class="muted">처음 누른 팀이 <strong>주 응원팀</strong>(★), 그다음부터는 팔로우가 됩니다. 다시 누르면 해제돼요.</p>
    <div class="pill-tabs">
      <button v-for="l in fandom.leagues" :key="l.code" :class="{ active: league === l.code }" @click="league = l.code; loadTargets()">{{ l.name }}</button>
    </div>
    <div v-if="currentPrimary" class="small muted note">
      이 리그의 주 응원팀은 <strong>{{ currentPrimary.target.name }}</strong>입니다.
      <span v-if="currentPrimary.primaryChangeAvailableAt">바꾸려면 30일이 지나야 합니다.</span>
    </div>
    <div class="teams">
      <button v-for="t in targets" :key="t.id" class="team"
              :class="{ primary: primaryId === t.id, follow: follows.has(t.id) }" :aria-pressed="primaryId === t.id || follows.has(t.id)"
              :style="primaryId === t.id || follows.has(t.id) ? { borderColor: readable(t.colorPrimary, t.colorSecondary), background: tint(t.colorPrimary, 0.12) } : {}"
              @click="toggle(t)">
        <span v-if="primaryId === t.id" class="mark">★ 주 응원</span>
        <span v-else-if="follows.has(t.id)" class="mark f">팔로우</span>
        <TeamCrest :name="t.name" :short-name="t.shortName" :color="t.colorPrimary" :color2="t.colorSecondary" :size="64" />
        <strong class="ellipsis">{{ t.name }}</strong>
      </button>
    </div>
    <div v-if="!targets.length" class="empty">
      이 리그에는 아직 등록된 팀이 없어요. <RouterLink :to="{ path: '/requests', query: { league } }" class="link">신청하기</RouterLink>
    </div>
    <div class="row actions">
      <button class="btn primary" :disabled="saving" @click="save">저장하고 시작하기</button>
      <RouterLink to="/" class="btn ghost">나중에</RouterLink>
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
