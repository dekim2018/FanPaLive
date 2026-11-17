<script setup>
import { tr } from '../i18n'
import { nextTick, onMounted, ref } from 'vue'
import { commentApi, reportApi } from '../api/interactions'
import { errorMessage } from '../api/http'
import LevelBadge from './LevelBadge.vue'
import ReportDialog from './ReportDialog.vue'
import TeamBadge from './TeamBadge.vue'
import { useAuthStore } from '../stores/auth'
import { useFandomStore } from '../stores/fandom'
import { useToastStore } from '../stores/toast'
import TrText from './TrText.vue'

/**
 * 방송별 댓글(채팅). 목록은 REST 로 불러오고, 새 댓글/수정/삭제는 SSE 이벤트로 실시간 반영한다.
 */
const props = defineProps({
  broadcastId: { type: [String, Number], required: true },
  events: { type: Object, required: true },
  height: { type: String, default: '520px' }
})

const auth = useAuthStore()
const fandom = useFandomStore()
const toast = useToastStore()
const comments = ref([])
const content = ref('')
const sending = ref(false)
const editingId = ref(null)
const editingText = ref('')
const listEl = ref(null)
const hasMore = ref(false)
let page = 0

async function load(more = false) {
  try {
    const res = await commentApi.list(props.broadcastId, { page, size: 50 })
    const items = [...res.content].reverse()
    if (more) {
      // 첫 페이지 이후 새 댓글이 쌓이면 다음 페이지가 밀려서 이미 보이는 댓글이 다시 온다.
      const shown = new Set(comments.value.map((c) => c.id))
      comments.value = [...items.filter((c) => !shown.has(c.id)), ...comments.value]
    } else {
      comments.value = items
    }
    hasMore.value = res.page + 1 < res.totalPages
    fandom.fetchBadges(comments.value.map((c) => c.authorId))
    fandom.fetchLevels(comments.value.map((c) => c.authorId))
    if (!more) scrollToBottom()
  } catch (e) {
    toast.error(errorMessage(e, tr('댓글을 불러오지 못했습니다.')))
  }
}

function loadMore() {
  page += 1
  load(true)
}

