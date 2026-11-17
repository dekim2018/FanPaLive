<script setup>
import { LOCALES, locale, setLocale, tr } from '../i18n'

/** 화면 언어 바꾸기. 바꾸면 새로고침해서 모든 문구를 새 언어로 다시 그린다. */
</script>

<template>
  <label class="locale">
    <span class="sr-only">{{ tr('화면 언어') }}</span>
    <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path fill="currentColor" d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm6.9 6h-3a15.7 15.7 0 0 0-1.4-3.6A8 8 0 0 1 18.9 8zM12 4c.8 1.2 1.5 2.5 1.9 4h-3.8c.4-1.5 1.1-2.8 1.9-4zM4.3 14a8.2 8.2 0 0 1 0-4h3.4a16.5 16.5 0 0 0 0 4zm.8 2h3a15.7 15.7 0 0 0 1.4 3.6A8 8 0 0 1 5.1 16zm3-8h-3a8 8 0 0 1 4.4-3.6A15.7 15.7 0 0 0 8.1 8zM12 20c-.8-1.2-1.5-2.5-1.9-4h3.8c-.4 1.5-1.1 2.8-1.9 4zm2.3-6H9.7a14.7 14.7 0 0 1 0-4h4.6a14.7 14.7 0 0 1 0 4zm.3 5.6a15.7 15.7 0 0 0 1.4-3.6h3a8 8 0 0 1-4.4 3.6zM16.3 14a16.5 16.5 0 0 0 0-4h3.4a8.2 8.2 0 0 1 0 4z"/></svg>
    <select name="locale" :value="locale" @change="setLocale($event.target.value)">
      <option v-for="l in LOCALES" :key="l.code" :value="l.code">{{ l.label }}</option>
    </select>
  </label>
</template>

<style scoped>
.locale {
  display: inline-flex; align-items: center; gap: 4px; height: 36px; padding: 0 8px; border-radius: 8px;
  border: 1px solid var(--border); background: var(--bg-elev); color: var(--text-sub);
}
.locale:focus-within { box-shadow: var(--focus); }
select { border: none; background: transparent; color: var(--text); font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }
select:focus { outline: none; }
@media (max-width: 560px) { .locale svg { display: none; } .locale { padding: 0 4px; } }
</style>
