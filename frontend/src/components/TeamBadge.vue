<script setup>
import { solid, teamFg } from '../lib/team'

/** 채팅 닉네임 옆, 카드 등에 붙는 작은 응원팀 표시 */
defineProps({
  label: { type: String, required: true },
  color: { type: String, default: '#3a3d44' },
  color2: { type: String, default: '' },
  to: { type: String, default: '' }
})
</script>

<template>
  <component :is="to ? 'RouterLink' : 'span'" :to="to || undefined" class="tbadge"
             :style="{ background: solid(color), color: teamFg(color, color2) }">{{ label }}</component>
</template>

<style scoped>
.tbadge {
  display: inline-flex; align-items: center; padding: 0 6px; height: 18px;
  border-radius: 4px; font-size: 11px; font-weight: 800; white-space: nowrap; vertical-align: middle;
}
</style>
