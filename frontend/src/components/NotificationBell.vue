<script setup>
import { tr } from '../i18n'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { formatAgo } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { notificationBody, notificationTitle, useNotificationStore } from '../stores/notifications'
import { useToastStore } from '../stores/toast'

/** 헤더 알림 종. 응원팀 편파방 시작, 승부 예측 결과를 보여 준다. */
const auth = useAuthStore()
const store = useNotificationStore()
const toast = useToastStore()
const router = useRouter()
const open = ref(false)

function onNew(data) {
  toast.show(data.body ? `${tr(data.title)}: ${data.body}` : tr(data.title))
}

function start() {
  if (auth.isLoggedIn) store.connect(onNew)
  else store.reset()
}

async function toggle() {
  open.value = !open.value
  if (open.value && !store.loaded) await store.load()
}

function go(n) {
  store.markRead(n.id)
  open.value = false
  if (n.link) router.push(n.link)
}

onMounted(start)
watch(() => auth.isLoggedIn, start)
onBeforeUnmount(() => store.disconnect())
</script>

<template>
  <div class="bell-wrap" @keydown.esc="open = false">
    <button type="button" class="icon-btn bell" :aria-label="store.unread ? tr('알림 {0}개 안 읽음', store.unread) : tr('알림')"
            aria-haspopup="true" :aria-expanded="open" @click="toggle">
      <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true"><path fill="currentColor" d="M12 22a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 22zm7-6V11a7 7 0 0 0-5.5-6.84V3a1.5 1.5 0 0 0-3 0v1.16A7 7 0 0 0 5 11v5l-2 2v1h18v-1z"/></svg>
      <span v-if="store.unread" class="count" aria-hidden="true">{{ store.unread > 99 ? '99+' : store.unread }}</span>
    </button>
    <div v-if="open" class="panel" role="dialog" :aria-label="tr('알림')">
      <div class="panel-head">
        <strong>{{ tr('알림') }}</strong>
        <span class="spacer"></span>
        <button v-if="store.unread" type="button" class="link" @click="store.markAllRead()">{{ tr('모두 읽음') }}</button>
      </div>
      <div v-if="!store.items.length" class="empty-note small muted">{{ tr('새 알림이 없습니다. 응원팀을 팔로우하면 편파방이 시작될 때 알려 드려요.') }}</div>
      <ul v-else class="list">
        <li v-for="n in store.items" :key="n.id">
          <button type="button" class="item" :class="{ unread: !n.read }" @click="go(n)">
            <span class="dot" :class="{ on: !n.read }" aria-hidden="true"></span>
            <span class="text">
              <span class="title">{{ notificationTitle(n) }}</span>
              <span v-if="n.body" class="body small muted">{{ notificationBody(n) }}</span>
              <span class="xsmall dim">{{ formatAgo(n.createdAt) }}<template v-if="!n.read">, {{ tr('안 읽음') }}</template></span>
            </span>
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.bell-wrap { position: relative; }
.icon-btn { background: none; border: none; color: var(--text); cursor: pointer; padding: 9px; border-radius: 8px; display: inline-flex; }
.icon-btn:hover { background: var(--bg-elev-2); }
.bell { position: relative; }
.count {
  position: absolute; top: 2px; right: 0; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 9px;
  background: var(--live); color: #fff; font-size: 11px; font-weight: 800; line-height: 18px; text-align: center;
}
.panel {
  position: absolute; right: -40px; top: 44px; width: min(360px, 92vw); max-height: 70vh; overflow-y: auto; z-index: 45;
  background: var(--bg-elev); border: 1px solid var(--border); border-radius: 12px; box-shadow: var(--shadow-lg);
}
.panel-head { display: flex; align-items: center; padding: 12px 14px; border-bottom: 1px solid var(--border); position: sticky; top: 0; background: var(--bg-elev); }
.link { background: none; border: none; color: var(--text-sub); font-size: 13px; font-weight: 600; cursor: pointer; padding: 4px 6px; border-radius: 6px; }
.link:hover { color: var(--text); background: var(--bg-elev-2); }
.empty-note { padding: 20px 16px; line-height: 1.5; }
.list { list-style: none; margin: 0; padding: 4px; }
.item {
  display: flex; gap: 10px; width: 100%; text-align: left; padding: 10px; border: none; border-radius: 8px;
  background: none; color: var(--text); cursor: pointer; font-family: inherit;
}
.item:hover { background: var(--bg-elev-2); }
.item.unread { background: var(--accent-soft); }
.dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; background: transparent; }
.dot.on { background: var(--accent); }
.text { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.title { font-weight: 700; font-size: 14px; }
.body { overflow-wrap: anywhere; }
@media (max-width: 560px) { .panel { position: fixed; left: 4vw; right: 4vw; top: calc(var(--header-h) + 4px); width: auto; } }
</style>
