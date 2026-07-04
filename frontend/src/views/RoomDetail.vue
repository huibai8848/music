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
        <!-- 左侧：当前播放 + 队列 + 音乐源 + 成员 -->
        <div class="left-panel">
          <!-- 当前播放 -->
          <div class="now-playing">
            <h3>🎵 正在播放</h3>
            <div class="song-info" v-if="currentPlayingSong">
              <img :src="currentPlayingSong.coverUrl" class="now-cover"
                   @error="e => e.target.style.display='none'" />
              <div class="now-detail">
                <p class="song-title">{{ currentPlayingSong.title }}</p>
                <p class="song-artist">{{ currentPlayingSong.artistName || '未知艺人' }}</p>
                <p class="song-progress">{{ formatTime(room.progress) }}</p>
              </div>
              <!-- 房主播放控制 -->
              <div class="play-controls" v-if="isOwner && currentPlayingSong">
                <button class="ctrl-btn" @click="togglePlay" :title="room.isPlaying ? '暂停' : '播放'">
                  {{ room.isPlaying ? '⏸' : '▶️' }}
                </button>
                <button class="ctrl-btn" @click="playNext" title="下一首">⏭</button>
              </div>
            </div>
            <div class="no-song" v-else>
              <p>暂无歌曲</p>
              <p class="hint">房主可以从歌单/专辑/收藏中添加歌曲</p>
            </div>
          </div>

          <!-- 播放队列 -->
          <div class="queue-panel">
            <h3>📋 播放队列 ({{ queue.length }})</h3>
            <div v-if="queue.length === 0" class="queue-empty">
              <p>队列为空</p>
              <p class="hint" v-if="isOwner">点击下方「添加歌曲」将音乐加入队列</p>
            </div>
            <div v-for="(item, idx) in queue" :key="idx" class="queue-item"
                 :class="{ 'queue-active': item.songId === room.currentSongId }">
              <img :src="item.coverUrl" class="q-cover" @error="e => e.target.style.display='none'" />
              <div class="q-info">
                <span class="q-title">{{ item.songTitle || '歌曲 #' + item.songId }}</span>
                <span class="q-artist" v-if="item.songArtist">{{ item.songArtist }}</span>
              </div>
              <!-- 房主操作 -->
              <template v-if="isOwner">
                <button class="q-play" @click="playFromQueue(item)" :title="item.songId === room.currentSongId && room.isPlaying ? '正在播放' : '播放'">
                  {{ item.songId === room.currentSongId && room.isPlaying ? '🔊' : '▶️' }}
                </button>
                <button class="q-remove" @click="removeFromQueue(item)" title="移除">✕</button>
              </template>
            </div>
          </div>

          <!-- 添加歌曲（仅房主可见） -->
          <div class="add-music" v-if="isOwner">
            <h3>🎶 添加歌曲</h3>
            <div class="source-tabs">
              <button :class="{ active: sourceTab === 'playlist' }" @click="sourceTab = 'playlist'; fetchSourceData()">歌单</button>
              <button :class="{ active: sourceTab === 'album' }" @click="sourceTab = 'album'; fetchSourceData()">专辑</button>
              <button :class="{ active: sourceTab === 'favorite' }" @click="sourceTab = 'favorite'; fetchSourceData()">收藏</button>
            </div>
            <div class="source-loading" v-if="sourceLoading">加载中...</div>
            <div class="source-list" v-else-if="sourceItems.length > 0">
              <div class="source-item" v-for="item in sourceItems" :key="item.id || item.targetId">
                <span class="source-name">{{ item.name || item.title || item.targetName }}</span>
                <button class="btn-add-queue" @click="loadSourceToQueue(item)">+ 队列</button>
              </div>
            </div>
            <div class="source-empty" v-else>
              <p v-if="sourceTab === 'playlist'">暂无歌单，先去创建一些吧</p>
              <p v-else-if="sourceTab === 'album'">暂无专辑</p>
              <p v-else>暂无收藏歌曲</p>
            </div>
            <div class="source-actions" v-if="sourceItems.length > 0">
              <button class="btn-add-all" @click="loadAllToQueue">全部添加到队列</button>
            </div>
          </div>

          <!-- 成员列表 -->
          <div class="members-panel">
            <h3>👥 在线成员 ({{ memberCount }})</h3>
            <div class="member-list">
              <div class="member-item" v-for="m in members" :key="m.userId">
                <img :src="m.avatar || ''" class="member-avatar" @error="onAvatarError" />
                <span class="member-name">{{ m.nickname }}</span>
                <span class="vip-badge" v-if="m.isVip">VIP</span>
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

    <!-- 房间密码弹窗 -->
    <div v-if="showPasswordDialog" class="modal-overlay" @click.self="cancelPassword">
      <div class="modal-card">
        <h3>🔒 此房间需要密码</h3>
        <p>请输入房间密码后加入</p>
        <div class="form-group">
          <input v-model="joinPassword" type="password" placeholder="输入房间密码"
                 maxlength="20" @keyup.enter="confirmJoinPassword"
                 class="password-input" />
        </div>
        <p class="error" v-if="joinPasswordError">{{ joinPasswordError }}</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="cancelPassword">取消</button>
          <button class="btn btn-primary" @click="confirmJoinPassword" :disabled="joiningRoom">
            {{ joiningRoom ? '验证中...' : '确认加入' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const roomId = computed(() => route.params.id?.toString() || '')
const room = ref(null)
const members = ref([])
const messages = ref([])
const queue = ref([])
const loading = ref(true)
const error = ref('')
const chatInput = ref('')
const ws = ref(null)
const heartbeatTimer = ref(null)
const pollTimer = ref(null)
const chatRef = ref(null)
const memberCount = ref(0)
const showDismissConfirm = ref(false)
const dismissing = ref(false)
const currentPlayingSong = ref(null)

// 密码房相关
const showPasswordDialog = ref(false)
const joinPassword = ref('')
const joinPasswordError = ref('')
const joiningRoom = ref(false)

// 音乐源相关
const sourceTab = ref('playlist')
const sourceItems = ref([])
const sourceLoading = ref(false)

// 房间音频实例（独立于全局播放器）
let roomAudio = null

const isOwner = computed(() => room.value && authStore.user?.id === room.value.ownerId)

function onAvatarError(e) { e.target.src = '' }

onMounted(async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }

  // 刚创建的房间：房主已在房间中，无需再调用 join
  const isNewlyCreated = route.query.created === '1'
  if (isNewlyCreated) {
    // 清除 query 参数，防止刷新后跳过 join
    router.replace({ query: {} })
  } else {
    // 先加入房间（私密房间会触发密码弹窗）
    const joined = await handleJoinRoom()
    if (!joined) return // 加入失败或取消，已跳转
  }

  await fetchRoomDetail()
  await fetchMembers()
  await fetchMessages()
  await fetchQueue()
  connectWebSocket()
  // 如果是房主，加载音乐源数据
  if (isOwner.value) {
    await fetchSourceData()
  } else {
    // 非房主加入房间后，如果房间正在播放歌曲，同步播放音频
    syncPlaybackAfterJoin()
  }
  // 每 5 秒轮询成员列表和消息
  startPolling()
})

