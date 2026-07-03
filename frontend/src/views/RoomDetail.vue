<template>
  <div class="room-detail">
    <!-- 加载/错误状态 -->
    <div class="loading" v-if="loading">加载房间信息...</div>
    <div class="error-state" v-else-if="error">
      <p>{{ error }}</p>
      <button class="btn" @click="router.push('/rooms')">返回房间列表</button>
    </div>

    <!-- 房间主体 -->
    <template v-else-if="room">
      <!-- 顶部栏 -->
      <div class="room-header">
        <button class="btn-back" @click="handleLeave">← 退出房间</button>
        <div class="room-title">
          <h1>{{ room.name }}</h1>
          <span class="room-status-badge" :class="room.status?.toLowerCase()">
            {{ statusText(room.status) }}
          </span>
        </div>
        <div class="room-info-bar">
          <span>👑 {{ room.ownerNickname || '未知' }}</span>
          <span>👤 {{ memberCount }}/{{ room.maxMembers || 8 }}</span>

          <!-- 房主操作区 -->
          <template v-if="isOwner">
            <button class="btn-action-owner" @click="showDismissConfirm = true">解散房间</button>
          </template>
        </div>
      </div>

      <div class="room-content">
        <!-- 左侧：当前播放 + 队列 + 成员列表 -->
        <div class="left-panel">
          <!-- 当前播放 -->
          <div class="now-playing">
            <h3>🎵 正在播放</h3>
            <div class="song-info" v-if="room.currentSongTitle">
              <p class="song-title">{{ room.currentSongTitle }}</p>
              <p class="song-artist">{{ room.currentSongArtist || '未知艺人' }}</p>
              <p class="song-progress">进度: {{ formatTime(room.progress) }}</p>
            </div>
            <div class="no-song" v-else>
              <p>暂无歌曲</p>
              <p class="hint">房主可以添加歌曲到队列</p>
            </div>
          </div>

          <!-- 播放队列 -->
          <div class="queue-panel">
            <h3>📋 播放队列</h3>
            <div v-if="queue.length === 0" class="queue-empty">队列为空</div>
            <div v-for="(item, idx) in queue" :key="idx" class="queue-item">
              <span class="q-title">{{ item.songTitle || '歌曲 #' + item.songId }}</span>
              <button v-if="isOwner" class="q-remove" @click="removeFromQueue(item)" title="移除">✕</button>
            </div>
          </div>

          <!-- 成员列表 -->
          <div class="members-panel">
            <h3>👥 在线成员 ({{ memberCount }})</h3>
            <div class="member-list">
              <div class="member-item" v-for="m in members" :key="m.userId">
                <img :src="m.avatar || ''" class="member-avatar" @error="onAvatarError" />
                <span class="member-name">{{ m.nickname }}</span>
                <span class="owner-badge" v-if="m.isOwner">房主</span>
                <!-- 房主操作：踢人 + 转让 -->
                <template v-if="isOwner && !m.isOwner">
                  <button class="btn-kick" @click="kickMember(m)" title="踢出">🚫</button>
                  <button class="btn-transfer" @click="transferOwner(m)" title="转让房主">👑</button>
                </template>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：聊天面板 -->
        <div class="right-panel">
          <div class="chat-panel">
            <h3>💬 聊天</h3>
            <div class="chat-messages" ref="chatRef">
              <div class="msg" v-for="msg in messages" :key="msg.id || msg.timestamp"
                   :class="{ 'msg-system': msg.type === 'SYSTEM' }">
                <template v-if="msg.type === 'SYSTEM'">
                  <span class="system-text">{{ msg.content }}</span>
                </template>
                <template v-else>
                  <strong>{{ msg.nickname || msg.userId }}: </strong>
                  <span>{{ msg.content }}</span>
                </template>
                <span class="msg-time">{{ formatMsgTime(msg.createdAt) }}</span>
              </div>
            </div>
            <div class="chat-input">
              <input v-model="chatInput" @keyup.enter="sendChat"
                     placeholder="输入聊天消息..." maxlength="500" />
              <button @click="sendChat" :disabled="!chatInput.trim()">发送</button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 确认解散弹窗 -->
    <div v-if="showDismissConfirm" class="modal-overlay" @click.self="showDismissConfirm = false">
      <div class="modal-card">
        <h3>解散房间</h3>
        <p>确定要解散「{{ room?.name }}」吗？此操作不可撤销。</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showDismissConfirm = false">取消</button>
          <button class="btn-danger" @click="dismissRoom" :disabled="dismissing">{{ dismissing ? '处理中...' : '确认解散' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const roomId = computed(() => Number(route.params.id))
const room = ref(null)
const members = ref([])
const messages = ref([])
const queue = ref([])
const loading = ref(true)
const error = ref('')
const chatInput = ref('')
const ws = ref(null)
const heartbeatTimer = ref(null)
const chatRef = ref(null)
const memberCount = ref(0)
const showDismissConfirm = ref(false)
const dismissing = ref(false)

const isOwner = computed(() => room.value && authStore.user?.id === room.value.ownerId)

function onAvatarError(e) { e.target.src = '' }

onMounted(async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  await fetchRoomDetail()
  await fetchMembers()
  await fetchMessages()
  await fetchQueue()
  connectWebSocket()
})

onUnmounted(() => {
  disconnectWebSocket()
})

// ====== API 请求 ======

async function fetchRoomDetail() {
  try {
    const res = await request.get(`/rooms/${roomId.value}`)
    if (res.code === 200 && res.data) {
      room.value = res.data
      memberCount.value = res.data.memberCount || 0
    } else {
      error.value = '房间不存在或已解散'
    }
  } catch (e) {
    error.value = e.message || '获取房间信息失败'
  } finally {
    loading.value = false
  }
}

async function fetchMembers() {
  try {
    const res = await request.get(`/rooms/${roomId.value}/members`)
    if (res.code === 200) {
      members.value = res.data || []
      memberCount.value = members.value.length
    }
  } catch (e) {
    console.error('获取成员列表失败', e)
  }
}

async function fetchMessages() {
  try {
    const res = await request.get(`/rooms/${roomId.value}/messages?limit=50`)
    if (res.code === 200) {
      messages.value = res.data || []
      scrollToBottom()
    }
  } catch (e) {
    console.error('获取消息失败', e)
  }
}

async function fetchQueue() {
  try {
    const res = await request.get(`/rooms/${roomId.value}/queue`)
    if (res.code === 200) {
      queue.value = res.data || []
    }
  } catch { /* ignore */ }
}

// ====== WebSocket ======

function getWsUrl() {
  const token = sessionStorage.getItem('access_token')
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = location.host || 'localhost:8888'
  return `${protocol}//${host}/ws/room/${roomId.value}?token=${token}`
}

function connectWebSocket() {
  if (ws.value) return
  try {
    ws.value = new WebSocket(getWsUrl())
    ws.value.onopen = () => { console.log('WebSocket 已连接'); startHeartbeat() }
    ws.value.onmessage = (event) => {
      try { handleWsMessage(JSON.parse(event.data)) }
      catch (e) { console.error('解析 WS 消息失败', e) }
    }
    ws.value.onclose = (event) => {
      stopHeartbeat()
      ws.value = null
      if (event.code !== 1000) setTimeout(connectWebSocket, 3000)
    }
    ws.value.onerror = (err) => console.error('WebSocket 错误', err)
  } catch (e) {
    console.error('连接 WebSocket 失败', e)
  }
}

function disconnectWebSocket() {
  stopHeartbeat()
  if (ws.value) {
    ws.value.close(1000, '用户离开')
    ws.value = null
  }
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer.value = setInterval(() => {
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ type: 'HEARTBEAT' }))
    }
  }, 30000)
}

