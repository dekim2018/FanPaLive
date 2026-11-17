<script setup>
import { onMounted, ref } from 'vue'
import { fandomApi } from '../api/fandom'
import { errorMessage } from '../api/http'
import MatchCard from '../components/MatchCard.vue'
import { useFandomStore } from '../stores/fandom'

const fandom = useFandomStore()
const today = new Date()
const toIso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const date = ref(toIso(today))
const league = ref('')
const items = ref([])
const error = ref('')
const days = Array.from({ length: 7 }, (_, i) => {
  const d = new Date(today)
  d.setDate(d.getDate() + i - 1)
  return { iso: toIso(d), label: i === 1 ? '오늘' : `${d.getMonth() + 1}/${d.getDate()}` }
})

async function load() {
  error.value = ''
  try {
    const params = { date: date.value }
    if (league.value) params.league = league.value
    items.value = await fandomApi.matches(params)
  } catch (e) {
    error.value = errorMessage(e, '경기 일정을 불러오지 못했습니다.')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>경기 일정</h1>
    <div v-if="!fandom.feature('matches')" class="empty">경기 응원 기능이 잠시 꺼져 있습니다.</div>
    <template v-else>
      <div class="pill-tabs">
        <button v-for="d in days" :key="d.iso" :class="{ active: date === d.iso }" @click="date = d.iso; load()">{{ d.label }}</button>
      </div>
      <div class="pill-tabs">
        <button :class="{ active: !league }" @click="league = ''; load()">전체 리그</button>
        <button v-for="l in fandom.leagues" :key="l.code" :class="{ active: league === l.code }" @click="league = l.code; load()">{{ l.name }}</button>
      </div>
      <div v-if="error" class="empty">{{ error }}</div>
      <div v-else-if="!items.length" class="empty">이 날짜에 등록된 경기가 없습니다.</div>
      <div v-else class="grid dense">
        <MatchCard v-for="m in items" :key="m.id" :match="m" />
      </div>
    </template>
  </div>
</template>
