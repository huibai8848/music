<template>
  <div class="list-page">
    <header class="page-header">
      <h1>🎵 歌曲列表</h1>
      <div class="header-actions">
        <router-link to="/search" class="search-link">🔍 搜索</router-link>
        <router-link to="/" class="back-link">← 首页</router-link>
      </div>
    </header>

    <main class="main">
      <!-- 风格筛选 -->
      <div class="filter-bar">
        <button v-for="g in genres" :key="g"
                :class="['filter-btn', { active: currentGenre === g }]"
                @click="currentGenre = g; loadSongs()">
          {{ g }}
        </button>
      </div>

      <!-- 歌曲列表 -->
      <div v-if="songs.length === 0" class="empty-hint">暂无歌曲</div>
      <div v-else class="song-table">
        <div v-for="(song, idx) in songs" :key="song.id" class="song-row"
             @dblclick="playSong(song)">
          <span class="row-index">{{ (page - 1) * size + idx + 1 }}</span>
          <div class="row-cover">
            <img :src="song.coverUrl || defaultCover" @error="onCoverError" alt="">
          </div>
          <div class="row-info">
            <div class="row-title" @click="$router.push('/songs/' + song.id)">{{ song.title }}</div>
            <div class="row-artist">{{ song.artistName || '未知' }}</div>
          </div>
          <span class="row-genre">{{ song.genre || '-' }}</span>
          <span class="row-duration">{{ formatDuration(song.duration) }}</span>
          <div class="row-actions">
            <button class="play-btn" @click.stop="playSong(song)" title="播放">▶</button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="page <= 1" @click="page--; loadSongs()">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button :disabled="page >= totalPages" @click="page++; loadSongs()">下一页</button>
      </div>
    </main>
  </div>
</template>

<script setup>
/**
 * 歌曲列表页
 *
 * 支持按风格筛选、分页浏览，双击歌曲播放。
 */
import { ref, onMounted } from 'vue'
import { usePlaybackStore } from '../stores/playback'
import request from '../utils/request'

const playbackStore = usePlaybackStore()

const songs = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const currentGenre = ref('全部')

/**
 * 风格列表，初始为硬编码默认值，页面加载后从后端 API 获取
 * GET /api/categories?type=GENRE → 动态更新
 */
const genres = ref(['全部'])
const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>')

const totalPages = ref(1)

function onCoverError(e) { e.target.src = defaultCover }

/** 从后端加载分类列表 */
async function loadGenres() {
  try {
    const res = await request.get('/categories', { params: { type: 'GENRE' } })
    if (res.code === 200 && Array.isArray(res.data)) {
      const list = res.data.map(c => c.name || c)
      genres.value = ['全部', ...list]
    }
  } catch { /* 保留默认值 */ }
}

async function loadSongs() {
  try {
    const params = { page: page.value, size: size.value }
    if (currentGenre.value !== '全部') params.genre = currentGenre.value
    const res = await request.get('/songs', { params })
    if (res.code === 200) {
      songs.value = res.data.records || []
      total.value = res.data.total || 0
      totalPages.value = Math.ceil(total.value / size.value) || 1
    }
  } catch { songs.value = [] }
}

function playSong(song) {
  playbackStore.replaceQueue(songs.value, songs.value.indexOf(song))
}

function formatDuration(sec) {
  if (!sec) return '--:--'
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

onMounted(() => {
  loadGenres()
  loadSongs()
})
</script>

<style scoped>
.list-page { min-height: 100vh; background: #f5f5f5; }
.page-header {
  background: #1565C0; color: #fff; padding: 16px 32px;
  display: flex; justify-content: space-between; align-items: center;
}
.page-header h1 { font-size: 20px; }
.header-actions { display: flex; gap: 16px; }
.header-actions a { color: rgba(255,255,255,0.9); font-size: 14px; }

.main { max-width: 900px; margin: 0 auto; padding: 20px; }

.filter-bar {
  display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px;
}
.filter-btn {
  padding: 6px 16px; border: 1px solid #ddd; border-radius: 20px;
  background: #fff; font-size: 13px; transition: all 0.2s;
}
.filter-btn.active { background: #1565C0; color: #fff; border-color: #1565C0; }
.filter-btn:hover:not(.active) { border-color: #1565C0; color: #1565C0; }

.song-table { background: #fff; border-radius: 10px; overflow: hidden; }
.song-row {
  display: flex; align-items: center; gap: 12px; padding: 10px 16px;
  border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: background 0.15s;
}
.song-row:hover { background: #f8f9fa; }
.row-index { width: 28px; font-size: 12px; color: #999; text-align: center; }
.row-cover img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; }
.row-info { flex: 1; min-width: 0; }
.row-title { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
.row-title:hover { color: #1565C0; }
.row-artist { font-size: 12px; color: #999; }
.row-genre { font-size: 12px; color: #999; width: 60px; }
.row-duration { font-size: 12px; color: #999; width: 50px; text-align: right; }
.play-btn {
  background: none; border: none; font-size: 18px; cursor: pointer;
  padding: 4px 8px; border-radius: 4px; opacity: 0;
}
.song-row:hover .play-btn { opacity: 1; }
.play-btn:hover { background: #f0f0f0; }

.pagination {
  display: flex; justify-content: center; align-items: center; gap: 16px;
  margin-top: 20px; padding: 16px;
}
.pagination button {
  padding: 6px 16px; border: 1px solid #ddd; border-radius: 6px;
  background: #fff; font-size: 13px;
}
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #666; }
.empty-hint { text-align: center; color: #ccc; padding: 60px 20px; font-size: 16px; }
</style>