onUnmounted(() => {
  disconnectWebSocket()
  destroyRoomAudio()
  stopPolling()
})

// ====== 音频管理 ======

function initRoomAudio() {
  if (roomAudio) return
  roomAudio = new Audio()
  roomAudio.preload = 'auto'

  roomAudio.addEventListener('ended', () => {
    // 歌曲播放结束，房主自动播放下一个
    if (isOwner.value) {
      playNext()
    }
  })

  roomAudio.addEventListener('timeupdate', () => {
    if (room.value && isOwner.value) {
      const progress = Math.floor(roomAudio.currentTime * 1000)
      room.value.progress = progress
      // 每 10 秒同步一次进度
      if (Math.floor(progress / 10000) > Math.floor((progress - 1000) / 10000)) {
        sendSync({ progress, isPlaying: true })
      }
    }
  })

  roomAudio.addEventListener('error', () => {
    console.warn('房间音频加载失败')
    if (isOwner.value) {
      setTimeout(playNext, 2000)
    }
  })
}

function destroyRoomAudio() {
  if (roomAudio) {
    roomAudio.pause()
    roomAudio.src = ''
    roomAudio = null
  }
}

function playAudio(song, startProgress = 0) {
  initRoomAudio()
  if (!song || !song.audioUrl) return
  roomAudio.src = song.audioUrl
  roomAudio.currentTime = startProgress / 1000
  roomAudio.play().then(() => {
    if (room.value) room.value.isPlaying = true
  }).catch(e => {
    console.warn('房间播放失败', e)
  })
}

