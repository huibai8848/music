<template>
  <div class="rankings-page">
    <header class="page-header">
      <h1>排行榜</h1>
      <div class="header-actions">
        <router-link to="/" class="back-link">← 首页</router-link>
      </div>
    </header>

    <main class="main">
      <!-- 榜单类型切换 -->
      <div class="tab-bar">
        <button v-for="tab in tabs" :key="tab.type"
                :class="['tab-btn', { active: currentType === tab.type }]"
                @click="switchTab(tab.type)">
          {{ tab.label }}
        </button>
      </div>

      <!-- 榜单说明 -->
      <div class="tab-desc">
        {{ currentLabel }} · 按播放量排序
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-hint">加载中...</div>

      <!-- 空状态 -->
      <div v-else-if="songs.length === 0" class="empty-hint">暂无数据</div>

      <!-- 歌曲列表 -->
      <div v-else class="song-table">
        <div v-for="(song, idx) in songs" :key="song.id" class="song-row"
             @dblclick="playSong(song)">
          <span class="row-rank" :class="{ 'rank-top': idx < 3 }">{{ idx + 1 }}</span>
          <div class="row-cover">
            <img :src="song.coverUrl || defaultCover" @error="onCoverError" alt="">
          </div>
          <div class="row-info">
            <div class="row-title" @click="$router.push('/songs/' + song.id)">{{ song.title }}</div>
            <div class="row-artist">{{ song.artistName || '未知' }}</div>
          </div>
          <span class="row-play-count">{{ formatPlayCount(song.playCount) }}</span>
          <div class="row-actions">
            <button class="play-btn" @click.stop="playSong(song)" title="播放">▶</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
/**
 * 排行榜页面
 *
 * 支持日榜/周榜/月榜/总榜切换，基于 Redis ZSet 实现。
 * 双击歌曲播放，点击歌名进入详情。
 */
import { ref, onMounted } from 'vue'
import { usePlaybackStore } from '../stores/playback'
import request from '../utils/request'

const playbackStore = usePlaybackStore()

const currentType = ref('daily')
const currentLabel = ref('日榜')
const songs = ref([])
const loading = ref(false)

const tabs = [
  { type: 'daily', label: '日榜' },
  { type: 'weekly', label: '周榜' },
  { type: 'monthly', label: '月榜' },
  { type: 'all', label: '总榜' }
]

const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>')

function onCoverError(e) { e.target.src = defaultCover }

async function loadRankings() {
  loading.value = true
  try {
    const res = await request.get('/rankings', {
      params: { type: currentType.value, limit: 50 }
    })
    if (res.code === 200) {
      songs.value = res.data.songs || []
      currentLabel.value = res.data.label || ''
    }
  } catch {
    songs.value = []
  } finally {
    loading.value = false
  }
}

function switchTab(type) {
  currentType.value = type
  loadRankings()
}

function playSong(song) {
  playbackStore.replaceQueue(songs.value, songs.value.indexOf(song))
}

function formatPlayCount(count) {
  if (!count && count !== 0) return ''
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return count.toString()
}

onMounted(loadRankings)
</script>

<style scoped>
.rankings-page { min-height: 100vh; background: #f5f5f5; }
.page-header {
  background: #1565C0; color: #fff; padding: 16px 32px;
  display: flex; justify-content: space-between; align-items: center;
}
.page-header h1 { font-size: 20px; }
.header-actions { display: flex; gap: 16px; }
.header-actions a { color: rgba(255,255,255,0.9); font-size: 14px; }

.main { max-width: 900px; margin: 0 auto; padding: 20px; }

.tab-bar {
  display: flex; gap: 8px; margin-bottom: 8px;
}
.tab-btn {
  padding: 8px 24px; border: 1px solid #ddd; border-radius: 20px;
  background: #fff; font-size: 14px; cursor: pointer;
  transition: all 0.2s;
}
.tab-btn.active {
  background: #1565C0; color: #fff; border-color: #1565C0;
}
.tab-btn:hover:not(.active) { border-color: #1565C0; color: #1565C0; }

.tab-desc { font-size: 12px; color: #999; margin-bottom: 12px; }

.song-table { background: #fff; border-radius: 10px; overflow: hidden; }
.song-row {
  display: flex; align-items: center; gap: 12px; padding: 10px 16px;
  border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: background 0.15s;
}
.song-row:hover { background: #f8f9fa; }
.row-rank {
  width: 28px; font-size: 14px; font-weight: 600; color: #999; text-align: center;
}
.row-rank.rank-top { color: #E53935; }
.row-cover img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; }
.row-info { flex: 1; min-width: 0; }
.row-title {
  font-size: 14px; color: #333;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer;
}
.row-title:hover { color: #1565C0; }
.row-artist { font-size: 12px; color: #999; }
.row-play-count { font-size: 12px; color: #999; width: 60px; text-align: right; }
.row-actions { width: 40px; text-align: center; }
.play-btn {
  background: none; border: none; font-size: 18px; cursor: pointer;
  padding: 4px 8px; border-radius: 4px; opacity: 0;
}
.song-row:hover .play-btn { opacity: 1; }
.play-btn:hover { background: #f0f0f0; }

.loading-hint, .empty-hint {
  text-align: center; color: #ccc; padding: 60px 20px; font-size: 16px;
}
</style>
