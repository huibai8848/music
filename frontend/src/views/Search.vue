<template>
  <div class="search-page">
    <header class="page-header">
      <h1>🔍 搜索</h1>
      <div class="search-box">
        <input v-model="keyword" @keyup.enter="doSearch" placeholder="搜索歌曲、艺人、专辑..." class="search-input" />
        <button @click="doSearch" class="search-btn">搜索</button>
      </div>
      <router-link to="/" class="back-link">← 首页</router-link>
    </header>

    <main class="main">
      <div v-if="keyword && searched">
        <!-- 搜索结果 -->
        <div class="result-section">
          <h3>歌曲结果 ({{ total }})</h3>
          <div v-if="songs.length === 0" class="empty-hint">未找到相关歌曲</div>
          <div v-else class="song-table">
            <div v-for="(song, idx) in songs" :key="song.id" class="song-row"
                 @dblclick="playSong(song, idx)">
              <span class="row-idx">{{ (page - 1) * size + idx + 1 }}</span>
              <div class="row-cover">
                <img :src="song.coverUrl || defaultCover" @error="onCoverError" alt="">
              </div>
              <div class="row-info">
                <div class="row-title" @click="$router.push('/songs/' + song.id)"><span v-html="highlight(song.title)"></span></div>
                <div class="row-artist">{{ song.artistName || '未知' }}</div>
              </div>
              <span class="row-duration">{{ formatDuration(song.duration) }}</span>
              <button class="play-btn" @click.stop="playSong(song, idx)">▶</button>
            </div>
          </div>
          <div v-if="total > size" class="pagination">
            <button :disabled="page <= 1" @click="page--; doSearch()">上一页</button>
            <span>{{ page }} / {{ Math.ceil(total / size) }}</span>
            <button :disabled="page * size >= total" @click="page++; doSearch()">下一页</button>
          </div>
        </div>
      </div>

      <div v-else-if="!searched" class="search-hint">
        输入关键词搜索歌曲、艺人或专辑
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { usePlaybackStore } from '../stores/playback'
import request from '../utils/request'

const playback = usePlaybackStore()

const keyword = ref('')
const songs = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const searched = ref(false)
const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>')

function onCoverError(e) { e.target.src = defaultCover }

async function doSearch() {
  if (!keyword.value.trim()) return
  page.value = 1
  searched.value = true
  await fetchResults()
}

async function fetchResults() {
  try {
    const res = await request.get('/songs/search', {
      params: { q: keyword.value, page: page.value, size: size.value }
    })
    if (res.code === 200) {
      songs.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch { songs.value = []; total.value = 0 }
}

function playSong(song, idx) {
  playback.replaceQueue(songs.value, idx)
}

function highlight(text) {
  if (!text || !keyword.value) return text
  const idx = text.toLowerCase().indexOf(keyword.value.toLowerCase())
  if (idx < 0) return text
  return text.slice(0, idx) + '<mark>' + text.slice(idx, idx + keyword.value.length) + '</mark>' + text.slice(idx + keyword.value.length)
}

function formatDuration(sec) {
  if (!sec) return '--:--'
  return `${Math.floor(sec / 60)}:${(sec % 60).toString().padStart(2, '0')}`
}
</script>

<style scoped>
.search-page { min-height: 100vh; background: #f5f5f5; }
.page-header {
  background: #1565C0; color: #fff; padding: 16px 32px;
  display: flex; align-items: center; gap: 16px;
}
.page-header h1 { font-size: 18px; white-space: nowrap; }
.search-box { flex: 1; display: flex; gap: 8px; max-width: 500px; }
.search-input {
  flex: 1; padding: 8px 14px; border: none; border-radius: 6px; font-size: 14px; outline: none;
}
.search-btn {
  padding: 8px 20px; background: rgba(255,255,255,0.2); color: #fff;
  border: 1px solid rgba(255,255,255,0.3); border-radius: 6px; font-size: 14px;
}
.search-btn:hover { background: rgba(255,255,255,0.3); }
.back-link { color: rgba(255,255,255,0.9); font-size: 14px; white-space: nowrap; }

.main { max-width: 800px; margin: 0 auto; padding: 24px 20px; }
.result-section { background: #fff; border-radius: 12px; padding: 24px; }
.result-section h3 { font-size: 16px; margin-bottom: 16px; }

.song-row {
  display: flex; align-items: center; gap: 12px; padding: 10px 0;
  border-bottom: 1px solid #f5f5f5; cursor: pointer;
}
.song-row:hover { background: #f8f9fa; }
.row-idx { width: 24px; font-size: 12px; color: #999; text-align: center; }
.row-cover img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; }
.row-info { flex: 1; min-width: 0; }
.row-title { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
.row-artist { font-size: 12px; color: #999; }
.row-duration { font-size: 12px; color: #999; width: 50px; text-align: right; }
.play-btn { background: none; border: none; font-size: 16px; cursor: pointer; opacity: 0; padding: 4px 8px; }
.song-row:hover .play-btn { opacity: 1; }

.pagination { display: flex; justify-content: center; gap: 16px; margin-top: 20px; padding: 12px; }
.pagination button { padding: 4px 16px; border: 1px solid #ddd; border-radius: 6px; background: #fff; font-size: 13px; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #666; }

.search-hint { text-align: center; color: #ccc; padding: 80px 20px; font-size: 16px; }
.empty-hint { text-align: center; color: #ccc; padding: 40px; font-size: 14px; }
mark { background: #FFE082; padding: 0 2px; }
</style>
