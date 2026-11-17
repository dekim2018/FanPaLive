<script setup>
import { tr } from '../i18n'
import { useFandomStore } from '../stores/fandom'

/** 종목과 리그 목록(카테고리 화면) */
const fandom = useFandomStore()
const KIND = { TEAM: tr('팀 종목'), INDIVIDUAL: tr('개인 종목') }
</script>

<template>
  <div>
    <h1>{{ tr('스포츠') }}</h1>
    <p class="muted small">{{ tr('리그를 고르면 구단과 선수, 편파방을 볼 수 있어요. 목록에 없는 해외 팀이나 선수는 신청하면 운영자 승인 후 추가됩니다.') }}</p>
    <section v-for="s in fandom.sports" :key="s.code" class="section">
      <div class="section-head">
        <h2>{{ tr(s.name) }}</h2>
        <span class="chip">{{ KIND[s.kind] || s.kind }}</span>
      </div>
      <div class="grid dense">
        <RouterLink v-for="l in s.leagues" :key="l.code" :to="`/leagues/${l.code}`" class="league card">
          <div class="code" translate="no">{{ l.code }}</div>
          <div>
            <strong>{{ tr(l.name) }}</strong>
            <div class="xsmall muted">{{ tr(l.country) }}<span v-if="l.openForRequest">, {{ tr('팬 신청 가능') }}</span></div>
          </div>
        </RouterLink>
      </div>
    </section>
    <template v-if="!fandom.sports.length">
      <div v-if="fandom.sportsError" class="empty">{{ tr('종목 정보를 불러오지 못했습니다.') }} <button type="button" class="btn sm" @click="fandom.loadSports()">{{ tr('다시 시도') }}</button></div>
      <div v-else-if="fandom.sportsLoading" class="empty" aria-busy="true">{{ tr('불러오는 중…') }}</div>
      <div v-else class="empty">{{ tr('등록된 종목이 없습니다.') }}</div>
    </template>
  </div>
</template>

<style scoped>
.league { display: flex; gap: 12px; align-items: center; transition: border-color .15s; }
.league:hover { border-color: var(--accent); }
.code {
  width: 52px; height: 52px; border-radius: 12px; flex-shrink: 0; display: flex; align-items: center; justify-content: center;
  background: var(--accent-soft); color: var(--accent-text); font-weight: 900; font-size: 12px; text-align: center; padding: 2px;
}
</style>
