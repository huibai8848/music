<template>
  <div class="search-page">
    <header class="page-header">
      <h1>🔍 搜索</h1>
      <div class="search-box">
        <input
          v-model="keyword"
          @keyup.enter="doSearch"
          placeholder="搜索歌曲、艺人、专辑..."
          class="search-input"
          ref="searchInput"
        />
        <button @click="doSearch" class="search-btn" :disabled="loading">搜索</button>
      </div>
      <router-link to="/" class="back-link">← 首页</router-link>
    </header>

    <main class="main">
      <!-- 未搜索时的提示 -->
      <div v-if="!searched" class="search-hint">
        输入关键词搜索歌曲、艺人或专辑
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-text">搜索中...</div>

      <!-- 搜索错误提示 -->
      <div v-if="searchError" class="error-banner">
        ⚠️ 搜索出错了，请稍后重试
        <button class="retry-btn" @click="doSearch">重试</button>
      </div>

      <!-- 无结果 -->
      <div v-if="searched && !loading && totalAll === 0 && !searchError" class="empty-state">
        <div class="empty-icon">🔍</div>
        <p>未找到与「<strong>{{ keyword }}</strong>」相关的内容</p>
        <p class="empty-hint">试试其他关键词</p>
      </div>

      <!-- 搜索结果 — 标签页 -->
      <div v-if="searched && totalAll > 0 && !searchError" class="result-section">
        <!-- 标签页导航 -->
        <div class="tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            :class="['tab-btn', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
            <span class="tab-count">{{ tab.count }}</span>
          </button>
        </div>

        <!-- ===== 歌曲 Tab ===== -->
        <div v-show="activeTab === 'songs'" class="tab-content">
          <div v-if="songs.length === 0" class="tab-empty">未找到相关歌曲</div>
          <div v-else class="song-table">
            <div
              v-for="(song, idx) in songs"
              :key="song.id"
              class="song-row"
              @dblclick="playSong(song)"
            >
              <span class="row-idx">{{ songStart + idx + 1 }}</span>
              <div class="row-cover">
                <img :src="song.coverUrl || defaultCover" @error="onCoverError" alt="" />
              </div>
              <div class="row-info">
                <div class="row-title" @click="$router.push('/songs/' + song.id)">
                  <span v-html="highlight(song.title)"></span>
                </div>
                <div class="row-artist">
                  <span v-if="song.artistName" v-html="highlight(song.artistName)"></span>
                  <span v-else>未知</span>
                </div>
              </div>
              <span class="row-duration">{{ formatDuration(song.duration) }}</span>
              <button class="play-btn" @click.stop="playSong(song)" title="播放">▶</button>
            </div>
          </div>
          <!-- 歌曲分页 -->
          <div v-if="songTotal > songSize" class="pagination">
            <button :disabled="songPage <= 1" @click="songPage--; loadSongs()">上一页</button>
            <span>{{ songPage }} / {{ Math.ceil(songTotal / songSize) }}</span>
            <button :disabled="songPage * songSize >= songTotal" @click="songPage++; loadSongs()">下一页</button>
          </div>
        </div>

        <!-- ===== 艺人 Tab ===== -->
        <div v-show="activeTab === 'artists'" class="tab-content">
          <div v-if="artists.length === 0" class="tab-empty">未找到相关艺人</div>
          <div v-else class="artist-grid">
            <div
              v-for="a in artists"
              :key="a.id"
              class="artist-card"
              @click="$router.push('/artists/' + a.id)"
            >
              <img :src="a.avatar || defaultAvatar" @error="onAvatarError" alt="" />
              <div class="artist-info">
                <div class="artist-name" v-html="highlight(a.name)"></div>
                <div class="artist-count">{{ a.songCount || 0 }} 首歌</div>
              </div>
            </div>
          </div>
          <!-- 艺人分页 -->
          <div v-if="artistTotal > artistSize" class="pagination">
            <button :disabled="artistPage <= 1" @click="artistPage--; loadArtists()">上一页</button>
            <span>{{ artistPage }} / {{ Math.ceil(artistTotal / artistSize) }}</span>
            <button :disabled="artistPage * artistSize >= artistTotal" @click="artistPage++; loadArtists()">下一页</button>
          </div>
        </div>

        <!-- ===== 专辑 Tab ===== -->
        <div v-show="activeTab === 'albums'" class="tab-content">
          <div v-if="albums.length === 0" class="tab-empty">未找到相关专辑</div>
          <div v-else class="album-grid">
            <div
              v-for="al in albums"
              :key="al.id"
              class="album-card"
              @click="$router.push('/albums/' + al.id)"
            >
              <img :src="al.coverUrl || defaultCover" @error="onCoverError" alt="" />
              <div class="album-info">
                <div class="album-title" v-html="highlight(al.title)"></div>
                <div class="album-artist">
                  <span v-if="al.artistName" v-html="highlight(al.artistName)"></span>
                  <span v-else>未知</span>
                </div>
              </div>
            </div>
          </div>
          <!-- 专辑分页 -->
          <div v-if="albumTotal > albumSize" class="pagination">
            <button :disabled="albumPage <= 1" @click="albumPage--; loadAlbums()">上一页</button>
            <span>{{ albumPage }} / {{ Math.ceil(albumTotal / albumSize) }}</span>
            <button :disabled="albumPage * albumSize >= albumTotal" @click="albumPage++; loadAlbums()">下一页</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
/**
 * 搜索页面 — 多类型聚合搜索
 *
 * 同时搜索歌曲、艺人、专辑三类内容，以标签页切换展示。
 * 每类结果独立维护分页状态，支持关键词高亮。
 */
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { usePlaybackStore } from '../stores/playback'
import request from '../utils/request'

const route = useRoute()
const playback = usePlaybackStore()

// ===== 搜索状态 =====
const keyword = ref('')
const searched = ref(false)
const loading = ref(false)
const searchError = ref(false)
const activeTab = ref('songs')
const searchInput = ref(null)

// ===== 默认占位图 =====
const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>')
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎤</text></svg>')

function onCoverError(e) { e.target.src = defaultCover }
function onAvatarError(e) { e.target.src = defaultAvatar }

// ===== 歌曲搜索状态 =====
const songs = ref([])
const songTotal = ref(0)
const songPage = ref(1)
const songSize = 20

// ===== 艺人搜索状态 =====
const artists = ref([])
const artistTotal = ref(0)
const artistPage = ref(1)
const artistSize = 20

// ===== 专辑搜索状态 =====
const albums = ref([])
const albumTotal = ref(0)
const albumPage = ref(1)
const albumSize = 20

// ===== 计算属性 =====

/** 歌曲结果起始序号 */
const songStart = computed(() => (songPage.value - 1) * songSize)

/** 标签页配置，带各类型命中数 */
const tabs = computed(() => [
  { key: 'songs', label: '歌曲', count: songTotal.value },
  { key: 'artists', label: '艺人', count: artistTotal.value },
  { key: 'albums', label: '专辑', count: albumTotal.value },
])

/** 三种类型结果总数之和 */
const totalAll = computed(() => songTotal.value + artistTotal.value + albumTotal.value)

// ===== 搜索方法 =====

/**
 * 关键词高亮：将匹配关键词的部分用 <mark> 包裹
 */
function highlight(text) {
  if (!text || !keyword.value) return text
  const kw = keyword.value.trim()
  if (!kw) return text
  // 转义正则特殊字符
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp('(' + escaped + ')', 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

/**
 * 格式化时长（秒 → m:ss）
 */
function formatDuration(sec) {
  if (!sec && sec !== 0) return '--:--'
  return `${Math.floor(sec / 60)}:${(sec % 60).toString().padStart(2, '0')}`
}

/**
 * 点击播放歌曲
 */
function playSong(song) {
  playback.replaceQueue(songs.value, songs.value.indexOf(song))
}

// ===== API 请求 =====

/**
 * 同时搜索歌曲、艺人、专辑
 * 三个请求并行发出，任一失败不影响其他类型的结果
 */
async function doSearch() {
  const q = keyword.value.trim()
  if (!q) return

  searched.value = true
  loading.value = true
  searchError.value = false

  // 重置所有分页到第一页
  songPage.value = 1
  artistPage.value = 1
  albumPage.value = 1
  activeTab.value = 'songs'

  // 并行发起三种类型的搜索
  await Promise.all([
    loadSongs(),
    loadArtists(),
    loadAlbums(),
  ])

  loading.value = false
}

/** 搜索歌曲 */
async function loadSongs() {
  try {
    const res = await request.get('/songs/search', {
      params: { q: keyword.value.trim(), page: songPage.value, size: songSize },
    })
    if (res.code === 200) {
      songs.value = res.data.records || []
      songTotal.value = res.data.total || 0
    }
  } catch (e) {
    console.warn('歌曲搜索失败', e)
    songs.value = []
    songTotal.value = 0
    searchError.value = true
  }
}

/** 搜索艺人 */
async function loadArtists() {
  try {
    const res = await request.get('/artists/search', {
      params: { q: keyword.value.trim(), page: artistPage.value, size: artistSize },
    })
    if (res.code === 200) {
      artists.value = res.data.records || []
      artistTotal.value = res.data.total || 0
    }
  } catch (e) {
    console.warn('艺人搜索失败', e)
    artists.value = []
    artistTotal.value = 0
    searchError.value = true
  }
}

/** 搜索专辑 */
async function loadAlbums() {
  try {
    const res = await request.get('/albums/search', {
      params: { q: keyword.value.trim(), page: albumPage.value, size: albumSize },
    })
    if (res.code === 200) {
      albums.value = res.data.records || []
      albumTotal.value = res.data.total || 0
    }
  } catch (e) {
    console.warn('专辑搜索失败', e)
    albums.value = []
    albumTotal.value = 0
    searchError.value = true
  }
}

// ===== 初始化 =====
onMounted(async () => {
  // 支持 URL query 参数 ?q=xxx，从导航栏搜索跳转过来时自动填充并搜索
  if (route.query.q) {
    keyword.value = route.query.q
    await nextTick()
    doSearch()
  }
  // 自动聚焦搜索框
  if (searchInput.value) {
    searchInput.value.focus()
  }
})
</script>

<style scoped>
/* ===== 页面布局 ===== */
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
  cursor: pointer; transition: background 0.2s;
}
.search-btn:hover { background: rgba(255,255,255,0.3); }
.search-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.back-link { color: rgba(255,255,255,0.9); font-size: 14px; white-space: nowrap; }

.main { max-width: 800px; margin: 0 auto; padding: 24px 20px; }

/* ===== 状态提示 ===== */
.search-hint { text-align: center; color: #ccc; padding: 80px 20px; font-size: 16px; }
.loading-text { text-align: center; color: #999; padding: 40px; font-size: 14px; }
.error-banner {
  background: #FFEBEE; color: #C62828; padding: 12px 20px;
  border-radius: 8px; text-align: center; font-size: 14px;
  display: flex; align-items: center; justify-content: center; gap: 12px;
}
.retry-btn {
  padding: 4px 12px; background: #C62828; color: #fff;
  border: none; border-radius: 4px; font-size: 12px; cursor: pointer;
}
.empty-state { text-align: center; padding: 60px 20px; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-state p { color: #666; font-size: 14px; margin: 4px 0; }
.empty-hint { color: #ccc !important; font-size: 13px !important; }

/* ===== 标签页 ===== */
.result-section { background: #fff; border-radius: 12px; overflow: hidden; }
.tabs {
  display: flex; border-bottom: 2px solid #f0f0f0;
  position: sticky; top: 0; background: #fff; z-index: 10;
}
.tab-btn {
  flex: 1; padding: 14px 16px; background: none; border: none;
  font-size: 14px; cursor: pointer; color: #999;
  position: relative; transition: color 0.2s;
}
.tab-btn.active { color: #1565C0; font-weight: 600; }
.tab-btn.active::after {
  content: ''; position: absolute; bottom: -2px; left: 20%; right: 20%;
  height: 2px; background: #1565C0; border-radius: 1px;
}
.tab-count {
  margin-left: 6px; font-size: 12px; color: #999;
  background: #f5f5f5; padding: 1px 8px; border-radius: 10px;
}
.tab-btn.active .tab-count { background: #E3F2FD; color: #1565C0; }

/* ===== 标签页内容区域 ===== */
.tab-content { padding: 16px 0; }
.tab-empty { text-align: center; color: #ccc; padding: 40px; font-size: 14px; }

/* ===== 歌曲列表 ===== */
.song-row {
  display: flex; align-items: center; gap: 12px; padding: 10px 20px;
  border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: background 0.15s;
}
.song-row:hover { background: #f8f9fa; }
.row-idx { width: 24px; font-size: 12px; color: #999; text-align: center; flex-shrink: 0; }
.row-cover img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; flex-shrink: 0; }
.row-info { flex: 1; min-width: 0; }
.row-title { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
.row-title:hover { color: #1565C0; }
.row-artist { font-size: 12px; color: #999; }
.row-duration { font-size: 12px; color: #999; width: 50px; text-align: right; flex-shrink: 0; }
.play-btn {
  background: none; border: none; font-size: 16px; cursor: pointer;
  padding: 4px 8px; border-radius: 4px; opacity: 0; transition: opacity 0.2s;
}
.song-row:hover .play-btn { opacity: 1; }
.play-btn:hover { background: #f0f0f0; }

/* ===== 艺人网格 ===== */
.artist-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px; padding: 0 12px;
}
.artist-card {
  background: #fafafa; border-radius: 10px; overflow: hidden;
  cursor: pointer; text-align: center; transition: transform 0.2s, box-shadow 0.2s;
}
.artist-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.artist-card img { width: 100%; aspect-ratio: 1; object-fit: cover; }
.artist-info { padding: 10px; }
.artist-name { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.artist-count { font-size: 12px; color: #999; margin-top: 4px; }

/* ===== 专辑网格 ===== */
.album-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px; padding: 0 12px;
}
.album-card {
  background: #fafafa; border-radius: 10px; overflow: hidden;
  cursor: pointer; transition: transform 0.2s, box-shadow 0.2s;
}
.album-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.album-card img { width: 100%; aspect-ratio: 1; object-fit: cover; }
.album-info { padding: 10px; }
.album-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.album-artist { font-size: 12px; color: #999; margin-top: 4px; }

/* ===== 分页 ===== */
.pagination {
  display: flex; justify-content: center; gap: 16px;
  margin-top: 20px; padding: 12px;
}
.pagination button {
  padding: 4px 16px; border: 1px solid #ddd; border-radius: 6px;
  background: #fff; font-size: 13px; cursor: pointer;
}
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #666; }

/* ===== 高亮标记 ===== */
:deep(mark) { background: #FFE082; padding: 0 2px; border-radius: 2px; }
</style>
