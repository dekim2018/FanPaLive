<script setup>
import { tr } from '../i18n'
defineProps({ status: { type: String, required: true }, online: { type: Boolean, default: true } })
const LABELS = { LIVE: 'LIVE', READY: tr('대기'), ENDED: tr('종료'), RECORDING: tr('녹화 중'), VOD: tr('다시보기'), FAILED: tr('실패') }
</script>

<template>
  <span class="badge" :class="status.toLowerCase()">
    <template v-if="status === 'LIVE'">{{ online ? 'LIVE' : tr('LIVE (연결 대기)') }}</template>
    <template v-else>{{ LABELS[status] || status }}</template>
  </span>
</template>