function pauseAudio() {
  if (roomAudio) {
    roomAudio.pause()
    if (room.value) {
      room.value.isPlaying = false
      room.value.progress = Math.floor(roomAudio.currentTime * 1000)
    }
  }
}

function resumeAudio() {
  if (roomAudio && roomAudio.src) {
    roomAudio.play().then(() => {
      if (room.value) room.value.isPlaying = true
    }).catch(e => {
      console.warn('恢复播放失败', e)
    })
  }
}

function seekAudio(progressMs) {
  if (roomAudio && roomAudio.src) {
    roomAudio.currentTime = progressMs / 1000
  }
}

// ====== API 请求 ======

async function fetchRoomDetail() {
  try {
    const res = await request.get(`/rooms/${roomId.value}`)
    if (res.code === 200 && res.data) {
      const newData = res.data
      // 记录更新前的歌曲 ID，用于检测切换
      const oldSongId = room.value?.currentSongId
      const newSongId = newData.currentSongId

      room.value = newData
      memberCount.value = newData.memberCount || 0
      // 更新当前播放歌曲信息
      updateCurrentSongInfo()

      // 非房主成员：每次轮询都检查播放状态，做同步
      if (!isOwner.value) {
        if (newSongId && newData.isPlaying) {
          if (newSongId !== oldSongId) {
            // 歌曲切换了 → 获取新歌曲并播放
            console.log(`检测到歌曲切换: ${oldSongId} → ${newSongId}`)
            playSyncedSong(newSongId, newData.progress || 0)
          } else if (roomAudio) {
            // 同一首歌
            // 如果本地已暂停 → 恢复播放
            if (roomAudio.paused) {
              console.log('检测到房主已恢复播放，本地同步恢复')
              if (newData.progress > 0) seekAudio(newData.progress)
              resumeAudio()
            }
            // 检查进度偏差（超过 10 秒则同步到服务器进度）
            const localMs = Math.floor(roomAudio.currentTime * 1000)
            const serverMs = newData.progress || 0
            if (serverMs > 0 && Math.abs(localMs - serverMs) > 10000) {
              console.log(`进度偏差 ${Math.abs(localMs - serverMs)}ms > 10s，同步到 ${serverMs}ms`)
              seekAudio(serverMs)
            }
          } else {
            // roomAudio 不存在（重连后丢失或从未初始化）→ 从服务器获取播放
            console.log('roomAudio 不存在，从服务器同步播放')
            playSyncedSong(newSongId, newData.progress || 0)
          }
        } else if (!newData.isPlaying && roomAudio && !roomAudio.paused) {
          // 服务器已暂停，但本地还在播放 → 暂停
          console.log('检测到房主已暂停，本地同步暂停')
          pauseAudio()
        }
      }
    } else {
      // 房间不存在，跳转回列表
      handleRoomGone()
    }
  } catch (e) {
    // 如果请求失败（网络或房间不存在），跳转回列表
    handleRoomGone()
  } finally {
    loading.value = false
  }
}

