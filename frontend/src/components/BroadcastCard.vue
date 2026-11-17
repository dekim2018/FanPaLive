<script setup>
import { computed } from 'vue'
import TeamCrest from './TeamCrest.vue'
import { elapsedSince, formatCount, formatDate } from '../lib/format'
import { textOn } from '../lib/team'

/**
 * 라이브 카드: 16:9 썸네일(LIVE 배지, 시청자 수, 방송 시간) + 아래 채널 정보와 카테고리 칩.
 * 썸네일 이미지는 아직 없어서 응원팀 색 그라디언트로 대신한다.
 */
const props = defineProps({
  broadcast: { type: Object, required: true },
  viewers: { type: Object, default: null }
})

const side = computed(() => props.broadcast.side)
const live = computed(() => props.broadcast.status === 'LIVE')
const thumbStyle = computed(() => {
  const c = side.value?.targetColor
  return c ? { background: `linear-gradient(135deg, ${c}cc 0%, #15161a 75%)` } : {}
})
</script>

<template>
  <RouterLink :to="`/broadcasts/${broadcast.id}`" class="bcard">
    <div class="thumb" :style="thumbStyle" aria-hidden="true">
      <div class="thumb-mark">
        <template v-if="side">
          <span class="mark-team" :style="{ color: textOn(side.targetColor) }">{{ side.targetShortName || side.targetName }}</span>
          <span class="mark-sub">편파중계</span>
        </template>
        <span v-else class="mark-sub">{{ broadcast.sourceType === 'MOBILE_CAMERA' ? 'CAMERA' : 'SCREEN' }}</span>
      </div>
      <div class="tl">
        <span v-if="live" class="badge live">LIVE</span>
        <span v-else class="badge" :class="broadcast.status.toLowerCase()">{{ broadcast.status === 'ENDED' ? '종료' : '대기' }}</span>
        <span v-if="live && viewers" class="viewers">{{ formatCount(viewers.current) }}명</span>
      </div>
      <div v-if="live" class="br small">{{ elapsedSince(broadcast.startedAt) }}</div>
    </div>
    <div class="meta">
      <TeamCrest v-if="side" :name="side.targetName" :short-name="side.targetShortName" :color="side.targetColor" :size="36" />
      <span v-else class="avatar">{{ broadcast.ownerNickname?.slice(0, 1) }}</span>
      <div class="meta-text">
        <span class="sr-only">{{ live ? `방송 중${viewers ? `, 시청자 ${viewers.current}명` : ''}` : broadcast.status === 'ENDED' ? '종료된 방송' : '대기 중인 방송' }}</span>
        <div class="title ellipsis">{{ broadcast.title }}</div>
        <div class="owner small ellipsis">{{ broadcast.ownerNickname }}</div>
        <div class="chips">
          <span v-if="side" class="chip">{{ side.leagueCode }}</span>
          <span v-if="side" class="chip">{{ side.targetName }}</span>
          <span v-if="!live" class="chip">{{ formatDate(broadcast.createdAt) }}</span>
          <span v-if="live && viewers && viewers.unique" class="chip">누적 약 {{ formatCount(viewers.unique) }}명</span>
        </div>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.bcard { display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.thumb {
  position: relative; aspect-ratio: 16/9; border-radius: 8px; overflow: hidden;
  background: linear-gradient(135deg, #2a2d34, #16171b);
  display: flex; align-items: center; justify-content: center; color: #fff;
  transition: transform .15s, box-shadow .15s;
}
.bcard:hover .thumb { transform: translateY(-3px); box-shadow: var(--shadow-lg); }
.bcard:hover .title { color: var(--accent-text); }
.bcard:focus-visible .thumb { box-shadow: 0 0 0 3px var(--accent); }
.thumb-mark { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.mark-team { font-size: 26px; font-weight: 900; letter-spacing: -.02em; text-shadow: 0 2px 10px rgba(0,0,0,.4); }
.mark-sub { font-size: 12px; font-weight: 800; letter-spacing: .18em; color: rgba(255,255,255,.55); }
.tl { position: absolute; top: 8px; left: 8px; display: flex; gap: 4px; align-items: center; }
.viewers { background: rgba(0,0,0,.6); color: #fff; font-size: 11px; font-weight: 700; padding: 1px 6px; border-radius: 4px; line-height: 18px; }
.br { position: absolute; bottom: 8px; right: 8px; background: rgba(0,0,0,.6); padding: 0 6px; border-radius: 4px; }
.meta { display: flex; gap: 10px; min-width: 0; }
.avatar {
  width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  background: var(--bg-elev-2); font-weight: 800; color: var(--text-sub);
  border: 1px solid var(--border);
}
.meta-text { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 2px; }
.title { font-weight: 700; font-size: 15px; }
.owner { color: var(--text-sub); }
.chips { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 2px; }
</style>