function stopHeartbeat() {
  if (heartbeatTimer.value) {
    clearInterval(heartbeatTimer.value)
    heartbeatTimer.value = null
  }
}

function handleWsMessage(msg) {
  switch (msg.type) {
    case 'HEARTBEAT':
      break
    case 'CHAT':
      messages.value.push({
        id: Date.now(), type: 'TEXT', userId: msg.userId,
        nickname: msg.nickname, content: msg.payload?.content || '',
        createdAt: msg.timestamp
      })
      scrollToBottom()
      break
    case 'SYSTEM':
      const payload = msg.payload || {}
      if (payload.action === 'JOIN' || payload.action === 'LEAVE') fetchMembers()
      if (payload.content) {
        messages.value.push({ id: Date.now(), type: 'SYSTEM', content: payload.content, timestamp: msg.timestamp })
        scrollToBottom()
      }
      break
    case 'SYNC':
      if (msg.payload) {
        const sync = msg.payload
        if (room.value) {
          if (sync.songId) room.value.currentSongId = sync.songId
          if (sync.isPlaying !== undefined) room.value.isPlaying = sync.isPlaying
          if (sync.progress !== undefined) room.value.progress = sync.progress
          if (sync.songId) fetchRoomDetail()
        }
      }
      break
  }
}