/** 房间已解散时，清理并返回列表 */
function handleRoomGone() {
  disconnectWebSocket()
  destroyRoomAudio()
  stopPolling()
  router.push('/rooms')
}

/**
 * 加入房间（私密房间需密码）
 * 返回 true 表示加入成功，false 表示取消/失败
 */
async function handleJoinRoom() {
  joiningRoom.value = true
  try {
    // 先尝试无密码加入
    await request.post(`/rooms/${roomId.value}/join`, {})
    joiningRoom.value = false
    return true
  } catch (e) {
    // 已在房间中（房主创建后无需重新加入），视为成功
    if (e.message && e.message.includes('已在房间中')) {
      joiningRoom.value = false
      return true
    }
    // 判断是否为密码错误
    if (e.message && e.message.includes('密码')) {
      // 显示密码对话框，等待用户输入
      joiningRoom.value = false
      showPasswordDialog.value = true
      joinPasswordError.value = ''
      // 通过 Promise + watch 等待用户确认密码
      return new Promise((resolve) => {
        const unwatch = watch(showPasswordDialog, async (val) => {
          if (!val) {
            // 对话框被关闭（用户取消）
            unwatch()
            handleRoomGone()
            resolve(false)
          }
        })
        // 将 resolve 挂到 window 临时变量（由模板中的确认按钮调用）
        window.__roomJoinResolve = resolve
      })
    }
    // 其他错误（房间不存在、已满员等）
    joiningRoom.value = false
    handleRoomGone()
    return false
  }
}

/** 确认密码并加入房间 */
async function confirmJoinPassword() {
  if (!joinPassword.value.trim()) {
    joinPasswordError.value = '请输入房间密码'
    return
  }
  joinPasswordError.value = ''
  joiningRoom.value = true
  try {
    await request.post(`/rooms/${roomId.value}/join`, {
      password: joinPassword.value
    })
    // 加入成功，关闭对话框
    showPasswordDialog.value = false
    joiningRoom.value = false
    if (window.__roomJoinResolve) {
      window.__roomJoinResolve(true)
      window.__roomJoinResolve = null
    }
  } catch (e) {
    joiningRoom.value = false
    joinPasswordError.value = e.message || '密码错误，请重试'
  }
}

