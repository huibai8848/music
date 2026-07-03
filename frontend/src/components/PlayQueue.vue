<template>
  <Transition name="slide">
    <div v-if="visible" class="queue-overlay" @click.self="$emit('close')">
      <div class="queue-drawer">
        <!-- 头部 -->
        <div class="queue-header">
          <h3>播放队列 <span class="count">({{ store.queue.length }}首)</span></h3>
          <div class="header-actions">
            <button class="btn-icon" @click="store.cycleMode()" :title="store.modeLabel">
              <span v-if="store.mode === 'SEQUENCE'">🔂</span>
              <span v-else-if="store.mode === 'LOOP'">🔁</span>
              <span v-else-if="store.mode === 'REPEAT_ONE'">🔂</span>
              <span v-else>🔀</span>
            </button>
            <button class="btn-icon" @click="store.clearQueue()" title="清空队列">🗑️</button>
            <button class="btn-icon" @click="$emit('close')" title="关闭">✕</button>
          </div>
        </div>

        <!-- 正在播放 -->
        <div v-if="store.currentSong" class="current-section">
          <div class="section-label">正在播放</div>
          <div class="current-item">
            <div class="item-cover">
              <img :src="store.currentSong.coverUrl || '/placeholder-cover.jpg'" alt=""
                   @error="$event.target.src = defaultCover">
            </div>
            <div class="item-info">
              <div class="item-title">{{ store.currentSong.title }}</div>
              <div class="item-artist">{{ store.currentSong.artistName || '未知艺人' }}</div>
            </div>
            <button class="btn-icon" @click="store.togglePlay()">
              {{ store.isPlaying ? '⏸' : '▶️' }}
            </button>
          </div>
        </div>

        <!-- 队列列表 -->
        <div class="queue-section">
          <div class="section-label">接下来</div>
          <div v-if="store.queue.length === 0" class="empty-tip">
            队列为空，去选择喜欢的歌曲吧
          </div>
          <div v-else class="queue-list">
            <div
              v-for="(song, idx) in store.queue"
              :key="idx"
              class="queue-item"
              :class="{ active: idx === store.queueIndex, current: idx === store.queueIndex }"
              @dblclick="store.play(song, idx)"
            >
              <span class="item-index">{{ idx + 1 }}</span>
              <div class="item-info">
                <div class="item-title">{{ song.title }}</div>
                <div class="item-artist">{{ song.artistName || '未知艺人' }}</div>
              </div>
              <button class="btn-icon remove-btn" @click="store.removeFromQueue(idx)" title="移除">✕</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/**
 * 播放队列抽屉组件
 *
 * 从右侧滑入，展示当前队列所有歌曲。
 * 支持双击播放、移除歌曲、切换模式、清空队列。
 */
import { usePlaybackStore } from '../stores/playback'

defineProps({
  visible: { type: Boolean, default: false }
})

defineEmits(['close'])

const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="%23ddd" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>')

const store = usePlaybackStore()
</script>

<style scoped>
/* 遮罩层 */
.queue-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.3);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

/* 抽屉 */
.queue-drawer {
  width: 360px;
  max-width: 90vw;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 20px rgba(0,0,0,0.15);
}

/* 头部 */
.queue-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}

.queue-header h3 { font-size: 16px; color: #333; }
.queue-header .count { font-size: 12px; color: #999; font-weight: normal; }

.header-actions { display: flex; gap: 8px; }

/* 当前播放 */
.current-section { padding: 12px 20px; border-bottom: 1px solid #f0f0f0; }

.current-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.current-item .item-cover img {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  object-fit: cover;
}

/* 队列列表 */
.queue-section { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.section-label { font-size: 12px; color: #999; padding: 12px 20px 8px; }

.queue-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px 80px;
}

/* 通用按钮 */
.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
  line-height: 1;
}

.btn-icon:hover { background: #f0f0f0; }

/* 队列项 */
.queue-item, .current-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  cursor: pointer;
  border-radius: 6px;
}

.queue-item:hover { background: #f8f9fa; }
.queue-item.active { background: #E3F2FD; }

.item-index { width: 24px; font-size: 12px; color: #999; text-align: center; }
.item-index.current { color: #1565C0; font-weight: bold; }

.item-info { flex: 1; min-width: 0; }
.item-title { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-artist { font-size: 12px; color: #999; }

.remove-btn { opacity: 0; font-size: 12px; }
.queue-item:hover .remove-btn { opacity: 1; }

.empty-tip { text-align: center; color: #ccc; padding: 40px 20px; font-size: 14px; }

/* 滑入动画 */
.slide-enter-active, .slide-leave-active { transition: transform 0.3s ease; }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }
</style>
