<script setup>
import { tr } from '../i18n'
import { computed } from 'vue'
import { formatCount } from '../lib/format'
import { gaugeColors, readable } from '../lib/team'

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
// 두 팀 배경색이 비슷하면 한쪽을 글자색으로 칠해 막대를 구분한다(두산과 롯데, KIA와 SSG)
const colors = computed(() => gaugeColors(props.home, props.away))
</script>

<template>
  <div class="gauge">
    <div class="labels row nowrap">
      <strong :style="{ color: readable(colors.home) }">{{ tr(home.shortName || home.name) }} {{ percent }}%</strong>
      <span class="spacer"></span>
      <strong :style="{ color: readable(colors.away) }">{{ 100 - percent }}% {{ tr(away.shortName || away.name) }}</strong>
    </div>
    <div class="bar" role="img" :aria-label="tr('응원 게이지 {0} {1}퍼센트, {2} {3}퍼센트', tr(home.name), percent, tr(away.name), 100 - percent)">
      <div class="home" :style="{ width: percent + '%', background: colors.home }"></div>
      <div class="away" :style="{ background: colors.away }"></div>
    </div>
    <div class="row nowrap small muted num">
      <span>{{ tr('하트 {0}', formatCount(homeHearts)) }}</span>
      <span class="spacer"></span>
      <span>{{ tr('하트 {0}', formatCount(awayHearts)) }}</span>
    </div>
  </div>
</template>

<style scoped>
.gauge { display: flex; flex-direction: column; gap: 6px; }
.bar { display: flex; height: 14px; border-radius: 999px; overflow: hidden; background: var(--bg-elev-2); }
.home { transition: width .5s ease; border-right: 3px solid var(--bg-elev); }
.away { flex: 1; }
</style>