function upsert(c) {
  const idx = comments.value.findIndex((x) => x.id === c.id)
  if (idx >= 0) comments.value[idx] = { ...comments.value[idx], ...c }
  else {
    comments.value.push(c)
    fandom.fetchBadges([c.authorId])
    fandom.fetchLevels([c.authorId])
    scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight
}

async function send() {
  if (!content.value.trim() || sending.value) return
  sending.value = true
  try {
    const created = await commentApi.write(props.broadcastId, content.value)
    upsert(created)
    content.value = ''
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    sending.value = false
  }
}

function startEdit(c) {
  editingId.value = c.id
  editingText.value = c.content
}

async function saveEdit(c) {
  try {
    upsert(await commentApi.edit(c.id, editingText.value))
    editingId.value = null
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

async function remove(c) {
  if (!confirm(tr('댓글을 삭제할까요?'))) return
  try {
    await commentApi.remove(c.id)
    comments.value = comments.value.filter((x) => x.id !== c.id)
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

const reporting = ref(null)

/** 운영자: 신고 없이 바로 댓글 숨김(모든 시청자 화면에서 내려간다) */
async function hide(c) {
  if (!confirm(tr('이 댓글을 숨길까요? 모든 시청자 화면에서 사라집니다.'))) return
  try {
    await reportApi.hideComment(c.id)
    comments.value = comments.value.filter((x) => x.id !== c.id)
    toast.success(tr('댓글을 숨겼습니다.'))
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

props.events.on('comment.created', upsert)
props.events.on('comment.updated', upsert)
props.events.on('comment.deleted', ({ id }) => {
  comments.value = comments.value.filter((x) => x.id !== id)
})

onMounted(() => load())
</script>

<template>
  <section class="chat card">
    <div class="row chat-head">
      <h3 style="margin:0">{{ tr('채팅') }}</h3>
      <span class="spacer"></span>
      <span class="small" :class="events.connected.value ? 'ok' : 'muted'">
        {{ events.connected.value ? tr('● 실시간') : tr('○ 연결 중') }}
      </span>
    </div>
    <div ref="listEl" class="chat-list" :style="{ height }" role="log" aria-live="polite" :aria-label="tr('채팅 메시지')">
      <button v-if="hasMore" type="button" class="btn sm ghost block" @click="loadMore">{{ tr('이전 댓글 보기') }}</button>
      <div v-if="!comments.length" class="muted small center">{{ tr('첫 댓글을 남겨 보세요.') }}</div>
      <div v-for="c in comments" :key="c.id" class="chat-item">
        <template v-if="editingId === c.id">
          <input v-model="editingText" class="input" name="comment-edit" :aria-label="tr('댓글 수정')" autocomplete="off" maxlength="300" @keyup.enter="saveEdit(c)" />
          <div class="row">
            <button type="button" class="btn sm primary" @click="saveEdit(c)">{{ tr('저장') }}</button>
            <button type="button" class="btn sm" @click="editingId = null">{{ tr('취소') }}</button>
          </div>
        </template>
        <template v-else>
          <TeamBadge v-if="fandom.badges[c.authorId]?.length" :label="tr(fandom.badges[c.authorId][0].shortName)"
                     :color="fandom.badges[c.authorId][0].colorPrimary" :color2="fandom.badges[c.authorId][0].colorSecondary" class="cbadge" />
          <LevelBadge :level="fandom.levels[c.authorId]" hide-rookie class="cbadge" />
          <span class="author">{{ c.authorNickname }}</span>
          <span class="text">{{ c.content }}</span>
          <span v-if="c.edited" class="muted small"> {{ tr('(수정됨)') }}</span>
          <span v-if="auth.user && auth.user.id === c.authorId" class="actions">
            <button type="button" class="link" :aria-label="tr('내 댓글 수정: {0}', c.content)" @click="startEdit(c)">{{ tr('수정') }}</button>
            <button type="button" class="link" :aria-label="tr('내 댓글 삭제: {0}', c.content)" @click="remove(c)">{{ tr('삭제') }}</button>
          </span>
          <span v-else-if="auth.user" class="actions other">
            <button type="button" class="link" :aria-label="tr('댓글 신고: {0}', c.content)" @click="reporting = c">{{ tr('신고') }}</button>
            <button v-if="auth.user.role === 'ADMIN'" type="button" class="link danger" :aria-label="tr('댓글 숨김: {0}', c.content)" @click="hide(c)">{{ tr('숨김') }}</button>
          </span>
        </template>
      </div>
    </div>
    <form v-if="auth.isLoggedIn" class="row chat-form" @submit.prevent="send">
      <input v-model="content" class="input" name="comment" :aria-label="tr('채팅 입력')" autocomplete="off" maxlength="300" :placeholder="tr('두산 화이팅!…')" />
      <button type="submit" class="btn primary" :disabled="sending">{{ sending ? tr('전송 중…') : tr('전송') }}</button>
    </form>
    <div v-else class="muted small center">
      <TrText :text="tr('{0} 후 채팅에 참여할 수 있습니다.')"><template #p0><RouterLink :to="{ name: 'login', query: { redirect: $route.fullPath } }" class="ok">{{ tr('로그인') }}</RouterLink></template></TrText>
    </div>
    <ReportDialog :open="!!reporting" type="COMMENT" :target-id="reporting?.id"
                  :summary="reporting ? `${reporting.authorNickname}: ${reporting.content}` : ''" @close="reporting = null" />
  </section>
</template>

<style scoped>
.chat { display: flex; flex-direction: column; gap: 10px; }
.chat-list { overflow-y: auto; display: flex; flex-direction: column; gap: 6px; padding-right: 4px; }
.chat-item { font-size: 14px; word-break: break-all; line-height: 1.45; }
.cbadge { margin-right: 4px; }
.author { color: var(--accent-text); font-weight: 700; margin-right: 6px; }
.actions { margin-left: 6px; }
.link { background: none; border: none; color: var(--text-sub); font-size: 12px; cursor: pointer; padding: 4px 6px; min-height: 24px; border-radius: 4px; }
.link:hover { color: var(--text); }
.link.danger { color: var(--danger); }
/* 남의 댓글 신고 버튼은 마우스를 올리거나 키보드로 들어왔을 때만 오른쪽 위에 띄운다(줄바꿈으로 채팅이 길어지지 않게) */
.chat-item { position: relative; }
.actions.other { position: absolute; top: -2px; right: 0; background: var(--bg-elev); border-radius: 6px; box-shadow: var(--shadow); opacity: 0; pointer-events: none; transition: opacity .15s; }
.chat-item:hover .actions.other, .chat-item:focus-within .actions.other { opacity: 1; pointer-events: auto; }
@media (hover: none) { .actions.other { position: static; opacity: 1; pointer-events: auto; box-shadow: none; background: none; } }
.ok { color: var(--ok); }
.center { text-align: center; padding: 8px 0; }
.chat-form { flex-wrap: nowrap; }
@media (max-width: 960px) { .chat-list { height: 320px !important; } }
</style>
