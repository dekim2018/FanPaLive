<script setup>
import { tr } from '../i18n'
import { useToastStore } from '../stores/toast'
const toast = useToastStore()
</script>

<template>
  <div class="toast-host" role="status" aria-live="polite">
    <button v-for="t in toast.items" :key="t.id" type="button" class="toast" :class="t.type"
            :aria-label="tr('{0} (누르면 닫힘)', t.message)" @click="toast.dismiss(t.id)">
      {{ t.message }}
    </button>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed; left: 50%; bottom: calc(24px + env(safe-area-inset-bottom)); transform: translateX(-50%);
  display: flex; flex-direction: column; gap: 8px; z-index: 100; width: min(92vw, 420px);
}
.toast {
  padding: 12px 16px; border-radius: 10px; background: var(--text); color: #fff; border: none;
  box-shadow: var(--shadow-lg); font-size: 14px; cursor: pointer; text-align: left; font-family: inherit;
}
.toast.success { border-left: 4px solid #6ee7a8; }
.toast.error { border-left: 4px solid #ff8a7a; }
</style>
