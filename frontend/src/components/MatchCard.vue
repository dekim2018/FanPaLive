<script setup>
import TeamCrest from './TeamCrest.vue'
import { formatDate } from '../lib/format'

defineProps({ match: { type: Object, required: true } })
const STATUS = { SCHEDULED: '예정', LIVE: 'LIVE', FINISHED: '종료', CANCELED: '취소' }
</script>

<template>
  <RouterLink :to="`/matches/${match.id}`" class="mcard">
    <div class="row nowrap small">
      <span class="badge" :class="match.status.toLowerCase()">{{ STATUS[match.status] }}</span>
      <span class="muted ellipsis">{{ match.league.name }}</span>
      <span class="spacer"></span>
      <span class="muted">{{ formatDate(match.startsAt) }}</span>
    </div>
    <div class="teams">
      <div class="team">
        <TeamCrest :name="match.home.name" :short-name="match.home.shortName" :color="match.home.colorPrimary" :size="40" />
        <span class="ellipsis">{{ match.home.shortName || match.home.name }}</span>
      </div>
      <div class="score num">
        <template v-if="match.status === 'SCHEDULED'">VS</template>
        <template v-else>{{ match.homeScore }} : {{ match.awayScore }}</template>
      </div>
      <div class="team">
        <TeamCrest :name="match.away.name" :short-name="match.away.shortName" :color="match.away.colorPrimary" :size="40" />
        <span class="ellipsis">{{ match.away.shortName || match.away.name }}</span>
      </div>
    </div>
    <div class="mini-gauge">
      <div :style="{ width: match.homePercent + '%', background: match.home.colorPrimary }"></div>
      <div :style="{ flex: 1, background: match.away.colorPrimary }"></div>
    </div>
  </RouterLink>
</template>

<style scoped>
.mcard {
  display: flex; flex-direction: column; gap: 10px; padding: 14px;
  background: var(--bg-elev); border: 1px solid var(--border); border-radius: var(--radius);
  min-width: 240px; transition: border-color .15s;
}
.mcard:hover { border-color: #444851; }
.teams { display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 8px; }
.team { display: flex; flex-direction: column; align-items: center; gap: 4px; font-weight: 700; font-size: 14px; min-width: 0; }
.score { font-size: 20px; font-weight: 900; }
.mini-gauge { display: flex; height: 4px; border-radius: 999px; overflow: hidden; background: var(--bg-elev-2); }
</style>
