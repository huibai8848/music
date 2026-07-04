<template>
  <div class="list-page">
    <header class="page-header">
      <h1>🎵 歌曲浏览</h1>
      <div class="header-actions">
        <router-link to="/search" class="search-link">🔍 搜索</router-link>
        <router-link to="/" class="back-link">← 首页</router-link>
      </div>
    </header>

    <main class="main">
      <!-- 艺人 / 专辑筛选行 -->
      <div class="quick-filter-row">
        <div class="filter-select-group">
          <label class="filter-label">艺人</label>
          <select v-model="selectedArtistId" class="filter-select" @change="onFilterChange">
            <option value="">全部艺人</option>
            <option v-for="a in artists" :key="a.id" :value="a.id">{{ a.name }}</option>
          </select>
        </div>
        <div class="filter-select-group">
          <label class="filter-label">专辑</label>
          <select v-model="selectedAlbumId" class="filter-select" @change="onFilterChange">
            <option value="">全部专辑</option>
            <option v-for="al in albums" :key="al.id" :value="al.id">{{ al.title }}</option>
          </select>
        </div>
        <button v-if="hasActiveFilter" class="btn-clear" @click="clearFilters">✕ 清除筛选</button>
      </div>

      <!-- 分类选项卡 -->
      <div class="cat-tabs">
        <button v-for="tab in categoryTabs" :key="tab.key"
                :class="['cat-tab', { active: activeCatTab === tab.key }]"
                @click="switchCatTab(tab.key)">
          {{ tab.label }}
        </button>
      </div>

      <!-- 分类筛选按钮 -->
      <div v-if="activeCatTab && currentCategories.length > 0" class="filter-bar">
        <button v-for="cat in currentCategories" :key="cat.id"
                :class="['filter-btn', { active: activeCategoryName === cat.name }]"
                @click="toggleCategory(cat.name)">
          {{ cat.name }}
        </button>
        <button v-if="activeCategoryName" class="filter-btn filter-btn-clear" @click="clearCategoryFilter">取消筛选</button>
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
          <span class="row-language">{{ song.language || '-' }}</span>
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
 * 歌曲浏览页
 *
 * 支持多维度筛选：
 * - 风格（GENRE）/ 语种（LANGUAGE）分类选项卡
 * - 艺人下拉选择
 * - 专辑下拉选择
 * - 分页浏览，双击歌曲播放
 */
import { ref, computed, onMounted } from 'vue'
import { usePlaybackStore } from '../stores/playback'
import request from '../utils/request'

const playbackStore = usePlaybackStore()

const songs = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const totalPages = ref(1)

// 分类相关
const activeCatTab = ref('') // '' = 全部, 'GENRE', 'LANGUAGE'
const activeCategoryName = ref('')
const allCategories = ref([]) // 从后端加载的所有分类

// 艺人 / 专辑筛选
const selectedArtistId = ref('')
const selectedAlbumId = ref('')
const artists = ref([])
const albums = ref([])

const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>')

const categoryTabs = [
  { key: '', label: '全部' },
  { key: 'GENRE', label: '风格' },
  { key: 'LANGUAGE', label: '语种' }
]

/** 当前选中的分类选项卡下的分类列表 */
const currentCategories = computed(() => {
  if (!activeCatTab.value) return []
  return allCategories.value.filter(c => c.type === activeCatTab.value)
})

/** 是否有任何筛选条件生效 */
const hasActiveFilter = computed(() => {
  return activeCategoryName.value ||
         selectedArtistId.value ||
         selectedAlbumId.value
})

function onCoverError(e) { e.target.src = defaultCover }

/** 从后端加载所有分类 */
async function loadCategories() {
  try {
    const res = await request.get('/categories')
    if (res.code === 200 && res.data) {
      // 返回的是按类型分组的 Map: { GENRE: [...], LANGUAGE: [...] }
      const grouped = res.data
      const flat = []
      if (grouped.GENRE) flat.push(...grouped.GENRE)
      if (grouped.LANGUAGE) flat.push(...grouped.LANGUAGE)
      if (grouped.YEAR) flat.push(...grouped.YEAR)
      allCategories.value = flat
    }
  } catch { /* ignore */ }
}

/** 加载艺人列表（用于下拉框） */
async function loadArtists() {
  try {
    const res = await request.get('/artists', { params: { page: 1, size: 200 } })
    if (res.code === 200) {
      artists.value = res.data?.records || []
    }
  } catch { /* ignore */ }
}

/** 加载专辑列表（用于下拉框） */
async function loadAlbums() {
  try {
    const res = await request.get('/albums', { params: { page: 1, size: 200 } })
    if (res.code === 200) {
      albums.value = res.data?.records || []
    }
  } catch { /* ignore */ }
}

