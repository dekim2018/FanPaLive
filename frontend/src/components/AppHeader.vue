<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fandomApi } from '../api/fandom'
import TeamCrest from './TeamCrest.vue'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'

const emit = defineEmits(['toggle-sidebar'])
const auth = useAuthStore()
const fandom = useFandomStore()
const toast = useToastStore()
const router = useRouter()

const q = ref('')
const results = ref([])
const searchOpen = ref(false)
const menuOpen = ref(false)
let debounce = null

watch(q, (value) => {
  clearTimeout(debounce)
  if (!value.trim()) {
    results.value = []
    return
  }
  debounce = setTimeout(async () => {
    try {
      results.value = (await fandomApi.searchTargets(value.trim())).slice(0, 8)
    } catch {
      results.value = []
    }
  }, 250)
})
onBeforeUnmount(() => clearTimeout(debounce))

function pick(target) {
  q.value = ''
  results.value = []
  searchOpen.value = false
  router.push(`/targets/${target.id}`)
}

function closeSearchLater() {
  setTimeout(() => { searchOpen.value = false }, 150)
}

function submitSearch() {
  if (results.value.length) pick(results.value[0])
}

async function logout() {
  menuOpen.value = false
  await auth.logout()
  fandom.clearMine()
  toast.success('로그아웃되었습니다.')
  router.push('/')
}
</script>

<template>
  <header class="header">
    <button class="icon-btn" aria-label="메뉴" @click="emit('toggle-sidebar')">
      <svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M3 6h18v2H3zm0 5h18v2H3zm0 5h18v2H3z"/></svg>
    </button>
    <RouterLink to="/" class="logo" aria-label="팬파라이브 홈">
      <svg viewBox="0 0 32 32" width="26" height="26" aria-hidden="true">
        <rect x="2" y="2" width="28" height="28" rx="8" fill="#7c5cff"/>
        <path d="M10 9h12v4h-8v3h7v4h-7v4h-4z" fill="#fff"/>
        <circle cx="24" cy="22" r="3" fill="#ff3040"/>
      </svg>
      <span class="logo-text">팬파라이브</span>
    </RouterLink>

    <form class="search" @submit.prevent="submitSearch">
      <input v-model="q" class="search-input" placeholder="구단, 선수 검색 (예: 두산)"
             @focus="searchOpen = true" @blur="closeSearchLater" />
      <svg class="search-icon" viewBox="0 0 24 24" width="18" height="18"><path fill="currentColor" d="M10 2a8 8 0 0 1 6.3 12.9l5.4 5.4-1.4 1.4-5.4-5.4A8 8 0 1 1 10 2zm0 2a6 6 0 1 0 0 12 6 6 0 0 0 0-12z"/></svg>
      <div v-if="searchOpen && results.length" class="search-pop">
        <button v-for="t in results" :key="t.id" type="button" class="search-item" @mousedown.prevent="pick(t)">
          <TeamCrest :name="t.name" :short-name="t.shortName" :color="t.colorPrimary" :size="28" />
          <span class="ellipsis">{{ t.name }}</span>
          <span class="muted xsmall">{{ t.leagueName }}</span>
        </button>
      </div>
    </form>

    <div class="right">
      <template v-if="auth.isLoggedIn">
        <RouterLink to="/broadcasts/new" class="btn primary sm studio">
          <svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M4 6h11a2 2 0 0 1 2 2v1.5l4-2.5v10l-4-2.5V16a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z"/></svg>
          <span class="hide-xs">방송하기</span>
        </RouterLink>
        <div class="user">
          <button class="avatar" @click="menuOpen = !menuOpen">{{ auth.user.nickname.slice(0, 1) }}</button>
          <div v-if="menuOpen" class="menu" @click="menuOpen = false">
            <div class="menu-head">
              <strong>{{ auth.user.nickname }}</strong>
              <span v-if="auth.user.role === 'ADMIN'" class="chip">운영자</span>
            </div>
            <RouterLink to="/me">마이페이지</RouterLink>
            <RouterLink to="/me/broadcasts">내 방송</RouterLink>
            <RouterLink to="/onboarding">응원팀 설정</RouterLink>
            <RouterLink v-if="auth.user.role === 'ADMIN'" to="/admin">운영 관리</RouterLink>
            <button @click.stop="logout">로그아웃</button>
          </div>
        </div>
      </template>
      <RouterLink v-else to="/login" class="btn primary sm">로그인</RouterLink>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky; top: 0; z-index: 30; height: var(--header-h);
  display: flex; align-items: center; gap: 10px; padding: 0 16px;
  background: rgba(18, 19, 22, .96); backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border);
}
.icon-btn { background: none; border: none; color: var(--text); cursor: pointer; padding: 6px; border-radius: 8px; display: inline-flex; }
.icon-btn:hover { background: var(--bg-elev-2); }
.logo { display: flex; align-items: center; gap: 8px; font-weight: 900; font-size: 19px; letter-spacing: -.03em; }
.search { position: relative; flex: 0 1 440px; margin: 0 auto; }
.search-input {
  width: 100%; height: 40px; padding: 0 40px 0 16px; border-radius: 999px;
  border: 1px solid var(--border); background: var(--bg-elev); color: var(--text); font-size: 14px;
}
.search-input:focus { outline: none; border-color: var(--accent); }
.search-icon { position: absolute; right: 14px; top: 11px; color: var(--text-sub); }
.search-pop {
  position: absolute; top: 46px; left: 0; right: 0; z-index: 40;
  background: var(--bg-elev); border: 1px solid var(--border); border-radius: 12px; padding: 6px;
  box-shadow: 0 12px 30px rgba(0,0,0,.4);
}
.search-item {
  width: 100%; display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: 8px;
  background: none; border: none; color: var(--text); cursor: pointer; text-align: left; font-size: 14px;
}
.search-item:hover { background: var(--bg-elev-2); }
.right { display: flex; align-items: center; gap: 10px; }
.user { position: relative; }
.avatar {
  width: 34px; height: 34px; border-radius: 50%; border: none; cursor: pointer;
  background: var(--accent-soft); color: #c9bbff; font-weight: 900;
}
.menu {
  position: absolute; right: 0; top: 42px; min-width: 180px; z-index: 40;
  background: var(--bg-elev); border: 1px solid var(--border); border-radius: 12px; padding: 6px;
  display: flex; flex-direction: column; box-shadow: 0 12px 30px rgba(0,0,0,.4);
}
.menu-head { display: flex; align-items: center; gap: 6px; padding: 8px 10px; border-bottom: 1px solid var(--border); margin-bottom: 4px; }
.menu a, .menu button {
  text-align: left; padding: 8px 10px; border-radius: 8px; font-size: 14px;
  background: none; border: none; color: var(--text); cursor: pointer;
}
.menu a:hover, .menu button:hover { background: var(--bg-elev-2); }
@media (max-width: 720px) {
  .logo-text { display: none; }
  .search { flex: 1; }
  .hide-xs { display: none; }
}
</style>
