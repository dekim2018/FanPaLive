<script setup>
import { tr } from '../i18n'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'
import TeamCrest from './TeamCrest.vue'

/**
 * 왼쪽 메뉴: 주요 메뉴, 내 응원팀(팔로잉 채널처럼), 리그 바로가기.
 * 좁은 화면에서는 헤더의 메뉴 버튼으로 열고 닫는 서랍이 된다.
 */
defineProps({ open: { type: Boolean, default: true } })
const emit = defineEmits(['close'])
const auth = useAuthStore()
const fandom = useFandomStore()

const MENU = [
  { to: '/', label: tr('홈'), icon: 'M3 11l9-8 9 8v10h-6v-6H9v6H3z' },
  { to: '/live', label: tr('전체 LIVE'), icon: 'M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8zm-8.5 4a8.5 8.5 0 0 1 2.5-6l1.4 1.4a6.5 6.5 0 0 0 0 9.2L6 18a8.5 8.5 0 0 1-2.5-6zm17 0a8.5 8.5 0 0 1-2.5 6l-1.4-1.4a6.5 6.5 0 0 0 0-9.2L18 6a8.5 8.5 0 0 1 2.5 6z' },
  { to: '/sports', label: tr('스포츠'), icon: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 2c1.6 0 3 .5 4.3 1.3L14 7.5l-2-1-2 1-2.3-2.2A8 8 0 0 1 12 4zm-6 3.3L8 9.5l-.5 3-2.7 1.4A8 8 0 0 1 6 7.3zM12 20a8 8 0 0 1-3.5-.8l.6-2.7 2.9-1 2.9 1 .6 2.7A8 8 0 0 1 12 20z' },
  { to: '/rankings', label: tr('응원 랭킹'), icon: 'M4 20V10h4v10zm6 0V4h4v16zm6 0v-7h4v7z' },
  { to: '/matches', label: tr('경기 일정'), icon: 'M7 2v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2V2h-2v2H9V2zm-2 8h14v10H5z' },
  { to: '/clips', label: tr('하이라이트'), icon: 'M4 5h16a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zm6 4v6l5-3z' },
  { to: '/hall-of-fame', label: tr('명예의 전당'), icon: 'M7 3h10v2h3v3a4 4 0 0 1-4 4h-.3A5 5 0 0 1 13 14.9V17h3v4H8v-4h3v-2.1A5 5 0 0 1 8.3 12H8a4 4 0 0 1-4-4V5h3zm0 4H6v1a2 2 0 0 0 1 1.7zm10 0v2.7A2 2 0 0 0 18 8V7z' },
  { to: '/recordings', label: tr('다시보기'), icon: 'M12 4a8 8 0 1 1-7.4 5H2l3.5-4L9 9H6.7A6 6 0 1 0 12 6z' },
  { to: '/requests', label: tr('팀/선수 신청'), icon: 'M11 4h2v7h7v2h-7v7h-2v-7H4v-2h7z' }
]
</script>

<template>
  <div v-if="open" class="scrim" aria-hidden="true" @click="emit('close')"></div>
  <aside class="sidebar" :class="{ open }" :aria-label="tr('사이드 메뉴')">
    <nav class="menu" :aria-label="tr('주요 메뉴')">
      <RouterLink v-for="m in MENU" :key="m.to" :to="m.to" class="item" @click="emit('close')">
        <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path fill="currentColor" :d="m.icon"/></svg>
        <span>{{ m.label }}</span>
      </RouterLink>
    </nav>

    <div class="group">
      <div class="group-title">{{ tr('내 응원팀') }}</div>
      <template v-if="auth.isLoggedIn && fandom.myFans.length">
        <RouterLink v-for="f in fandom.myFans" :key="f.target.id" :to="`/targets/${f.target.id}`" class="item team" @click="emit('close')">
          <TeamCrest :name="f.target.name" :short-name="f.target.shortName" :color="f.target.colorPrimary" :color2="f.target.colorSecondary" :size="26" />
          <span class="ellipsis">{{ tr(f.target.name) }}</span>
          <span v-if="f.primary" class="star" :aria-label="tr('주 응원팀')">★</span>
        </RouterLink>
      </template>
      <RouterLink v-else-if="auth.isLoggedIn" to="/onboarding" class="item hint">{{ tr('응원팀을 골라 보세요') }}</RouterLink>
      <RouterLink v-else to="/login" class="item hint">{{ tr('로그인하고 팬 등록하기') }}</RouterLink>
    </div>

    <nav class="group" :aria-label="tr('리그')">
      <div class="group-title">{{ tr('리그') }}</div>
      <RouterLink v-for="l in fandom.leagues" :key="l.code" :to="`/leagues/${l.code}`" class="item league" @click="emit('close')">
        <span class="code">{{ l.code.slice(0, 3) }}</span>
        <span class="ellipsis">{{ tr(l.name) }}</span>
      </RouterLink>
    </nav>

    <div class="foot xsmall dim">{{ tr('팬파라이브는 팬이 만드는 편파중계 플랫폼입니다. 구단 로고와 명칭의 권리는 각 구단에 있습니다.') }}</div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: sticky; top: var(--header-h); align-self: flex-start;
  width: var(--sidebar-w); height: calc(100vh - var(--header-h)); overflow-y: auto; flex-shrink: 0;
  padding: 12px 10px 24px; border-right: 1px solid var(--border); background: var(--bg-elev);
  overscroll-behavior: contain;
}
.scrim { display: none; }
.menu, .group { display: flex; flex-direction: column; gap: 2px; }
.group { margin-top: 18px; padding-top: 14px; border-top: 1px solid var(--border); }
.group-title { font-size: 13px; font-weight: 800; color: var(--text-sub); padding: 0 10px 6px; }
.item {
  display: flex; align-items: center; gap: 12px; padding: 8px 10px; border-radius: 8px;
  font-size: 14px; font-weight: 600; color: var(--text-sub); min-width: 0; min-height: 40px;
}
.item:hover { background: var(--bg-elev-2); color: var(--text); }
.item.router-link-exact-active { background: var(--accent-soft); color: var(--accent-text); box-shadow: inset 3px 0 0 var(--accent); }
.team { gap: 10px; }
.star { margin-left: auto; color: var(--gold); font-size: 12px; }
.hint { color: var(--text-sub); font-size: 13px; }
.code {
  width: 26px; height: 26px; border-radius: 6px; flex-shrink: 0; background: var(--bg-elev-2);
  display: inline-flex; align-items: center; justify-content: center; font-size: 9px; font-weight: 900; color: var(--text-sub);
}
.foot { margin-top: 24px; padding: 0 10px; line-height: 1.5; }
@media (max-width: 1100px) {
  .sidebar {
    position: fixed; left: 0; top: var(--header-h); z-index: 25;
    transform: translateX(-100%); transition: transform .2s, visibility .2s;
    visibility: hidden;
  }
  .sidebar.open { visibility: visible; }
  .sidebar.open { transform: none; box-shadow: var(--shadow-lg); }
  .scrim { display: block; position: fixed; inset: var(--header-h) 0 0 0; z-index: 24; background: var(--overlay); }
}
@media (min-width: 1101px) {
  .sidebar:not(.open) { display: none; }
}
</style>
