<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import AppSidebar from './components/AppSidebar.vue'
import ToastHost from './components/ToastHost.vue'
import { useAuthStore } from './stores/auth'
import { useFandomStore } from './stores/fandom'
import { consumeSynced } from './lib/query'

const auth = useAuthStore()
const router = useRouter()
const fandom = useFandomStore()
// 넓은 화면은 메뉴를 펼쳐 두고, 좁은 화면은 접어 둔다.
const sidebarOpen = ref(window.innerWidth > 1100)

function closeOnNarrow() {
  if (window.innerWidth <= 1100) sidebarOpen.value = false
}

// 같은 화면에서 필터만 바꿔 주소(쿼리)를 맞출 때는 다시 그리지 않는다.
// 사이드바 링크, 뒤로 가기처럼 바깥에서 쿼리가 바뀌면 화면을 새로 만들어 주소와 화면을 맞춘다.
const reloads = ref(0)
router.afterEach((to, from, failure) => {
  const synced = consumeSynced(to)
  if (!failure && !synced && to.path === from.path && to.fullPath !== from.fullPath) reloads.value++
})

onMounted(() => {
  fandom.init()
  if (auth.isLoggedIn) fandom.loadMyFans()
})
watch(() => auth.isLoggedIn, (loggedIn) => {
  if (loggedIn) fandom.loadMyFans(true)
  else fandom.clearMine()
})
</script>

<template>
  <a href="#main" class="skip-link">본문으로 건너뛰기</a>
  <AppHeader @toggle-sidebar="sidebarOpen = !sidebarOpen" />
  <div class="app-body">
    <AppSidebar :open="sidebarOpen" @close="closeOnNarrow" />
    <main id="main" class="app-main" tabindex="-1">
      <div class="container">
        <RouterView :key="`${$route.path}#${reloads}`" />
      </div>
    </main>
  </div>
  <ToastHost />
</template>
