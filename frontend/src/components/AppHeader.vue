<script setup>
import { tr } from '../i18n'
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fandomApi } from '../api/fandom'
import LocaleSwitch from './LocaleSwitch.vue'
import NotificationBell from './NotificationBell.vue'
import TeamCrest from './TeamCrest.vue'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'
import { useNotificationStore } from '../stores/notifications'
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
  useNotificationStore().reset()
  toast.success(tr('로그아웃되었습니다.'))
  router.push('/')
}
</script>

<template>
  <header class="header">
    <button class="icon-btn" type="button" :aria-label="tr('메뉴 열고 닫기')" @click="emit('toggle-sidebar')">
      <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true"><path fill="currentColor" d="M3 6h18v2H3zm0 5h18v2H3zm0 5h18v2H3z"/></svg>
    </button>
    <RouterLink to="/" class="logo" :aria-label="tr('팬파라이브 홈')">
      <!-- 로고: 채팅 말풍선 + 한쪽으로 기운 F(편파) + LIVE 점 -->
      <svg viewBox="0 0 40 40" width="30" height="30" aria-hidden="true">
        <path d="M6 4H34A4 4 0 0 1 38 8V26A4 4 0 0 1 34 30H17L9 37V30H6A4 4 0 0 1 2 26V8A4 4 0 0 1 6 4Z" fill="#5e5ede"/>
        <g transform="skewX(-12) translate(4 0)"><path d="M14 10H27V14H18.5V16.5H25V20.5H18.5V25H14Z" fill="#fff"/></g>
        <circle cx="31.5" cy="10.5" r="3" fill="#ff5a4e"/>
      </svg>
      <span class="logo-text wordmark">{{ tr('팬파라이브') }}</span>
    </RouterLink>

    <form class="search" role="search" @submit.prevent="submitSearch">
      <label for="global-search" class="sr-only">{{ tr('구단, 선수 검색') }}</label>
      <input id="global-search" v-model="q" class="search-input" type="search" name="q" autocomplete="off"
             spellcheck="false" :placeholder="tr('구단, 선수를 검색해보세요')"
             @focus="searchOpen = true" @blur="closeSearchLater" />
      <svg class="search-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path fill="currentColor" d="M10 2a8 8 0 0 1 6.3 12.9l5.4 5.4-1.4 1.4-5.4-5.4A8 8 0 1 1 10 2zm0 2a6 6 0 1 0 0 12 6 6 0 0 0 0-12z"/></svg>
      <div v-if="searchOpen && results.length" class="search-pop" :aria-label="tr('검색 결과')">
        <button v-for="t in results" :key="t.id" type="button" class="search-item" @mousedown.prevent="pick(t)">
          <TeamCrest :name="t.name" :short-name="t.shortName" :color="t.colorPrimary" :color2="t.colorSecondary" :size="28" />
          <span class="ellipsis">{{ tr(t.name) }}</span>
          <span class="muted xsmall">{{ tr(t.leagueName) }}</span>
        </button>
      </div>
    </form>

    <div class="right">
      <LocaleSwitch />
      <template v-if="auth.isLoggedIn">
        <NotificationBell />
        <RouterLink to="/broadcasts/new" class="btn primary sm studio" :aria-label="tr('방송하기')">
          <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path fill="currentColor" d="M4 6h11a2 2 0 0 1 2 2v1.5l4-2.5v10l-4-2.5V16a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z"/></svg>
          <span class="hide-xs">{{ tr('방송하기') }}</span>
        </RouterLink>
        <div class="user" @keydown.esc="menuOpen = false">
          <button class="avatar" type="button" :aria-label="tr('{0} 메뉴', auth.user.nickname)" aria-haspopup="menu"
                  :aria-expanded="menuOpen" @click="menuOpen = !menuOpen">{{ auth.user.nickname.slice(0, 1) }}</button>
          <div v-if="menuOpen" class="menu" @click="menuOpen = false">
            <div class="menu-head">
              <strong>{{ auth.user.nickname }}</strong>
              <span v-if="auth.user.role === 'ADMIN'" class="chip">{{ tr('운영자') }}</span>
            </div>
            <RouterLink to="/me">{{ tr('마이페이지') }}</RouterLink>
            <RouterLink to="/me/broadcasts">{{ tr('내 방송') }}</RouterLink>
            <RouterLink to="/onboarding">{{ tr('응원팀 설정') }}</RouterLink>
            <RouterLink v-if="auth.user.role === 'ADMIN'" to="/admin">{{ tr('운영 관리') }}</RouterLink>
            <button @click.stop="logout">{{ tr('로그아웃') }}</button>
          </div>
        </div>
      </template>
      <RouterLink v-else to="/login" class="btn primary sm">{{ tr('로그인') }}</RouterLink>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky; top: 0; z-index: 30; height: var(--header-h);
  display: flex; align-items: center; gap: 10px;
  padding: 0 max(16px, env(safe-area-inset-right)) 0 max(16px, env(safe-area-inset-left));
  background: rgba(255, 255, 255, .96); backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border);
}
.icon-btn { background: none; border: none; color: var(--text); cursor: pointer; padding: 9px; border-radius: 8px; display: inline-flex; }
.icon-btn:hover { background: var(--bg-elev-2); }
.logo { display: flex; align-items: center; gap: 8px; }
.logo-text { font-size: 22px; line-height: 1; color: var(--text); padding-top: 2px; }
.search { position: relative; flex: 0 1 440px; margin: 0 auto; }
.search-input {
  width: 100%; height: 40px; padding: 0 40px 0 16px; border-radius: 999px;
  border: 1px solid var(--border-strong); background: var(--bg); color: var(--text); font-size: 16px;
}
.search-input::placeholder { color: var(--text-dim); }
.search-input:focus-visible { outline: none; border-color: var(--accent); box-shadow: var(--focus); background: var(--bg-elev); }
.search-icon { position: absolute; right: 14px; top: 11px; color: var(--text-sub); }
.search-pop {
  position: absolute; top: 46px; left: 0; right: 0; z-index: 40;
  background: var(--bg-elev); border: 1px solid var(--border); border-radius: 12px; padding: 6px;
  box-shadow: var(--shadow-lg);
}
.search-item {
  width: 100%; display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: 8px;
  background: none; border: none; color: var(--text); cursor: pointer; text-align: left; font-size: 14px;
}
.search-item:hover { background: var(--bg-elev-2); }
.right { display: flex; align-items: center; gap: 10px; }
.user { position: relative; }
.avatar {
  width: 40px; height: 40px; border-radius: 50%; border: none; cursor: pointer;
  background: var(--accent-soft); color: var(--accent-text); font-weight: 900;
  box-shadow: inset 0 0 0 1px var(--key);
}
.menu {
  position: absolute; right: 0; top: 42px; min-width: 180px; z-index: 40;
  background: var(--bg-elev); border: 1px solid var(--border); border-radius: 12px; padding: 6px;
  display: flex; flex-direction: column; box-shadow: var(--shadow-lg);
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
