<script setup>
import { onMounted, ref } from 'vue'
import { tr } from '../i18n'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import TeamCrest from '../components/TeamCrest.vue'
import { formatCount } from '../lib/format'
import { latestOnly } from '../lib/latest'
import { readable, teamFg, solid } from '../lib/team'
import { useFandomStore } from '../stores/fandom'

/**
 * 명예의 전당: 지난 시즌 응원 점수 최종 순위와 우승팀 MVP 팬.
 * 새 시즌 첫날 자동으로 기록되고, 시즌 중에는 응원 랭킹 화면에서 실시간 순위를 본다.
 */
const fandom = useFandomStore()
const league = ref('KBO')
const seasons = ref([])
const loading = ref(true)
const error = ref('')
// 리그를 빠르게 바꿔도 마지막 요청의 응답만 쓴다
const latest = latestOnly()

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { value, stale } = await latest(fandomApi.hallOfFame(league.value))
    if (stale) return
    seasons.value = value
    loading.value = false
  } catch (e) {
    error.value = errorMessage(e)
    loading.value = false
  }
}

function seasonLabel(key) {
  return tr('{0} 시즌', key.replace(/^S/, ''))
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <div>
      <h1 style="margin-bottom:4px">{{ tr('명예의 전당') }}</h1>
      <p class="muted small" style="margin:0">{{ tr('시즌이 끝나면 응원 점수 최종 순위와 우승팀에 가장 많이 기여한 MVP 팬을 기록합니다.') }}</p>
    </div>
    <div class="pill-tabs" style="margin:0">
      <button v-for="l in fandom.leagues" :key="l.code" type="button" :class="{ active: league === l.code }" :aria-pressed="league === l.code" @click="league = l.code; load()">{{ tr(l.name) }}</button>
    </div>
    <div v-if="error" class="empty">{{ error }}</div>
    <div v-else-if="!loading && !seasons.length" class="empty">{{ tr('아직 마감된 시즌이 없습니다. 첫 시즌이 끝나면 이곳에 기록됩니다.') }}</div>
    <section v-for="s in seasons" :key="s.seasonKey" class="card season" :aria-labelledby="`season-${s.seasonKey}`">
      <div class="champion" :style="{ background: solid(s.rows[0]?.colorPrimary), color: teamFg(s.rows[0]?.colorPrimary, s.rows[0]?.colorSecondary) }">
        <div class="xsmall label">{{ seasonLabel(s.seasonKey) }}</div>
        <h2 :id="`season-${s.seasonKey}`" class="champ-name">{{ tr('응원 1위 {0}', tr(s.rows[0]?.name)) }}</h2>
        <div v-if="s.mvp" class="mvp">
          <span class="xsmall label">{{ tr('MVP 팬') }}</span>
          <strong>{{ s.mvp.nickname }}</strong>
          <span class="xsmall">{{ tr('기여 점수 {0}', formatCount(s.mvp.points)) }}</span>
        </div>
      </div>
      <ol class="rows">
        <li v-for="r in s.rows" :key="r.targetId" class="row nowrap">
          <span class="rank num" :style="{ color: r.rank === 1 ? readable(r.colorPrimary, r.colorSecondary) : undefined }">{{ r.rank }}</span>
          <TeamCrest :name="r.name" :short-name="r.shortName" :color="r.colorPrimary" :color2="r.colorSecondary" :size="30" />
          <RouterLink :to="`/targets/${r.targetId}`" class="ellipsis name">{{ tr(r.name) }}</RouterLink>
          <span class="spacer"></span>
          <span class="num small muted">{{ tr('{0}점', formatCount(r.score)) }}</span>
        </li>
      </ol>
    </section>
  </div>
</template>

<style scoped>
.season { display: grid; grid-template-columns: minmax(220px, 1fr) 2fr; gap: 16px; padding: 0; overflow: hidden; }
.champion { padding: 20px; display: flex; flex-direction: column; gap: 8px; }
.label { opacity: .9; font-weight: 700; letter-spacing: .04em; }
.champ-name { margin: 0; font-size: 22px; }
.mvp { margin-top: auto; display: flex; flex-direction: column; gap: 2px; padding: 10px 12px; border-radius: 8px; background: rgba(255, 255, 255, .14); }
.mvp strong { font-size: 18px; }
.rows { list-style: none; margin: 0; padding: 12px 16px 12px 0; display: flex; flex-direction: column; gap: 4px; }
.rows li { gap: 10px; padding: 6px 0; border-bottom: 1px solid var(--border); }
.rows li:last-child { border-bottom: none; }
.rank { width: 24px; text-align: center; font-weight: 900; }
.name { font-weight: 700; }
.name:hover { text-decoration: underline; text-underline-offset: 3px; }
@media (max-width: 720px) { .season { grid-template-columns: 1fr; } .rows { padding: 0 16px 12px; } }
</style>