/** 取消加入房间 */
function cancelPassword() {
  showPasswordDialog.value = false
  if (window.__roomJoinResolve) {
    window.__roomJoinResolve(false)
    window.__roomJoinResolve = null
  }
  router.push('/rooms')
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

async function fetchSourceData() {
  sourceLoading.value = true
  sourceItems.value = []
  try {
    if (sourceTab.value === 'playlist') {
      const res = await request.get('/playlists/mine')
      if (res.code === 200 && res.data) {
        sourceItems.value = res.data.map(p => ({ ...p, id: p.id, name: p.title }))
      }
    } else if (sourceTab.value === 'album') {
      const res = await request.get('/albums?page=1&size=50')
      if (res.code === 200 && res.data?.records) {
        sourceItems.value = res.data.records.map(a => ({ ...a, name: a.title }))
      }
    } else if (sourceTab.value === 'favorite') {
      const res = await request.get('/favorites?page=1&size=50')
      if (res.code === 200 && res.data?.records) {
        sourceItems.value = (res.data.records || []).filter(f => f.targetType === 'SONG')
      }
    }
  } catch (e) {
    console.error('获取音乐源失败', e)
  } finally {
    sourceLoading.value = false
  }
}

async function loadSourceToQueue(item) {
  try {
    if (sourceTab.value === 'playlist') {
      await request.post(`/rooms/${roomId.value}/queue/playlist/${item.id}`)
    } else if (sourceTab.value === 'album') {
      await request.post(`/rooms/${roomId.value}/queue/album/${item.id}`)
    } else if (sourceTab.value === 'favorite') {
      // 单个收藏歌曲，添加到队列
      await request.post(`/rooms/${roomId.value}/queue`, { songId: item.targetId })
    }
    await fetchQueue()
  } catch (e) {
    console.error('加载到队列失败', e)
  }
}

async function loadAllToQueue() {
  try {
    if (sourceTab.value === 'playlist') {
      for (const item of sourceItems.value) {
        await request.post(`/rooms/${roomId.value}/queue/playlist/${item.id}`)
      }
    } else if (sourceTab.value === 'album') {
      for (const item of sourceItems.value) {
        await request.post(`/rooms/${roomId.value}/queue/album/${item.id}`)
      }
    } else if (sourceTab.value === 'favorite') {
      await request.post(`/rooms/${roomId.value}/queue/favorites`)
    }
    await fetchQueue()
  } catch (e) {
    console.error('批量加载失败', e)
  }
}

// ====== 播放控制（仅房主） ======

async function playFromQueue(item) {
  if (!isOwner.value) return
  // 如果是同一首歌，切换播放/暂停
  if (item.songId === room.value?.currentSongId) {
    togglePlay()
    return
  }

  // 获取歌曲完整信息
  let song = null
  try {
    const res = await request.get(`/songs/${item.songId}`)
    if (res.code === 200 && res.data) {
      song = res.data
    }
  } catch (e) {
    console.error('获取歌曲信息失败', e)
  }
  if (!song) return

  // 开始播放
  currentPlayingSong.value = song
  playAudio(song)

  // 更新房间状态
  if (room.value) {
    room.value.currentSongId = item.songId
    room.value.currentSongTitle = song.title
    room.value.currentSongArtist = song.artistName
    room.value.currentSongCover = song.coverUrl
    room.value.isPlaying = true
    room.value.progress = 0
    room.value.status = 'PLAYING'
  }

  // 发送同步消息
  sendSync({
    songId: item.songId,
    isPlaying: true,
    progress: 0,
    action: 'PLAY'
  })

  await fetchQueue()
}

async function togglePlay() {
  if (!isOwner.value || !room.value) return

  if (room.value.isPlaying) {
    pauseAudio()
    sendSync({ isPlaying: false, progress: room.value.progress || 0 })
  } else if (currentPlayingSong.value) {
    resumeAudio()
    sendSync({ isPlaying: true, progress: room.value.progress || 0 })
  }
}

async function playNext() {
  if (!isOwner.value) return
  const currentIdx = queue.value.findIndex(q => q.songId === room.value?.currentSongId)
  let nextIdx = currentIdx + 1

  // 超出队列长度，从头开始（列表循环）
  if (nextIdx >= queue.value.length) {
    nextIdx = 0
  }

  if (queue.value.length > 0) {
    await playFromQueue(queue.value[nextIdx])
  }
}

function updateCurrentSongInfo() {
  if (!room.value || !room.value.currentSongId) {
    // 轮询时 room.currentSongId 可能为 null（数据到达前），
    // 不覆盖已有的 currentPlayingSong，避免"正在播放"闪烁消失
    return
  }
  // 从队列中查找
  const qItem = queue.value.find(q => q.songId === room.value.currentSongId)
  if (qItem && qItem.songTitle) {
    currentPlayingSong.value = {
      id: qItem.songId,
      title: qItem.songTitle,
      artistName: qItem.songArtist,
      coverUrl: qItem.coverUrl,
      audioUrl: qItem.audioUrl,
      duration: qItem.duration
    }
  } else if (room.value.currentSongTitle) {
    currentPlayingSong.value = {
      id: room.value.currentSongId,
      title: room.value.currentSongTitle,
      artistName: room.value.currentSongArtist,
      coverUrl: room.value.currentSongCover
    }
  }
}

function sendSync(payload) {
  if (!ws.value || ws.value.readyState !== WebSocket.OPEN) return
  ws.value.send(JSON.stringify({
    type: 'SYNC',
    payload: { ...payload, roomId: roomId.value }
  }))
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
    ws.value.onopen = () => {
      console.log('WebSocket 已连接')
      startHeartbeat()
      // 重连后刷新房间状态
      fetchMembers()
      fetchMessages()
      fetchQueue()
    }
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
      if (payload.action === 'DISMISS') {
        // 房间已解散，立即返回列表
        handleRoomGone()
        return
      }
      if (payload.action === 'JOIN' || payload.action === 'LEAVE') fetchMembers()
      if (payload.content) {
        messages.value.push({ id: Date.now(), type: 'SYSTEM', content: payload.content, timestamp: msg.timestamp })
        scrollToBottom()
      }
      break
    case 'SYNC':
      handleSyncMessage(msg)
      break
  }
}

