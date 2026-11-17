<script setup>
import { tr } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { broadcastApi } from '../api/broadcasts'
import { fandomApi } from '../api/fandom'
import BroadcastCard from '../components/BroadcastCard.vue'
import HeartButton from '../components/HeartButton.vue'
import TeamCrest from '../components/TeamCrest.vue'
import { useViewerStats } from '../composables/useViewerStats'
import { formatCount } from '../lib/format'
import { latestOnly } from '../lib/latest'
import { useFandomStore } from '../stores/fandom'

const route = useRoute()
const fandom = useFandomStore()
const code = route.params.code
const { stats, refresh: refreshViewers } = useViewerStats()
const targets = ref([])
const targetsLoading = ref(true)
const targetsError = ref(false)
const lives = ref([])
const livesLoading = ref(true)
const livesError = ref(false)
const sort = ref('fans')
const league = computed(() => fandom.leagueByCode(code))
// 정렬을 빠르게 바꾸면 늦게 온 이전 정렬 결과가 덮어쓰지 않게 한다.
const latestTargets = latestOnly()

async function loadTargets() {
  targetsLoading.value = true
  targetsError.value = false
  try {
    const { value, stale } = await latestTargets(fandomApi.leagueTargets(code, sort.value))
    if (stale) return
    targets.value = value
  } catch {
    targetsError.value = true
  }
  targetsLoading.value = false
}

function changeSort(s) {
  sort.value = s
  loadTargets()
}

async function loadLives() {
  livesLoading.value = true
  livesError.value = false
  try {
    lives.value = (await broadcastApi.list({ league: code, status: 'LIVE', size: 12 })).content
    refreshViewers(lives.value)
  } catch {
    livesError.value = true
  } finally {
    livesLoading.value = false
  }
}

onMounted(() => {
  loadTargets()
  loadLives()
})
</script>

<template>
  <div>
    <div class="row head">
      <h1 style="margin:0">{{ league?.name || code }}</h1>
      <span v-if="league" class="chip">{{ tr(league.sportName) }}</span>
      <span class="spacer"></span>
      <RouterLink :to="{ path: '/rankings', query: { league: code } }" class="btn sm ghost">{{ tr('응원 랭킹') }}</RouterLink>
      <RouterLink v-if="league?.openForRequest" :to="{ path: '/requests', query: { league: code } }" class="btn sm">{{ tr('팀/선수 신청') }}</RouterLink>
    </div>

    <section class="section">
      <div class="section-head"><h2>{{ tr('편파방 LIVE') }}</h2></div>
      <div v-if="livesError" class="empty">{{ tr('방송 목록을 불러오지 못했습니다.') }} <button type="button" class="btn sm" @click="loadLives">{{ tr('다시 시도') }}</button></div>
      <div v-else-if="livesLoading && !lives.length" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
      <div v-else-if="!lives.length" class="empty">{{ tr('이 리그에서 방송 중인 편파방이 없습니다.') }}</div>
      <div v-else class="grid" :aria-busy="livesLoading">
        <BroadcastCard v-for="b in lives" :key="b.id" :broadcast="b" :viewers="stats[b.id]" />
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <h2>{{ tr('구단과 선수') }}</h2>
        <div class="pill-tabs" style="margin:0 0 0 auto">
          <button type="button" :class="{ active: sort === 'fans' }" :aria-pressed="sort === 'fans'" @click="changeSort('fans')">{{ tr('팬 많은 순') }}</button>
          <button type="button" :class="{ active: sort === 'name' }" :aria-pressed="sort === 'name'" @click="changeSort('name')">{{ tr('이름순') }}</button>
        </div>
      </div>
      <div v-if="targetsError" class="empty">{{ tr('구단/선수 목록을 불러오지 못했습니다.') }} <button type="button" class="btn sm" @click="loadTargets">{{ tr('다시 시도') }}</button></div>
      <div v-else-if="targetsLoading && !targets.length" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
      <div v-else-if="!targets.length" class="empty">{{ tr('아직 등록된 구단/선수가 없습니다.') }}</div>
      <div v-else class="grid dense" :aria-busy="targetsLoading">
        <div v-for="t in targets" :key="t.id" class="tcard card" :style="{ borderTopColor: t.colorPrimary }">
          <RouterLink :to="`/targets/${t.id}`" class="row nowrap">
            <TeamCrest :name="t.name" :short-name="t.shortName" :color="t.colorPrimary" :color2="t.colorSecondary" :size="44" />
            <div style="min-width:0">
              <strong class="ellipsis" style="display:block">{{ tr(t.name) }}</strong>
              <span class="xsmall muted">{{ t.kind === 'PLAYER' ? tr('선수') : tr('구단') }}</span>
            </div>
          </RouterLink>
          <div class="row xsmall muted num">
            <span>{{ tr('팬 {0}', formatCount(t.primaryFans + t.followFans)) }}</span>
            <span>{{ tr('하트 {0}', formatCount(t.hearts)) }}</span>
            <span class="spacer"></span>
            <HeartButton v-if="fandom.feature('hearts')" :target-id="t.id" compact />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.head { margin-bottom: 20px; }
.tcard { display: flex; flex-direction: column; gap: 10px; border-top: 3px solid; }
</style>
