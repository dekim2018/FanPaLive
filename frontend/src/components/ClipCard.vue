<script setup>
import { computed } from 'vue'
import { tr } from '../i18n'
import { formatDate, formatDuration } from '../lib/format'
import { teamFg } from '../lib/team'

/** 하이라이트 클립 카드. 썸네일 대신 응원팀 색 바탕에 재생 표시와 길이를 그린다. */
const props = defineProps({ clip: { type: Object, required: true } })
const side = computed(() => props.clip.side)
const thumbStyle = computed(() => {
  const c = side.value?.targetColor
  return c ? { background: `linear-gradient(135deg, ${c} 0%, #15161a 85%)` } : {}
})
</script>

<template>
  <RouterLink :to="`/clips/${clip.id}`" class="ccard">
    <div class="thumb" :style="thumbStyle" aria-hidden="true">
      <svg viewBox="0 0 24 24" width="36" height="36"><circle cx="12" cy="12" r="11" fill="rgba(0,0,0,.45)"/><path d="M10 8l6 4-6 4z" fill="#fff"/></svg>
      <span v-if="side" class="team" :style="{ color: teamFg(side.targetColor, side.targetTextColor) }">{{ tr(side.targetShortName || side.targetName) }}</span>
      <span class="len">{{ formatDuration(clip.durationSeconds) }}</span>
    </div>
    <div class="title ellipsis">{{ clip.title }}</div>
    <div class="xsmall muted ellipsis">{{ tr('{0} 클립, {1}', clip.creatorNickname, formatDate(clip.createdAt)) }}</div>
  </RouterLink>
</template>

<style scoped>
.ccard { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.thumb {
  position: relative; aspect-ratio: 16/9; border-radius: 8px; overflow: hidden; color: #fff;
  background: linear-gradient(135deg, #3a3d44, #16171b); display: grid; place-items: center;
  transition: transform .15s, box-shadow .15s;
}
.ccard:hover .thumb { transform: translateY(-3px); box-shadow: var(--shadow-lg); }
.ccard:focus-visible .thumb { box-shadow: 0 0 0 3px var(--accent); }
.team { position: absolute; top: 8px; left: 10px; font-weight: 900; font-size: 15px; text-shadow: 0 1px 6px rgba(0,0,0,.4); }
.len { position: absolute; bottom: 8px; right: 8px; background: rgba(0,0,0,.65); font-size: 12px; font-weight: 700; padding: 0 6px; border-radius: 4px; line-height: 20px; }
.title { font-weight: 700; font-size: 15px; }
.ccard:hover .title { color: var(--accent-text); }
</style>
