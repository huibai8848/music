<template>
  <!-- 底部固定播放条（仅在有歌曲时显示） -->
  <div v-if="store.currentSong" class="player-bar">
    <div class="player-inner">

      <!-- 左侧：歌曲信息 -->
      <div class="player-left" @click="expandPanel = !expandPanel">
        <div class="cover-wrap">
          <img :src="store.currentSong.coverUrl || defaultCover"
               @error="onCoverError"
               alt="" class="cover-img"
               :class="{ spinning: store.isPlaying }">
        </div>
        <div class="song-info">
          <div class="song-title">{{ store.currentSong.title }}</div>
          <div class="song-artist">{{ store.currentSong.artistName || '未知艺人' }}</div>
        </div>
      </div>

      <!-- 中间：控制按钮 + 进度条 -->
      <div class="player-center">
        <!-- 控制按钮 -->
        <div class="controls">
          <button class="ctrl-btn" @click="store.cycleMode()" :title="store.modeLabel">
            <span v-if="store.mode === 'SEQUENCE'" class="mode-icon">🔂</span>
            <span v-else-if="store.mode === 'LOOP'" class="mode-icon loop">🔁</span>
            <span v-else-if="store.mode === 'REPEAT_ONE'" class="mode-icon repeat-one">🔂</span>
            <span v-else class="mode-icon shuffle">🔀</span>
          </button>

          <button class="ctrl-btn" @click="store.prev()" title="上一首">⏮</button>

          <button class="ctrl-btn play-btn" @click="store.togglePlay()" :title="store.isPlaying ? '暂停' : '播放'">
            <span class="play-icon">{{ store.isPlaying ? '⏸' : '▶️' }}</span>
          </button>

          <button class="ctrl-btn" @click="store.next()" title="下一首">⏭</button>

          <button class="ctrl-btn" :class="{ active: store.showLyrics }"
                  @click="store.showLyrics = !store.showLyrics" title="歌词">📃</button>
        </div>

        <!-- 进度条 -->
        <div class="progress-wrap">
          <span class="time-current">{{ formatTime(store.currentTime) }}</span>
          <div class="progress-bar" ref="progressRef"
               @mousedown="onProgressMouseDown"
               @click="onProgressClick">
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: store.progressPercent + '%' }"></div>
              <div class="progress-thumb" :style="{ left: store.progressPercent + '%' }"></div>
            </div>
          </div>
          <span class="time-total">{{ formatTime(store.duration) }}</span>
        </div>
      </div>

      <!-- 右侧：音量 + 队列 -->
      <div class="player-right">
        <div class="volume-wrap">
          <button class="ctrl-btn" @click="store.toggleMute()" :title="store.isMuted ? '取消静音' : '静音'">
            {{ store.isMuted || store.volume === 0 ? '🔇' : store.volume < 0.5 ? '🔉' : '🔊' }}
          </button>
          <div class="volume-slider" @click="onVolumeClick">
            <div class="volume-track">
              <div class="volume-fill" :style="{ width: (store.isMuted ? 0 : store.volume * 100) + '%' }"></div>
            </div>
          </div>
        </div>

        <button class="ctrl-btn queue-btn" :class="{ active: store.showQueue }"
                @click="store.showQueue = !store.showQueue" title="播放队列">
          📋 <span class="queue-badge" v-if="store.queue.length > 0">{{ store.queue.length }}</span>
        </button>
      </div>
    </div>

    <!-- 展开面板（歌词 / 全屏） -->
    <Transition name="expand">
      <div v-if="expandPanel" class="expand-panel">
        <div class="expand-layout">
          <div class="expand-disc">
            <DiscAnimation
              :cover-url="store.currentSong?.coverUrl || ''"
              :is-playing="store.isPlaying"
            />
          </div>
          <div class="expand-lyrics">
            <LyricsPanel
              :song-id="store.currentSong?.id"
              :current-time="store.currentTime"
              @seek="onLyricsSeek"
            />
          </div>
        </div>
      </div>
    </Transition>
  </div>

  <!-- 队列抽屉 -->
  <PlayQueue :visible="store.showQueue" @close="store.showQueue = false" />
</template>

<script setup>
/**
 * PlayerBar.vue — 全局底部播放条
 *
 * 功能：
 * 1. 展示当前播放歌曲信息（封面、标题、艺人）
 * 2. 播放/暂停、上一首、下一首控制
 * 3. 可拖拽/点击的进度条
 * 4. 音量控制（滑块 + 静音切换）
 * 5. 播放模式切换（顺序/循环/单曲/随机）
 * 6. 每 10 秒自动上报播放进度
 * 7. 展开歌词面板
 * 8. 打开队列抽屉
 *
 * 组件结构：
 * PlayerBar.vue（主控）
 * ├── LyricsPanel.vue（歌词面板，展开时显示）
 * └── PlayQueue.vue（队列抽屉，点击时弹出）
 */
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { usePlaybackStore } from '../stores/playback'
import LyricsPanel from './LyricsPanel.vue'
import PlayQueue from './PlayQueue.vue'
import DiscAnimation from './DiscAnimation.vue'

const store = usePlaybackStore()

/** 默认封面（SVG 占位图） */
const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60">' +
  '<rect fill="#e0e0e0" width="60" height="60"/>' +
  '<text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>'
)

/** 封面加载失败时使用默认 */
function onCoverError(e) {
  e.target.src = defaultCover
}

/** 是否展开面板（歌词/全屏） */
const expandPanel = ref(false)