/** 加入房间后，如果房间正在播放则同步播放音频 */
async function syncPlaybackAfterJoin() {
  if (!room.value?.currentSongId || !room.value?.isPlaying) return
  // 等 WebSocket 连接就绪后再开始播放
  const waitForWs = () => {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      playSyncedSong(room.value.currentSongId, room.value.progress || 0)
    } else {
      setTimeout(waitForWs, 500)
    }
  }
  setTimeout(waitForWs, 1000)
}

function handleSyncMessage(msg) {
  const sync = msg.payload || {}
  if (!room.value) return

  const oldSongId = room.value.currentSongId
  const newSongId = sync.songId || oldSongId

  // 更新房间状态
  if (sync.songId) room.value.currentSongId = sync.songId
  if (sync.isPlaying !== undefined) room.value.isPlaying = sync.isPlaying
  if (sync.progress !== undefined) room.value.progress = sync.progress

  // 歌曲切换了
  if (sync.songId && sync.songId !== oldSongId) {
    // 非房主成员：加载新歌曲并播放
    if (!isOwner.value) {
      playSyncedSong(sync.songId, sync.progress || 0)
    }
    fetchRoomDetail()
    fetchQueue()
  } else if (sync.isPlaying !== undefined && !isOwner.value) {
    // 非房主成员：播放/暂停控制
    if (sync.isPlaying) {
      // 房主恢复播放
      if (roomAudio && roomAudio.src) {
        // 已有音频实例 → 恢复播放（可能带进度跳转）
        if (sync.progress) seekAudio(sync.progress)
        resumeAudio()
      } else {
        // roomAudio 不存在（重连后丢失等）→ 重新加载并播放
        const songId = sync.songId || room.value?.currentSongId
        if (songId) playSyncedSong(songId, sync.progress || 0)
      }
    } else {
      pauseAudio()
    }
  }
}

