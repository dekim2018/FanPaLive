<script setup>
import { tr } from '../i18n'
import TeamCrest from './TeamCrest.vue'
import { computed } from 'vue'
import { formatDate } from '../lib/format'
import { gaugeColors } from '../lib/team'

const props = defineProps({ match: { type: Object, required: true } })
const gauge = computed(() => gaugeColors(props.match.home, props.match.away))
const STATUS = { SCHEDULED: tr('예정'), LIVE: 'LIVE', FINISHED: tr('종료'), CANCELED: tr('취소') }
</script>

<template>
  <RouterLink :to="`/matches/${match.id}`" class="mcard"
              :aria-label="tr('{0} 대 {1}, {2}, 매치룸 열기', tr(match.home.name), tr(match.away.name), STATUS[match.status])">
    <div class="row nowrap small">
      <span class="badge" :class="match.status.toLowerCase()">{{ STATUS[match.status] }}</span>
      <span class="muted ellipsis">{{ tr(match.league.name) }}</span>
      <span class="spacer"></span>
      <span class="muted">{{ formatDate(match.startsAt) }}</span>
    </div>
    <div class="teams">
      <div class="team">
        <TeamCrest :name="match.home.name" :short-name="match.home.shortName" :color="match.home.colorPrimary" :color2="match.home.colorSecondary" :size="40" />
        <span class="ellipsis">{{ tr(match.home.shortName || match.home.name) }}</span>
      </div>
      <div class="score num">
        <template v-if="match.status === 'SCHEDULED'">VS</template>
        <template v-else>{{ match.homeScore }} : {{ match.awayScore }}</template>
      </div>
      <div class="team">
        <TeamCrest :name="match.away.name" :short-name="match.away.shortName" :color="match.away.colorPrimary" :color2="match.away.colorSecondary" :size="40" />
        <span class="ellipsis">{{ tr(match.away.shortName || match.away.name) }}</span>
      </div>
    </div>
    <div class="mini-gauge" aria-hidden="true">
      <div :style="{ width: match.homePercent + '%', background: gauge.home }"></div>
      <div :style="{ flex: 1, background: gauge.away }"></div>
    </div>
  </RouterLink>
</template>

<style scoped>
.mcard {
  display: flex; flex-direction: column; gap: 10px; padding: 14px;
  background: var(--bg-elev); border: 1px solid var(--border); border-radius: var(--radius);
  min-width: 240px; transition: border-color .15s, box-shadow .15s; box-shadow: var(--shadow);
}
.mcard:hover { border-color: var(--border-strong); box-shadow: var(--shadow-lg); }
.teams { display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 8px; }
.team { display: flex; flex-direction: column; align-items: center; gap: 4px; font-weight: 700; font-size: 14px; min-width: 0; }
.score { font-size: 20px; font-weight: 900; }
.mini-gauge > div:first-child { border-right: 2px solid var(--bg-elev); }
.mini-gauge { display: flex; height: 5px; border-radius: 999px; overflow: hidden; background: var(--bg-elev-2); }
</style>
