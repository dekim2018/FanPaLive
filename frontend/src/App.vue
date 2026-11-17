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
  <AppHeader @toggle-sidebar="sidebarOpen = !sidebarOpen" />
  <div class="app-body">
    <AppSidebar :open="sidebarOpen" @close="closeOnNarrow" />
    <main class="app-main">
      <div class="container">
        <RouterView :key="$route.fullPath" />
      </div>
    </main>
  </div>
  <ToastHost />
</template>