/** 切换分类选项卡 */
function switchCatTab(key) {
  activeCatTab.value = key
  activeCategoryName.value = ''
  page.value = 1
  loadSongs()
}

/** 切换分类筛选按钮 */
function toggleCategory(name) {
  if (activeCategoryName.value === name) {
    activeCategoryName.value = ''
  } else {
    activeCategoryName.value = name
  }
  page.value = 1
  loadSongs()
}

/** 清除当前分类筛选 */
function clearCategoryFilter() {
  activeCategoryName.value = ''
  page.value = 1
  loadSongs()
}

/** 清除所有筛选 */
function clearFilters() {
  activeCategoryName.value = ''
  selectedArtistId.value = ''
  selectedAlbumId.value = ''
  page.value = 1
  loadSongs()
}

/** 筛选条件变更（艺人/专辑下拉） */
function onFilterChange() {
  page.value = 1
  loadSongs()
}

async function loadSongs() {
  try {
    const params = { page: page.value, size: size.value }

    // 分类筛选：根据当前选项卡类型决定使用的参数字段
    if (activeCatTab.value === 'GENRE' && activeCategoryName.value) {
      params.genre = activeCategoryName.value
    } else if (activeCatTab.value === 'LANGUAGE' && activeCategoryName.value) {
      params.language = activeCategoryName.value
    }

    // 艺人 / 专辑筛选
    if (selectedArtistId.value) {
      params.artistId = selectedArtistId.value
    }
    if (selectedAlbumId.value) {
      params.albumId = selectedAlbumId.value
    }

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
  loadCategories()
  loadArtists()
  loadAlbums()
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

.main { max-width: 960px; margin: 0 auto; padding: 20px; }

/* 快速筛选行 */
.quick-filter-row {
  display: flex; gap: 12px; align-items: flex-end; flex-wrap: wrap;
  margin-bottom: 16px; background: #fff; padding: 12px 16px;
  border-radius: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.filter-select-group { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 12px; color: #999; }
.filter-select {
  padding: 6px 12px; border: 1px solid #ddd; border-radius: 6px;
  font-size: 13px; background: #fff; min-width: 140px;
  cursor: pointer; outline: none;
}
.filter-select:focus { border-color: #1565C0; }
.btn-clear {
  padding: 6px 14px; background: #fff; color: #E53935;
  border: 1px solid #E53935; border-radius: 6px;
  font-size: 13px; cursor: pointer; white-space: nowrap;
}
.btn-clear:hover { background: #FFEBEE; }

/* 分类选项卡 */
.cat-tabs {
  display: flex; gap: 4px; margin-bottom: 12px;
}
.cat-tab {
  padding: 8px 20px; background: #fff;
  border: 1px solid #ddd; border-radius: 8px 8px 0 0;
  font-size: 14px; cursor: pointer; color: #666;
  transition: all 0.2s;
}
.cat-tab.active {
  background: #1565C0; color: #fff; border-color: #1565C0;
}
.cat-tab:hover:not(.active) { border-color: #1565C0; color: #1565C0; }

/* 分类筛选按钮 */
.filter-bar {
  display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px;
}
.filter-btn {
  padding: 6px 16px; border: 1px solid #ddd; border-radius: 20px;
  background: #fff; font-size: 13px; cursor: pointer;
  transition: all 0.2s;
}
.filter-btn.active { background: #1565C0; color: #fff; border-color: #1565C0; }
.filter-btn:hover:not(.active) { border-color: #1565C0; color: #1565C0; }
.filter-btn-clear { color: #999; font-size: 12px; border-color: #ccc; }

/* 歌曲表格 */
.song-table { background: #fff; border-radius: 10px; overflow: hidden; }
.song-row {
  display: flex; align-items: center; gap: 12px; padding: 10px 16px;
  border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: background 0.15s;
}
.song-row:hover { background: #f8f9fa; }
.row-index { width: 28px; font-size: 12px; color: #999; text-align: center; flex-shrink: 0; }
.row-cover img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; flex-shrink: 0; }
.row-info { flex: 1; min-width: 0; }
.row-title { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
.row-title:hover { color: #1565C0; }
.row-artist { font-size: 12px; color: #999; }
.row-genre { font-size: 12px; color: #999; min-width: 60px; text-align: center; flex-shrink: 0; }
.row-language { font-size: 12px; color: #999; min-width: 60px; text-align: center; flex-shrink: 0; }
.row-duration { font-size: 12px; color: #999; width: 50px; text-align: right; flex-shrink: 0; }
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
  background: #fff; font-size: 13px; cursor: pointer;
}
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #666; }
.empty-hint { text-align: center; color: #ccc; padding: 60px 20px; font-size: 16px; }
</style>
