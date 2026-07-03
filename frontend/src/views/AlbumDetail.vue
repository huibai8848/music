<template>
  <div class="detail-page">
    <main v-if="album" class="main">
      <!-- 专辑头部 -->
      <div class="album-hero">
        <div class="hero-cover">
          <img :src="album.coverUrl || defaultCover" @error="onCoverError" alt="">
        </div>
        <div class="hero-info">
          <h2>{{ album.title }}</h2>
          <p class="hero-artist">{{ album.artistName || '未知艺人' }}</p>
          <p class="hero-meta">{{ album.songCount || 0 }} 首 · {{ album.releaseDate || '-' }}</p>
          <p class="hero-desc">{{ album.description || '暂无简介' }}</p>
          <div class="hero-actions">
            <button class="btn-secondary" :class="{ favorited: isFavorited }" @click="toggleFavorite">
              {{ isFavorited ? '❤️ 已收藏' : '🤍 收藏' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 歌曲列表 -->
      <div class="section">
        <h3>歌曲列表</h3>
        <div class="song-table">
          <div v-for="(song, idx) in album.songs" :key="song.id" class="song-row"
               @dblclick="playSong(song, idx)">
            <span class="row-idx">{{ idx + 1 }}</span>
            <div class="row-info">
              <div class="row-title" @click="$router.push('/songs/' + song.id)">{{ song.title }}</div>
            </div>
            <span class="row-duration">{{ formatDuration(song.duration) }}</span>
            <button class="play-btn" @click.stop="playSong(song, idx)">▶</button>
          </div>
          <div v-if="!album.songs?.length" class="empty-hint">暂无歌曲</div>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="section">
        <h3>评论</h3>
        <CommentSection :target-type="'ALBUM'" :target-id="album.id" />
      </div>
    </main>

    <div v-else-if="loading" class="loading">加载中...</div>
  </div>
</template>

<script setup>
/**
 * 专辑详情页
 *
 * 功能：
 * 1. 展示专辑信息 + 歌曲列表
 * 2. 播放歌曲 / 跳转歌曲详情
 * 3. 收藏专辑
 * 4. 评论区
 */
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { usePlaybackStore } from '../stores/playback'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'
import CommentSection from '../components/CommentSection.vue'

const route = useRoute()
const playback = usePlaybackStore()
const authStore = useAuthStore()

const album = ref(null)
const loading = ref(true)
const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>')

function onCoverError(e) { e.target.src = defaultCover }

// ====== 收藏 ======
const isFavorited = ref(false)

async function checkFavorite() {
  if (!authStore.isLoggedIn) return
  try {
    const res = await request.get('/favorites')
    if (res.code === 200) {
      const list = res.data?.records || res.data || []
      const arr = Array.isArray(res.data) ? res.data : list
      isFavorited.value = arr.some(f =>
        f.targetType === 'ALBUM' && Number(f.targetId) === Number(route.params.id))
    }
  } catch { /* ignore */ }
}

async function toggleFavorite() {
  if (!authStore.isLoggedIn) { alert('请先登录'); return }
  try {
    if (isFavorited.value) {
      await request.delete(`/favorites/album/${album.value.id}`)
      isFavorited.value = false
    } else {
      await request.post(`/favorites/album/${album.value.id}`)
      isFavorited.value = true
    }
  } catch { alert('操作失败') }
}

// ====== API ======

async function loadAlbum() {
  try {
    const res = await request.get(`/albums/${route.params.id}`)
    if (res.code === 200) album.value = res.data
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function playSong(song, idx) {
  if (album.value?.songs) playback.replaceQueue(album.value.songs, idx)
}

function formatDuration(sec) {
  if (!sec) return '--:--'
  return `${Math.floor(sec / 60)}:${(sec % 60).toString().padStart(2, '0')}`
}

onMounted(() => {
  loadAlbum()
  checkFavorite()
})
</script>

<style scoped>
.detail-page { min-height: 100vh; background: #f5f5f5; }
.main { max-width: 800px; margin: 0 auto; padding: 24px 20px; }

.album-hero { display: flex; gap: 24px; background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 24px; }
.hero-cover img { width: 200px; height: 200px; border-radius: 12px; object-fit: cover; }
.hero-info { flex: 1; }
.hero-info h2 { font-size: 24px; margin-bottom: 8px; }
.hero-artist { font-size: 16px; color: #666; margin-bottom: 8px; }
.hero-meta { font-size: 13px; color: #999; margin-bottom: 12px; }
.hero-desc { font-size: 14px; color: #666; line-height: 1.6; }

.hero-actions { margin-top: 12px; }
.btn-secondary {
  padding: 8px 20px; background: #fff; color: #666;
  border: 1px solid #ddd; border-radius: 8px; font-size: 14px; cursor: pointer;
  transition: all 0.2s;
}
.btn-secondary:hover { border-color: #1565C0; color: #1565C0; }
.btn-secondary.favorited { border-color: #E53935; color: #E53935; }

.section { background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 24px; }
.section h3 { font-size: 16px; margin-bottom: 16px; }

.song-row {
  display: flex; align-items: center; gap: 12px; padding: 10px 0;
  border-bottom: 1px solid #f5f5f5; cursor: pointer;
}
.song-row:hover { background: #f8f9fa; }
.row-idx { width: 24px; font-size: 12px; color: #999; text-align: center; }
.row-info { flex: 1; min-width: 0; }
.row-title { font-size: 14px; color: #333; cursor: pointer; }
.row-title:hover { color: #1565C0; }
.row-duration { font-size: 12px; color: #999; width: 50px; text-align: right; }
.play-btn { background: none; border: none; font-size: 16px; cursor: pointer; opacity: 0; padding: 4px 8px; }
.song-row:hover .play-btn { opacity: 1; }
.empty-hint { text-align: center; color: #ccc; padding: 20px; font-size: 13px; }
.loading { text-align: center; color: #999; padding: 60px; }
</style>
