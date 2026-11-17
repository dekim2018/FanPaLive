<script setup>
import { computed } from 'vue'
import { solid, teamFg } from '../lib/team'

/**
 * 구단/선수 엠블럼 대신 쓰는 원형 표시. 진한 배경색 위에 연한 글자색으로 약칭과 테두리를 그린다.
 * 실제 구단 로고는 쓰지 않는다.
 */
const props = defineProps({
  name: { type: String, default: '' },
  shortName: { type: String, default: '' },
  color: { type: String, default: '#3a3d44' },
  color2: { type: String, default: '' },
  size: { type: Number, default: 36 }
})
const label = computed(() => (props.shortName || props.name || '?').slice(0, 3))
const style = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.max(10, Math.round(props.size * (label.value.length > 2 ? 0.28 : 0.34)))}px`,
  background: solid(props.color),
  color: teamFg(props.color, props.color2),
  boxShadow: `inset 0 0 0 ${props.size >= 40 ? 2 : 1.5}px ${teamFg(props.color, props.color2)}, 0 0 0 1px rgba(17,24,39,.08)`
}))
</script>

<template>
  <span class="crest" :style="style" :title="name" aria-hidden="true">{{ label }}</span>
</template>

<style scoped>
.crest {
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0;
  border-radius: 50%; font-weight: 900; letter-spacing: -.02em;
}
</style>