// ====== 房主操作 ======

async function kickMember(member) {
  if (!confirm(`确定将 ${member.nickname} 踢出房间？`)) return
  try {
    await request.post(`/rooms/${roomId.value}/kick/${member.userId}`)
    members.value = members.value.filter(m => m.userId !== member.userId)
    memberCount.value = members.value.length
  } catch (e) {
    alert(e.message || '踢出失败')
  }
}

async function transferOwner(member) {
  if (!confirm(`确定将房主转让给 ${member.nickname}？`)) return
  try {
    await request.post(`/rooms/${roomId.value}/transfer/${member.userId}`)
    alert('房主已转让')
    room.value.ownerId = member.userId
    room.value.ownerNickname = member.nickname
    fetchMembers()
  } catch (e) {
    alert(e.message || '转让失败')
  }
}

async function dismissRoom() {
  dismissing.value = true
  try {
    await request.post(`/rooms/${roomId.value}/dismiss`)
    disconnectWebSocket()
    router.push('/rooms')
  } catch (e) {
    alert(e.message || '解散失败')
  } finally {
    dismissing.value = false
    showDismissConfirm.value = false
  }
}

// ====== 队列管理 ======

async function removeFromQueue(item) {
  try {
    await request.delete(`/rooms/${roomId.value}/queue`, {
      data: { songId: item.songId }
    })
    queue.value = queue.value.filter(q => q.songId !== item.songId)
  } catch (e) {
    alert(e.message || '移除失败')
  }
}

// ====== 聊天 ======

function sendChat() {
  const content = chatInput.value.trim()
  if (!content || !ws.value || ws.value.readyState !== WebSocket.OPEN) return
  ws.value.send(JSON.stringify({ type: 'CHAT', payload: { content } }))
  chatInput.value = ''
}

async function handleLeave() {
  disconnectWebSocket()
  try { await request.post(`/rooms/${roomId.value}/leave`) }
  catch { /* ignore */ }
  router.push('/rooms')
}

function scrollToBottom() {
  nextTick(() => { if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight })
}

// ====== 工具 ======

function statusText(status) {
  const map = { WAITING: '等待中', PLAYING: '播放中', PAUSED: '已暂停' }
  return map[status] || status || '等待中'
}

function formatTime(ms) {
  if (!ms && ms !== 0) return '0:00'
  const totalSeconds = Math.floor(ms / 1000)
  return `${Math.floor(totalSeconds / 60)}:${(totalSeconds % 60).toString().padStart(2, '0')}`
}

function formatMsgTime(timestamp) {
  if (!timestamp) return ''
  const d = new Date(timestamp)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}
</script>

<style scoped>
.room-detail {
  height: calc(100vh - 52px);  /* NavBar 高度 + 0 */
  display: flex;
  flex-direction: column;
  background: #121212;
  color: #fff;
}

.loading, .error-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  gap: 16px;
  color: #999;
}

