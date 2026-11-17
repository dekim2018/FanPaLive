<script setup>
import { tr } from '../i18n'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { broadcastApi, recordingApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import LivePlayer from '../components/LivePlayer.vue'
import ChatPanel from '../components/ChatPanel.vue'
import DonationPanel from '../components/DonationPanel.vue'
import StatusBadge from '../components/StatusBadge.vue'
import TeamCrest from '../components/TeamCrest.vue'
import HeartButton from '../components/HeartButton.vue'
import ReportDialog from '../components/ReportDialog.vue'
import { reportApi } from '../api/interactions'
import { viewerApi } from '../api/fandom'
import { teamStyle } from '../lib/team'
import { formatCount } from '../lib/format'
import { useLiveEvents } from '../composables/useLiveEvents'
import { elapsedSince, formatDate, formatDuration } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const broadcastId = route.params.id
const events = useLiveEvents(broadcastId)

const broadcast = ref(null)
const error = ref('')
const recordings = ref([])
const viewers = ref(null)
const tick = ref(0)
let pollTimer = null
let tickTimer = null

const isOwner = computed(() => broadcast.value && auth.user?.id === broadcast.value.ownerId)
const isLive = computed(() => broadcast.value?.status === 'LIVE')
const reportOpen = ref(false)

/** 운영자: 방송 중단(종료 + 목록에서 숨김 + 재시작 막기) */
async function moderate() {
  const reason = prompt(tr('중단 사유를 적어 주세요. 방송자에게 보입니다.'), tr('중계권 영상 재송출'))
  if (reason === null) return
  try {
    await reportApi.endBroadcast(broadcast.value.id, reason)
    toast.success(tr('방송을 중단했습니다.'))
    await load()
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

async function load() {
  try {
    broadcast.value = await broadcastApi.get(broadcastId)
    error.value = ''
  } catch (e) {
    // 5초 새로고침이 한 번 실패했다고 플레이어와 채팅을 내리지 않는다(시청이 끊긴다).
    // 처음 불러오기 실패나 방송이 삭제된 경우(404)만 오류 화면을 보인다.
    if (!broadcast.value || e?.response?.status === 404) {
      error.value = e?.response?.status === 404 ? tr('존재하지 않거나 삭제된 방송입니다.') : errorMessage(e)
    }
  }
}

async function loadViewers() {
  if (!isLive.value) return
  try {
    viewers.value = (await viewerApi.stats([broadcast.value.id]))[broadcast.value.id] || null
  } catch {
    viewers.value = null
  }
}

async function loadRecordings() {
  try {
    recordings.value = (await recordingApi.list({ broadcastId, size: 5 })).content
  } catch {
    recordings.value = []
  }
}

async function remove() {
  if (!confirm(tr('방송을 삭제할까요?'))) return
  try {
    await broadcastApi.remove(broadcastId)
    toast.success(tr('삭제했습니다.'))
    router.replace('/me/broadcasts')
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

onMounted(async () => {
  await load()
  loadRecordings()
  setTimeout(loadViewers, 800)
  events.connect()
  // 방송 시작/종료 상태 변화를 감지해서 플레이어를 켜고 끈다.
  pollTimer = setInterval(async () => {
    const wasLive = isLive.value
    await load()
    loadViewers()
    if (wasLive && !isLive.value) {
      toast.show(tr('방송이 종료되었습니다.'))
      loadRecordings()
    }
  }, 5000)
  tickTimer = setInterval(() => { tick.value++ }, 1000)
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  clearInterval(tickTimer)
})
</script>

<template>
  <div v-if="error" class="empty">{{ error }}</div>
  <div v-else-if="broadcast" class="layout-watch" :style="teamStyle(broadcast.side?.targetColor, broadcast.side?.targetTextColor)">
    <div class="stack">
      <LivePlayer :url="broadcast.watchUrl" :active="isLive">
        <template #overlay="{ state }">
          <template v-if="!isLive">
            <div v-if="broadcast.status === 'ENDED'">{{ tr('방송이 종료되었습니다.') }}</div>
            <div v-else>{{ tr('아직 방송이 시작되지 않았습니다.') }}</div>
            <RouterLink v-if="recordings.length" :to="`/recordings/${recordings[0].id}`" class="btn primary sm">{{ tr('다시보기') }}</RouterLink>
          </template>
          <div v-else-if="state === 'connecting'">{{ tr('연결 중…') }}</div>
          <div v-else>{{ tr('방송자의 송출을 기다리는 중…') }}</div>
        </template>
      </LivePlayer>

      <div class="card stack info" :class="{ side: broadcast.side }">
        <div class="row">
          <StatusBadge :status="broadcast.status" :online="broadcast.streamOnline" />
          <h1 style="margin:0; font-size:20px">{{ broadcast.title }}</h1>
        </div>
        <div v-if="broadcast.side" class="row side-row">
          <RouterLink :to="`/targets/${broadcast.side.targetId}`" class="row nowrap">
            <TeamCrest :name="broadcast.side.targetName" :short-name="broadcast.side.targetShortName" :color="broadcast.side.targetColor" :color2="broadcast.side.targetTextColor" :size="40" />
            <div>
              <div class="xsmall muted">{{ tr('{0} 편파중계', broadcast.side.leagueCode) }}</div>
              <strong>{{ tr(broadcast.side.targetName) }}</strong>
            </div>
          </RouterLink>
          <span class="spacer"></span>
          <RouterLink v-if="broadcast.side.matchId" :to="`/matches/${broadcast.side.matchId}`" class="btn sm ghost">{{ tr('매치룸 응원 대결') }}</RouterLink>
          <HeartButton :target-id="broadcast.side.targetId" source="BROADCAST" />
        </div>
        <div class="row small muted">
          <strong style="color:var(--text)">{{ broadcast.ownerNickname }}</strong>
          <span v-if="isLive && viewers" class="num">{{ tr('{0}명 시청 중, 누적 약 {1}명', formatCount(viewers.current), formatCount(viewers.unique)) }}</span>
          <span v-if="isLive" :data-tick="tick">{{ tr('방송 시간 {0}', elapsedSince(broadcast.startedAt)) }}</span>
          <span v-else-if="broadcast.endedAt">{{ tr('종료 {0}', formatDate(broadcast.endedAt)) }}</span>
          <span v-if="broadcast.sourceType">{{ broadcast.sourceType === 'PC_SCREEN' ? tr('PC 화면 송출') : tr('모바일 카메라 송출') }}</span>
        </div>
        <p v-if="broadcast.description" class="desc">{{ broadcast.description }}</p>
        <div v-if="broadcast.moderated" class="notice small" role="status">{{ tr('운영 정책 위반으로 중단된 방송입니다. 다시 시작할 수 없습니다.') }}</div>
        <div v-if="auth.user && !isOwner" class="row">
          <button type="button" class="btn sm ghost" @click="reportOpen = true">{{ tr('방송 신고') }}</button>
          <button v-if="auth.user.role === 'ADMIN' && !broadcast.moderated" type="button" class="btn sm danger" @click="moderate">{{ tr('방송 중단(운영)') }}</button>
        </div>
        <div v-if="isOwner" class="row">
          <RouterLink :to="`/studio/${broadcast.id}`" class="btn primary sm">{{ tr('스튜디오(송출 제어)') }}</RouterLink>
          <RouterLink :to="`/broadcasts/${broadcast.id}/edit`" class="btn sm">{{ tr('정보 수정') }}</RouterLink>
          <button class="btn sm danger" :disabled="isLive" @click="remove">{{ tr('삭제') }}</button>
        </div>
      </div>

      <div v-if="recordings.length" class="card stack">
        <h3 style="margin:0">{{ tr('이 방송의 다시보기') }}</h3>
        <RouterLink v-for="r in recordings" :key="r.id" :to="`/recordings/${r.id}`" class="row small rec">
          <StatusBadge :status="r.status === 'READY' ? 'VOD' : r.status" />
          <span>{{ formatDate(r.startedAt) }}</span>
          <span class="muted">{{ r.status === 'READY' ? formatDuration(r.durationSeconds) : tr('녹화 중') }}</span>
        </RouterLink>
      </div>
    </div>

    <div class="stack">
      <DonationPanel :broadcast="broadcast" :events="events" />
      <ChatPanel :broadcast-id="broadcastId" :events="events" height="360px" :team-name="broadcast.side?.targetShortName || broadcast.side?.targetName || ''" />
    </div>
    <ReportDialog :open="reportOpen" type="BROADCAST" :target-id="broadcast.id"
                  :summary="`${broadcast.ownerNickname}: ${broadcast.title}`" @close="reportOpen = false" />
  </div>
</template>

<style scoped>
.info.side { border-top: 3px solid var(--team); }
.side-row { padding: 10px 0; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); }
.desc { white-space: pre-wrap; margin: 0; color: var(--text); }
.rec { padding: 6px 0; border-top: 1px solid var(--border); }
.notice { padding: 10px 12px; border-radius: 8px; background: var(--warn-soft); color: var(--warn); font-weight: 600; }
</style>
