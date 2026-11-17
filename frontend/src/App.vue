<script setup>
import { onMounted, ref, watch } from 'vue'
import AppHeader from './components/AppHeader.vue'
import AppSidebar from './components/AppSidebar.vue'
import ToastHost from './components/ToastHost.vue'
import { useAuthStore } from './stores/auth'
import { useFandomStore } from './stores/fandom'

const auth = useAuthStore()
const fandom = useFandomStore()
// 넓은 화면은 메뉴를 펼쳐 두고, 좁은 화면은 접어 둔다.
const sidebarOpen = ref(window.innerWidth > 1100)

function closeOnNarrow() {
  if (window.innerWidth <= 1100) sidebarOpen.value = false
}

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
        <!-- 같은 화면 안에서 필터만 바뀌면(쿼리) 다시 그리지 않고, 경로가 바뀔 때만 새로 만든다 -->
        <RouterView :key="$route.path" />
      </div>
    </main>
  </div>
  <ToastHost />
</template>
