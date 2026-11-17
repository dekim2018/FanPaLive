<script setup>
import { computed } from 'vue'
import { formatCount } from '../lib/format'
import { readable } from '../lib/team'

/** 매치룸 응원 게이지: 홈/원정 하트 비율 */
const props = defineProps({
  home: { type: Object, required: true },
  away: { type: Object, required: true },
  homeHearts: { type: Number, default: 0 },
  awayHearts: { type: Number, default: 0 }
})
const percent = computed(() => {
  const total = props.homeHearts + props.awayHearts
  return total === 0 ? 50 : Math.round((props.homeHearts * 100) / total)
})
</script>

<template>
  <div class="gauge">
    <div class="labels row nowrap">
      <strong :style="{ color: readable(home.colorPrimary, home.colorSecondary) }">{{ home.shortName || home.name }} {{ percent }}%</strong>
      <span class="spacer"></span>
      <strong :style="{ color: readable(away.colorPrimary, away.colorSecondary) }">{{ 100 - percent }}% {{ away.shortName || away.name }}</strong>
    </div>
    <div class="bar" role="img" :aria-label="`응원 게이지 ${home.name} ${percent}퍼센트, ${away.name} ${100 - percent}퍼센트`">
      <div class="home" :style="{ width: percent + '%', background: home.colorPrimary }"></div>
      <div class="away" :style="{ background: away.colorPrimary }"></div>
    </div>
    <div class="row nowrap small muted num">
      <span>하트 {{ formatCount(homeHearts) }}</span>
      <span class="spacer"></span>
      <span>하트 {{ formatCount(awayHearts) }}</span>
    </div>
  </div>
</template>

<style scoped>
.gauge { display: flex; flex-direction: column; gap: 6px; }
.bar { display: flex; height: 14px; border-radius: 999px; overflow: hidden; background: var(--bg-elev-2); }
.home { transition: width .5s ease; border-right: 2px solid var(--bg-elev); }
.away { flex: 1; }
</style>
