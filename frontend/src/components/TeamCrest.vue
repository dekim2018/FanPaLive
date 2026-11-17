<script setup>
import { computed } from 'vue'
import { textOn } from '../lib/team'

/** 구단/선수 엠블럼 대신 쓰는 원형 표시(팀 색 + 약칭). 실제 구단 로고는 쓰지 않는다. */
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
  background: props.color2 ? `linear-gradient(135deg, ${props.color} 60%, ${props.color2})` : props.color,
  color: textOn(props.color)
}))
</script>

<template>
  <span class="crest" :style="style" :title="name" aria-hidden="true">{{ label }}</span>
</template>

<style scoped>
.crest {
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0;
  border-radius: 50%; font-weight: 900; letter-spacing: -.02em;
  box-shadow: inset 0 0 0 2px rgba(255,255,255,.18), 0 0 0 1px rgba(17,24,39,.08);
}
</style>