/* 顶部栏 */
.room-header {
  padding: 12px 20px;
  background: #1a1a1a;
  border-bottom: 1px solid #333;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.btn-back {
  background: transparent;
  border: 1px solid #555;
  color: #ccc;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.btn-back:hover { background: #333; color: #fff; }

.room-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.room-title h1 { margin: 0; font-size: 18px; }

.room-status-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.room-status-badge.playing { background: #1db954; color: #fff; }
.room-status-badge.paused { background: #f59e0b; color: #fff; }
.room-status-badge.waiting { background: #6b7280; color: #fff; }

.room-info-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #999;
}

.btn-action-owner {
  background: #E53935;
  border: none;
  color: #fff;
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

.btn-action-owner:hover { background: #c62828; }

/* 主体布局 */
.room-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.left-panel {
  width: 320px;
  border-right: 1px solid #333;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 当前播放 */
.now-playing {
  padding: 16px;
  border-bottom: 1px solid #333;
}

.now-playing h3 { margin: 0 0 10px; font-size: 14px; }
.song-title { font-size: 16px; font-weight: 600; color: #1db954; margin: 0 0 4px; }
.song-artist { font-size: 13px; color: #999; margin: 0 0 6px; }
.song-progress { font-size: 11px; color: #666; }
.no-song { color: #666; text-align: center; padding: 16px; font-size: 13px; }
.no-song .hint { font-size: 11px; margin-top: 4px; }

/* 播放队列 */
.queue-panel {
  padding: 16px;
  border-bottom: 1px solid #333;
  max-height: 160px;
  overflow-y: auto;
}

.queue-panel h3 { margin: 0 0 10px; font-size: 14px; }
.queue-empty { color: #666; font-size: 12px; text-align: center; padding: 8px; }

.queue-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  font-size: 13px;
  border-radius: 6px;
}

.queue-item:hover { background: #1e1e1e; }

.q-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.q-remove { background: none; border: none; color: #E53935; cursor: pointer; font-size: 12px; padding: 2px 4px; }

/* 成员列表 */
.members-panel {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.members-panel h3 { margin: 0 0 10px; font-size: 14px; }

.member-list { display: flex; flex-direction: column; gap: 6px; }

.member-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  background: #1e1e1e;
  font-size: 13px;
}

.member-avatar {
  width: 30px; height: 30px;
  border-radius: 50%;
  background: #333;
  object-fit: cover;
}

.member-name { flex: 1; }
.owner-badge { background: #f59e0b; color: #000; padding: 1px 6px; border-radius: 6px; font-size: 10px; font-weight: 600; }

.btn-kick, .btn-transfer {
  background: none; border: none;
  cursor: pointer; font-size: 14px;
  padding: 2px 4px; opacity: 0.5;
}

.btn-kick:hover, .btn-transfer:hover { opacity: 1; }
.btn-kick:hover { color: #E53935; }
.btn-transfer:hover { color: #f59e0b; }

/* 聊天面板 */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-panel h3 {
  padding: 16px 20px 10px;
  margin: 0;
  font-size: 14px;
  border-bottom: 1px solid #333;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.msg { font-size: 13px; line-height: 1.5; }
.msg-system { text-align: center; color: #666; font-size: 11px; padding: 4px 0; }
.system-text { font-style: italic; }
.msg strong { color: #1db954; margin-right: 4px; }
.msg-time { color: #555; font-size: 10px; margin-left: 6px; }

.chat-input {
  display: flex;
  padding: 10px 16px;
  gap: 8px;
  border-top: 1px solid #333;
}

.chat-input input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #444;
  border-radius: 20px;
  background: #2a2a2a;
  color: #fff;
  font-size: 13px;
  outline: none;
}

.chat-input input:focus { border-color: #1db954; }

.chat-input button {
  background: #1db954;
  color: #fff;
  border: none;
  padding: 8px 18px;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
}

.chat-input button:disabled { background: #444; color: #666; cursor: not-allowed; }

/* 弹窗 */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.6);
  display: flex; align-items: center; justify-content: center; z-index: 2000;
}

.modal-card {
  background: #1e1e1e;
  border: 1px solid #444;
  border-radius: 12px;
  padding: 24px;
  width: 380px;
  color: #fff;
}

.modal-card h3 { margin: 0 0 12px; font-size: 18px; }
.modal-card p { color: #ccc; font-size: 14px; margin-bottom: 16px; }

.modal-actions { display: flex; gap: 8px; justify-content: flex-end; }
.btn-cancel { padding: 8px 20px; background: #333; border: 1px solid #555; color: #ccc; border-radius: 6px; font-size: 13px; cursor: pointer; }
.btn-danger { padding: 8px 20px; background: #E53935; color: #fff; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; }
.btn-danger:disabled { background: #666; cursor: not-allowed; }
</style>
