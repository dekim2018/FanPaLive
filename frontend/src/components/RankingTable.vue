<script setup>
import TeamCrest from './TeamCrest.vue'
import HeartButton from './HeartButton.vue'
import { formatCount, formatWon } from '../lib/format'

/**
 * 응원 랭킹 표. 점수 = 주 팬 x100 + 팔로우 x20 + 하트 점수 + 후원금/100.
 * 점수가 같으면 먼저 그 점수에 도달한 대상이 위에 온다.
 */
defineProps({
  rows: { type: Array, required: true },
  showHeart: { type: Boolean, default: true },
  compact: { type: Boolean, default: false }
})
</script>

<template>
  <div class="rank-wrap">
    <table class="table">
      <thead>
        <tr>
          <th style="width:56px">순위</th>
          <th>구단/선수</th>
          <th class="r">응원 점수</th>
          <th v-if="!compact" class="r hide-sm">팬</th>
          <th v-if="!compact" class="r hide-sm">하트</th>
          <th v-if="!compact" class="r hide-md">후원</th>
          <th v-if="showHeart" style="width:60px"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in rows" :key="r.targetId">
          <td>
            <div class="rank num" :class="{ top: r.rank <= 3 }">{{ r.rank }}</div>
            <div v-if="r.rankChange" class="xsmall num" :class="r.rankChange > 0 ? 'up' : 'down'">
              {{ r.rankChange > 0 ? '▲' : '▼' }}{{ Math.abs(r.rankChange) }}
            </div>
          </td>
          <td>
            <RouterLink :to="`/targets/${r.targetId}`" class="row nowrap name">
              <TeamCrest :name="r.name" :short-name="r.shortName" :color="r.colorPrimary" :color2="r.colorSecondary" :size="32" />
              <span class="ellipsis">{{ r.name }}</span>
              <span v-if="r.kind === 'PLAYER'" class="chip">선수</span>
            </RouterLink>
          </td>
          <td class="r num strong">{{ Number(r.score).toLocaleString('ko-KR') }}</td>
          <td v-if="!compact" class="r num hide-sm">{{ formatCount(r.primaryFans + r.followFans) }}</td>
          <td v-if="!compact" class="r num hide-sm">{{ formatCount(r.hearts) }}</td>
          <td v-if="!compact" class="r num hide-md">{{ formatWon(r.donationAmount) }}</td>
          <td v-if="showHeart"><HeartButton :target-id="r.targetId" source="RANKING" compact /></td>
        </tr>
      </tbody>
    </table>
    <div v-if="!rows.length" class="empty">아직 집계된 응원이 없습니다.</div>
  </div>
</template>

<style scoped>
.rank-wrap { overflow-x: auto; }
.r { text-align: right; }
.strong { font-weight: 800; }
.rank { font-weight: 900; font-size: 16px; }
.rank.top { color: var(--accent); }
.name { font-weight: 700; min-width: 0; }
@media (max-width: 720px) { .hide-sm { display: none; } }
@media (max-width: 1000px) { .hide-md { display: none; } }
</style>
