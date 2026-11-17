<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tr } from '../i18n'
import { clipApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import ClipCard from '../components/ClipCard.vue'
import { q, syncQuery } from '../lib/query'
import { useFandomStore } from '../stores/fandom'

/** 하이라이트 모음. 전체, 리그별, 내 응원팀별로 볼 수 있다. */
const route = useRoute()
const router = useRouter()
const fandom = useFandomStore()
// 새로고침이나 링크 공유를 해도 같은 필터와 페이지가 열리도록 주소에서 읽고 주소에 남긴다.
const initialTarget = Number(q(route, 'target', '')) || null
const filter = ref({ league: initialTarget ? '' : q(route, 'league', ''), target: initialTarget })
const items = ref([])
const loading = ref(true)
const error = ref('')
const page = ref(Math.max(0, (Number(q(route, 'page', '1')) || 1) - 1))
const totalPages = ref(0)
// 페이지 번호는 사람이 읽기 쉽게 1부터 주소에 남긴다.
watch([filter, page], () => syncQuery(router, route,
  { league: filter.value.league, target: filter.value.target, page: page.value + 1 }, { page: 1 }))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: 24 }
    if (filter.value.league) params.league = filter.value.league
    if (filter.value.target) params.target = filter.value.target
    const res = await clipApi.list(params)
    items.value = res.content
    totalPages.value = res.totalPages
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

function choose(next) {
  filter.value = { league: '', target: null, ...next }
  page.value = 0
  load()
}

function move(delta) {
  page.value += delta
  load()
}

onMounted(() => {
  fandom.loadMyFans()
  load()
})
</script>

<template>
  <div class="stack">
    <div>
      <h1 style="margin-bottom:4px">{{ tr('하이라이트') }}</h1>
      <p class="muted small" style="margin:0">{{ tr('다시보기에서 팬들이 직접 고른 명장면입니다. 다시보기 화면에서 누구나 15~60초 클립을 만들 수 있어요.') }}</p>
    </div>
    <div class="pill-tabs" style="margin:0">
      <button type="button" :class="{ active: !filter.league && !filter.target }" :aria-pressed="!filter.league && !filter.target" @click="choose({})">{{ tr('전체') }}</button>
      <button v-for="f in fandom.myFans" :key="'t' + f.target.id" type="button" :class="{ active: filter.target === f.target.id }"
              :aria-pressed="filter.target === f.target.id"
              @click="choose({ target: f.target.id })">{{ tr(f.target.name) }}</button>
      <button v-for="l in fandom.leagues" :key="l.code" type="button" :class="{ active: filter.league === l.code }"
              :aria-pressed="filter.league === l.code" @click="choose({ league: l.code })">{{ tr(l.name) }}</button>
    </div>
    <div v-if="error" class="empty">{{ error }}</div>
    <div v-else-if="!loading && !items.length" class="empty">{{ tr('아직 클립이 없습니다. 다시보기에서 첫 하이라이트를 만들어 보세요.') }}</div>
    <div v-else class="grid dense">
      <ClipCard v-for="c in items" :key="c.id" :clip="c" />
    </div>
    <div v-if="totalPages > 1" class="pagination">
      <button class="btn sm" :disabled="page === 0" @click="move(-1)">{{ tr('이전') }}</button>
      <button class="btn sm" :disabled="page + 1 >= totalPages" @click="move(1)">{{ tr('다음') }}</button>
    </div>
  </div>
</template>