// ==================== 进度条拖拽 ====================

const progressRef = ref(null)
let isDragging = false

function onProgressClick(e) {
  if (isDragging) return
  const rect = progressRef.value.getBoundingClientRect()
  const ratio = (e.clientX - rect.left) / rect.width
  store.seekTo(ratio * store.duration)
}

function onProgressMouseDown(e) {
  isDragging = true
  const rect = progressRef.value.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  store.seekTo(ratio * store.duration)

  const onMove = (ev) => {
    const r = progressRef.value.getBoundingClientRect()
    const ra = Math.max(0, Math.min(1, (ev.clientX - r.left) / r.width))
    store.seekTo(ra * store.duration)
  }

  const onUp = () => {
    isDragging = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }

  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

// ==================== 音量控制 ====================

function onVolumeClick(e) {
  const el = e.currentTarget
  const rect = el.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  store.setVolume(ratio)
}

// ==================== 歌词跳转 ====================

function onLyricsSeek(timeMs) {
  store.seekTo(timeMs)
}

// ==================== 进度定时上报 ====================

/** 每 10 秒上报一次播放进度到后端 */
let progressTimer = null

onMounted(() => {
  // 每 10 秒上报进度
  progressTimer = setInterval(() => {
    store.reportProgress()
  }, 10000)
})

onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer)
})

// 队列变化时持久化到 localStorage
watch(() => store.queue.length, () => {
  store.saveState()
})

watch(() => store.mode, () => {
  store.saveState()
})

// ==================== 工具 ====================

/**
 * 格式化毫秒为 mm:ss
 */
function formatTime(ms) {
  if (!ms || ms < 0) return '00:00'
  const totalSec = Math.floor(ms / 1000)
  const min = Math.floor(totalSec / 60)
  const sec = totalSec % 60
  return `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
}
</script>

<style scoped>
/* 底部播放条容器 */
.player-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  box-shadow: 0 -2px 12px rgba(0,0,0,0.08);
  z-index: 999;
  /* 给进度拖拽预留空间 */
  padding-top: 4px;
}

.player-inner {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 20px;
  max-width: 1400px;
  margin: 0 auto;
  gap: 16px;
}

/* ===== 左侧：歌曲信息 ===== */
.player-left {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 240px;
  flex-shrink: 0;
  cursor: pointer;
}

.cover-wrap {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.cover-img.spinning {
  animation: spin 4s linear infinite;
}

.song-info {
  min-width: 0;
}

.song-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-artist {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 中间：控制 + 进度 ===== */
.player-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

/* 控制按钮 */
.controls {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ctrl-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
  line-height: 1;
  position: relative;
}

.ctrl-btn:hover { background: #f0f0f0; }
.ctrl-btn.active { color: #1565C0; background: #E3F2FD; }

.play-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1565C0;
  border-radius: 50%;
  color: #fff;
  font-size: 16px;
}

.play-btn:hover { background: #0D47A1; }

.play-icon { line-height: 1; }

/* 进度条 */
.progress-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  max-width: 600px;
}

.time-current, .time-total {
  font-size: 11px;
  color: #999;
  min-width: 36px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.progress-bar {
  flex: 1;
  height: 20px;
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
}

.progress-track {
  width: 100%;
  height: 4px;
  background: #e8e8e8;
  border-radius: 2px;
  position: relative;
  transition: height 0.15s;
}

.progress-bar:hover .progress-track {
  height: 6px;
}

.progress-fill {
  height: 100%;
  background: #1565C0;
  border-radius: 2px;
  transition: width 0.1s linear;
}

.progress-thumb {
  position: absolute;
  top: 50%;
  width: 12px;
  height: 12px;
  background: #1565C0;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

.progress-bar:hover .progress-thumb {
  opacity: 1;
}

/* ===== 右侧：音量 + 队列 ===== */
.player-right {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 200px;
  flex-shrink: 0;
  justify-content: flex-end;
}

.volume-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.volume-slider {
  width: 80px;
  height: 20px;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.volume-track {
  width: 100%;
  height: 4px;
  background: #e8e8e8;
  border-radius: 2px;
  position: relative;
}

.volume-fill {
  height: 100%;
  background: #666;
  border-radius: 2px;
}

.queue-btn .queue-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background: #1565C0;
  color: #fff;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

/* ===== 展开面板 ===== */
.expand-panel {
  height: 300px;
  background: #fafafa;
  border-top: 1px solid #eee;
  overflow-y: auto;
  padding: 20px;
}
.expand-layout {
  display: flex; gap: 24px; align-items: flex-start; max-width: 900px; margin: 0 auto;
}
.expand-disc { flex-shrink: 0; width: 180px; }
.expand-lyrics { flex: 1; min-width: 0; }

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  height: 0;
  padding-top: 0;
  padding-bottom: 0;
  opacity: 0;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 手机适配：小于 768px 隐藏封面缩略图，缩小按钮 */
@media (max-width: 768px) {
  .player-left .cover-wrap {
    display: none;
  }
  .player-left {
    width: auto;
    flex-shrink: 1;
  }
  .ctrl-btn {
    font-size: 14px;
    padding: 2px 4px;
  }
  .play-btn {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }
  .player-right {
    width: auto;
  }
  .volume-wrap {
    display: none;
  }
  .progress-wrap {
    max-width: none;
  }
  .time-current, .time-total {
    min-width: 28px;
    font-size: 10px;
  }
}
</style>
