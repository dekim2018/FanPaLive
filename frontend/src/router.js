import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('./views/HomeView.vue') },
  { path: '/live', name: 'live', component: () => import('./views/LiveListView.vue') },
  { path: '/sports', name: 'sports', component: () => import('./views/SportsView.vue') },
  { path: '/leagues/:code', name: 'league', component: () => import('./views/LeagueView.vue') },
  { path: '/targets/:id', name: 'target', component: () => import('./views/TargetView.vue') },
  { path: '/rankings', name: 'rankings', component: () => import('./views/RankingsView.vue') },
  { path: '/matches', name: 'matches', component: () => import('./views/MatchesView.vue') },
  { path: '/matches/:id', name: 'match', component: () => import('./views/MatchRoomView.vue') },
  { path: '/requests', name: 'requests', component: () => import('./views/TargetRequestView.vue') },
  { path: '/onboarding', name: 'onboarding', component: () => import('./views/OnboardingView.vue'), meta: { auth: true } },
  { path: '/me', name: 'me', component: () => import('./views/MyPageView.vue'), meta: { auth: true } },
  { path: '/admin', name: 'admin', component: () => import('./views/AdminView.vue'), meta: { auth: true, admin: true } },
  { path: '/login', name: 'login', component: () => import('./views/LoginView.vue') },
  { path: '/signup', name: 'signup', component: () => import('./views/SignupView.vue') },
  { path: '/broadcasts/new', name: 'broadcast-new', component: () => import('./views/BroadcastFormView.vue'), meta: { auth: true } },
  { path: '/broadcasts/:id/edit', name: 'broadcast-edit', component: () => import('./views/BroadcastFormView.vue'), meta: { auth: true } },
  { path: '/broadcasts/:id', name: 'watch', component: () => import('./views/WatchView.vue') },
  { path: '/studio/:id', name: 'studio', component: () => import('./views/StudioView.vue'), meta: { auth: true } },
  { path: '/me/broadcasts', name: 'my-broadcasts', component: () => import('./views/MyBroadcastsView.vue'), meta: { auth: true } },
  { path: '/recordings', name: 'recordings', component: () => import('./views/RecordingsView.vue') },
  { path: '/recordings/:id', name: 'recording', component: () => import('./views/RecordingDetailView.vue') },
  { path: '/donations/success', name: 'donation-success', component: () => import('./views/DonationResultView.vue'), meta: { auth: true }, props: { result: 'success' } },
  { path: '/donations/fail', name: 'donation-fail', component: () => import('./views/DonationResultView.vue'), props: { result: 'fail' } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('./views/NotFoundView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 화면 접근 제어는 UX 용도이며, 실제 권한 검증은 모든 API 에서 서버가 다시 한다.
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && auth.user?.role !== 'ADMIN') {
    return { name: 'home' }
  }
  return true
})

export default router
