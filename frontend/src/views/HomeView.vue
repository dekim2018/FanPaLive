<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { broadcastApi } from '../api/broadcasts'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import BroadcastCard from '../components/BroadcastCard.vue'
import MatchCard from '../components/MatchCard.vue'
import RankingTable from '../components/RankingTable.vue'
import TeamCrest from '../components/TeamCrest.vue'
import { useViewerStats } from '../composables/useViewerStats'
import { formatCount } from '../lib/format'
import { teamFg } from '../lib/team'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'

const auth = useAuthStore()
const fandom = useFandomStore()
const { stats, refresh: refreshViewers } = useViewerStats()

const league = ref('')
const lives = ref([])
const matches = ref([])
const ranking = ref(null)
const overall = ref([])
const error = ref('')
let timer = null

const featured = computed(() => lives.value[0] || null)
const rest = computed(() => lives.value.slice(featured.value ? 1 : 0))
const myTeamIds = computed(() => new Set(fandom.myFans.map((f) => f.target.id)))
const myTeamLives = computed(() => lives.value.filter((b) => b.side && myTeamIds.value.has(b.side.targetId)))

async function loadLives() {
  try {
    const params = { status: 'LIVE', size: 30 }
    if (league.value) params.league = league.value
    lives.value = (await broadcastApi.list(params)).content
    error.value = ''
    refreshViewers(lives.value)
  } catch (e) {
    error.value = errorMessage(e, '방송 목록을 불러오지 못했습니다.')
  }
}

async function loadSide() {
  const [m, r, o] = await Promise.allSettled([
    fandomApi.upcoming(8),
    fandomApi.ranking('KBO', 'WEEKLY', 'SCORE'),
    fandomApi.overall(5)
  ])
  if (m.status === 'fulfilled') matches.value = m.value
  if (r.status === 'fulfilled') ranking.value = r.value
  if (o.status === 'fulfilled') overall.value = o.value
}

function pickLeague(code) {
  league.value = code
  loadLives()
}

