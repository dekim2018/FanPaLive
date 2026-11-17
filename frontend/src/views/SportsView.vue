<script setup>
import { useFandomStore } from '../stores/fandom'

/** 종목과 리그 목록(카테고리 화면) */
const fandom = useFandomStore()
const KIND = { TEAM: '팀 종목', INDIVIDUAL: '개인 종목' }
</script>

<template>
  <div>
    <h1>스포츠</h1>
    <p class="muted small">리그를 고르면 구단과 선수, 편파방을 볼 수 있어요. 목록에 없는 해외 팀이나 선수는 신청하면 운영자 승인 후 추가됩니다.</p>
    <section v-for="s in fandom.sports" :key="s.code" class="section">
      <div class="section-head">
        <h2>{{ s.name }}</h2>
        <span class="chip">{{ KIND[s.kind] || s.kind }}</span>
      </div>
      <div class="grid dense">
        <RouterLink v-for="l in s.leagues" :key="l.code" :to="`/leagues/${l.code}`" class="league card">
          <div class="code">{{ l.code }}</div>
          <div>
            <strong>{{ l.name }}</strong>
            <div class="xsmall muted">{{ l.country }}<span v-if="l.openForRequest">, 팬 신청 가능</span></div>
          </div>
        </RouterLink>
      </div>
    </section>
    <div v-if="!fandom.sports.length" class="empty">종목 정보를 불러오는 중입니다.</div>
  </div>
</template>

<style scoped>
.league { display: flex; gap: 12px; align-items: center; transition: border-color .15s; }
.league:hover { border-color: var(--accent); }
.code {
  width: 52px; height: 52px; border-radius: 12px; flex-shrink: 0; display: flex; align-items: center; justify-content: center;
  background: var(--accent-soft); color: #c9bbff; font-weight: 900; font-size: 12px; text-align: center; padding: 2px;
}
</style>
