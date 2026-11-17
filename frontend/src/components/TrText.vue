<script setup>
import { computed } from 'vue'

/**
 * 문장 안 일부를 강조(굵게, 링크)해야 할 때 쓰는 번역 문장.
 * 언어마다 어순이 달라서 문장을 조각내지 않고, {0} {1} 자리에 슬롯(p0, p1)을 끼운다.
 * 예) <TrText :text="tr('{0}님에게 {1}을 후원했습니다.')"><template #p0>..</template><template #p1>..</template></TrText>
 */
const props = defineProps({ text: { type: String, required: true } })
const parts = computed(() => props.text.split(/(\{\d+\})/).filter((p) => p !== '').map((p) => {
  const m = p.match(/^\{(\d+)\}$/)
  return m ? { slot: 'p' + m[1] } : { text: p }
}))
</script>

<template>
  <template v-for="(p, i) in parts" :key="i"><slot v-if="p.slot" :name="p.slot" /><template v-else>{{ p.text }}</template></template>
</template>
