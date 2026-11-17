<script setup>
import { computed } from 'vue'
import { tr } from '../i18n'
import { levelInfo } from '../lib/level'

/** 팬 등급 배지. hideRookie 면 루키는 그리지 않는다(채팅용). */
const props = defineProps({
  level: { type: String, default: 'ROOKIE' },
  hideRookie: { type: Boolean, default: false }
})
const info = computed(() => levelInfo(props.level))
</script>

<template>
  <span v-if="!(hideRookie && level === 'ROOKIE')" class="lv" :style="{ color: info.fg, background: info.bg }"
        :title="tr('팬 등급: {0}', info.label)">{{ info.label }}</span>
</template>

<style scoped>
.lv {
  display: inline-flex; align-items: center; height: 18px; padding: 0 6px; border-radius: 4px;
  font-size: 11px; font-weight: 800; white-space: nowrap; vertical-align: middle;
}
</style>