onMounted(() => {
  loadLives()
  loadSide()
  timer = setInterval(loadLives, 15000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div>
    <!-- 대표 라이브 -->
    <section v-if="featured" class="hero" :style="{ '--hero': featured.side?.targetColor || '#5e5ede' }">
      <RouterLink :to="`/broadcasts/${featured.id}`" class="hero-thumb">
        <span class="badge live">LIVE</span>
        <span v-if="stats[featured.id]" class="hero-viewers">{{ formatCount(stats[featured.id].current) }}명 시청</span>
        <span class="hero-mark" :style="{ color: featured.side ? teamFg(featured.side.targetColor, featured.side.targetTextColor) : '#fff' }">{{ featured.side?.targetShortName || 'LIVE' }}</span>
      </RouterLink>
      <div class="hero-info">
        <div class="row">
          <TeamCrest v-if="featured.side" :name="featured.side.targetName" :short-name="featured.side.targetShortName" :color="featured.side.targetColor" :color2="featured.side.targetTextColor" :size="44" />
          <div>
            <div class="small muted">{{ featured.ownerNickname }}</div>
            <div v-if="featured.side" class="small"><strong>{{ featured.side.targetName }}</strong> 편파중계</div>
          </div>
        </div>
        <h1 class="hero-title">{{ featured.title }}</h1>
        <p v-if="featured.description" class="muted hero-desc">{{ featured.description }}</p>
        <RouterLink :to="`/broadcasts/${featured.id}`" class="btn primary">지금 보러 가기</RouterLink>
      </div>
    </section>
    <section v-else-if="!error" class="hero empty-hero">
      <div>
        <h1>지금 진행 중인 편파중계가 없어요</h1>
        <p class="muted">내가 응원하는 팀의 경기를 직접 중계해 보세요. PC 화면이나 휴대폰 카메라로 바로 시작할 수 있습니다.</p>
        <RouterLink to="/broadcasts/new" class="btn primary">편파중계 시작하기</RouterLink>
      </div>
    </section>

    <!-- 응원팀 미설정 안내 -->
    <section v-if="auth.isLoggedIn && fandom.myFansLoaded && !fandom.myFans.length" class="cta card row">
      <div>
        <strong>아직 응원팀이 없어요.</strong>
        <div class="small muted">주 응원팀을 정하면 랭킹에 내 응원이 들어가고 채팅에 팀 배지가 붙습니다.</div>
      </div>
      <span class="spacer"></span>
      <RouterLink to="/onboarding" class="btn primary sm">응원팀 고르기</RouterLink>
    </section>

    <!-- 내 팀 라이브 -->
    <section v-if="myTeamLives.length" class="section">
      <div class="section-head"><h2>내 응원팀 LIVE</h2></div>
      <div class="grid">
        <BroadcastCard v-for="b in myTeamLives" :key="'my' + b.id" :broadcast="b" :viewers="stats[b.id]" />
      </div>
    </section>

    <!-- 경기 -->
    <section v-if="fandom.feature('matches') && matches.length" class="section">
      <div class="section-head">
        <h2>오늘의 응원 대결</h2>
        <RouterLink to="/matches" class="more">전체 일정</RouterLink>
      </div>
      <div class="strip">
        <MatchCard v-for="m in matches" :key="m.id" :match="m" />
      </div>
    </section>

    <!-- 라이브 목록 -->
    <section class="section">
      <div class="section-head">
        <h2>스포츠 LIVE</h2>
        <RouterLink to="/live" class="more">전체 보기</RouterLink>
      </div>
      <div class="pill-tabs">
        <button :class="{ active: !league }" @click="pickLeague('')">전체</button>
        <button v-for="l in fandom.leagues" :key="l.code" :class="{ active: league === l.code }" @click="pickLeague(l.code)">{{ l.name }}</button>
      </div>
      <div v-if="error" class="empty">{{ error }}</div>
      <div v-else-if="!rest.length && !featured" class="empty">
        이 리그에서 방송 중인 채널이 없습니다.
        <div style="margin-top:12px"><RouterLink to="/broadcasts/new" class="btn primary sm">첫 편파중계 만들기</RouterLink></div>
      </div>
      <div v-else class="grid">
        <BroadcastCard v-for="b in rest" :key="b.id" :broadcast="b" :viewers="stats[b.id]" />
      </div>
    </section>

    <!-- 랭킹 -->
    <div class="two-col">
      <section v-if="ranking" class="section card">
        <div class="section-head">
          <h2>이번 주 {{ ranking.league.name }} 응원 랭킹</h2>
          <RouterLink to="/rankings" class="more">전체 랭킹</RouterLink>
        </div>
        <RankingTable :rows="ranking.rows.slice(0, 5)" compact />
      </section>
      <section v-if="overall.length" class="section card">
        <div class="section-head">
          <h2>종목 통합 팬 TOP</h2>
        </div>
        <div class="stack">
          <RouterLink v-for="o in overall" :key="o.row.targetId" :to="`/targets/${o.row.targetId}`" class="row nowrap overall">
            <span class="num rank">{{ o.rank }}</span>
            <TeamCrest :name="o.row.name" :short-name="o.row.shortName" :color="o.row.colorPrimary" :color2="o.row.colorSecondary" :size="30" />
            <span class="ellipsis">{{ o.row.name }}</span>
            <span class="chip">{{ o.league.name }}</span>
            <span class="spacer"></span>
            <span class="small muted num">팬 {{ formatCount(o.row.primaryFans + o.row.followFans) }}</span>
          </RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.hero {
  display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr); gap: 24px;
  padding: 20px; margin-bottom: 28px; border-radius: 14px;
  background: linear-gradient(120deg, color-mix(in srgb, var(--hero) 9%, var(--bg-elev)) 0%, var(--bg-elev) 60%);
  border: 1px solid var(--border); box-shadow: var(--shadow);
}
.hero-thumb {
  position: relative; aspect-ratio: 16/9; border-radius: 10px; overflow: hidden;
  background: linear-gradient(135deg, var(--hero), #121316 80%);
  display: flex; align-items: center; justify-content: center; color: #fff;
}
.hero-thumb .badge { position: absolute; top: 12px; left: 12px; }
.hero-viewers { position: absolute; top: 12px; left: 56px; background: rgba(0,0,0,.6); font-size: 12px; font-weight: 700; padding: 0 8px; border-radius: 4px; line-height: 20px; }
.hero-mark { font-size: 48px; font-weight: 900; text-shadow: 0 4px 20px rgba(0,0,0,.4); }
.hero-info { display: flex; flex-direction: column; gap: 12px; justify-content: center; min-width: 0; }
.hero-title { font-size: 24px; margin: 0; }
.hero-desc { margin: 0; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.empty-hero { grid-template-columns: 1fr; padding: 36px; --hero: #5e5ede; }
.cta { margin-bottom: 28px; }
.strip { display: flex; gap: 12px; overflow-x: auto; padding-bottom: 6px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.overall { gap: 10px; padding: 4px 0; font-weight: 600; }
.rank { width: 20px; font-weight: 900; color: var(--text-sub); }
@media (max-width: 900px) {
  .hero { grid-template-columns: 1fr; }
  .two-col { grid-template-columns: 1fr; }
}
</style>