async function playSyncedSong(songId, startProgress) {
  try {
    const res = await request.get(`/songs/${songId}`)
    if (res.code === 200 && res.data) {
      const song = res.data
      currentPlayingSong.value = song
      playAudio(song, startProgress)
    }
  } catch (e) {
    console.error('获取同步歌曲信息失败', e)
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

// ====== 5 秒轮询 ======

function startPolling() {
  stopPolling()
  pollTimer.value = setInterval(() => {
    fetchRoomDetail()
    fetchMembers()
    fetchMessages()
  }, 5000)
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

// ====== 聊天 ======

function sendChat() {
  const content = chatInput.value.trim()
  if (!content || !ws.value || ws.value.readyState !== WebSocket.OPEN) return
  ws.value.send(JSON.stringify({ type: 'CHAT', payload: { content } }))
  // 本地立即添加消息，让发送者也能看到（后端广播排除了发送者）
  messages.value.push({
    id: Date.now(),
    type: 'TEXT',
    userId: authStore.user?.id,
    nickname: authStore.user?.nickname || '我',
    content: content,
    createdAt: Date.now()
  })
  chatInput.value = ''
  scrollToBottom()
}

async function handleLeave() {
  destroyRoomAudio()
  try { await request.post(`/rooms/${roomId.value}/leave`) }
  catch (e) { console.warn('退出房间API调用失败', e) }
  disconnectWebSocket()
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
  height: calc(100vh - 52px);
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
  width: 360px;
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

.song-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.now-cover {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  background: #333;
}

.now-detail {
  flex: 1;
  min-width: 0;
}

.song-title { font-size: 15px; font-weight: 600; color: #1db954; margin: 0 0 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.song-artist { font-size: 12px; color: #999; margin: 0 0 2px; }
.song-progress { font-size: 11px; color: #666; }

.play-controls {
  display: flex;
  gap: 4px;
}

.ctrl-btn {
  background: #2a2a2a;
  border: 1px solid #444;
  color: #fff;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.ctrl-btn:hover { background: #1db954; border-color: #1db954; }

.no-song { color: #666; text-align: center; padding: 16px; font-size: 13px; }
.no-song .hint { font-size: 11px; margin-top: 4px; }

/* 播放队列 */
.queue-panel {
  padding: 12px 16px;
  border-bottom: 1px solid #333;
  max-height: 200px;
  overflow-y: auto;
}

.queue-panel h3 { margin: 0 0 10px; font-size: 14px; }

.queue-empty { color: #666; font-size: 12px; text-align: center; padding: 8px; }
.queue-empty .hint { font-size: 11px; margin-top: 4px; color: #555; }

.queue-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  font-size: 13px;
  border-radius: 6px;
  transition: background 0.15s;
}

.queue-item:hover { background: #1e1e1e; }
.queue-active { background: #1a3a1a; border-left: 3px solid #1db954; }

.q-cover {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  object-fit: cover;
  background: #333;
}

.q-info {
  flex: 1;
  min-width: 0;
}

.q-title { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.q-artist { display: block; font-size: 11px; color: #777; }

.q-play, .q-remove {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  padding: 2px 4px;
  opacity: 0.6;
  transition: opacity 0.15s;
}

.q-play:hover, .q-remove:hover { opacity: 1; }
.q-play { color: #1db954; }
.q-remove { color: #E53935; }

/* 添加音乐（房主） */
.add-music {
  padding: 12px 16px;
  border-bottom: 1px solid #333;
}

.add-music h3 { margin: 0 0 10px; font-size: 14px; }

.source-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}

.source-tabs button {
  flex: 1;
  padding: 6px 8px;
  border: 1px solid #444;
  border-radius: 6px;
  background: #2a2a2a;
  color: #ccc;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.source-tabs button.active {
  background: #1db954;
  border-color: #1db954;
  color: #fff;
}

.source-tabs button:hover:not(.active) { background: #333; }

.source-loading, .source-empty {
  font-size: 12px;
  color: #666;
  text-align: center;
  padding: 12px;
}

.source-list {
  max-height: 140px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.source-item:hover { background: #1e1e1e; }

.source-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #ccc;
}

.btn-add-queue {
  background: transparent;
  border: 1px solid #444;
  color: #1db954;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-add-queue:hover { background: #1db954; color: #fff; }

.source-actions {
  display: flex;
  gap: 6px;
}

.btn-add-all {
  flex: 1;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: #1db954;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.btn-add-all:hover { background: #169c46; }

/* 成员列表 */
.members-panel {
  padding: 12px 16px;
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

.vip-badge {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #000;
  padding: 1px 6px;
  border-radius: 6px;
  font-size: 9px;
  font-weight: 700;
}

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

.password-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #444;
  border-radius: 8px;
  background: #2a2a2a;
  color: #fff;
  font-size: 14px;
  box-sizing: border-box;
  outline: none;
}

.password-input:focus {
  border-color: #1db954;
}

.error {
  color: #ef4444;
  font-size: 13px;
  margin-top: 8px;
  text-align: center;
}
</style>
