<template>
  <div class="artist-detail">
    <main v-if="artist" class="main">
      <!-- 艺人头部 -->
      <div class="artist-hero">
        <div class="hero-avatar">
          <img :src="artist.avatar || defaultAvatar" @error="onAvatarError" alt="">
        </div>
        <div class="hero-info">
          <h2>{{ artist.name }}</h2>
          <p class="hero-meta">{{ artist.country || '-' }} · {{ artist.songCount || 0 }} 首歌</p>
          <p class="hero-bio">{{ artist.bio || '暂无简介' }}</p>
          <div class="hero-actions">
            <button class="btn-secondary" :class="{ favorited: isFavorited }" @click="toggleFavorite">
              {{ isFavorited ? '❤️ 已关注' : '🤍 关注' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 歌曲列表 -->
      <div class="section">
        <h3>热门作品</h3>
        <div class="song-table">
          <div v-for="(song, idx) in artist.songs" :key="song.id" class="song-row"
               @dblclick="playSong(song, idx)">
            <span class="row-idx">{{ idx + 1 }}</span>
            <div class="row-info">
              <div class="row-title" @click="$router.push('/songs/' + song.id)">{{ song.title }}</div>
            </div>
            <span class="row-duration">{{ formatDuration(song.duration) }}</span>
            <button class="play-btn" @click.stop="playSong(song, idx)">▶</button>
          </div>
          <div v-if="!artist.songs?.length" class="empty-hint">暂无歌曲</div>
        </div>
      </div>

      <!-- 专辑列表 -->
      <div class="section">
        <h3>专辑</h3>
        <div class="album-grid">
          <div v-for="al in artist.albums" :key="al.id" class="album-card"
               @click="$router.push('/albums/' + al.id)">
            <img :src="al.coverUrl || defaultCover" @error="onCoverError" alt="">
            <div class="album-info">
              <div class="album-title">{{ al.title }}</div>
              <div class="album-year">{{ al.releaseDate || '' }}</div>
            </div>
          </div>
          <div v-if="!artist.albums?.length" class="empty-hint">暂无专辑</div>
        </div>
      </div>
    </main>

    <div v-else-if="loading" class="loading">加载中...</div>
  </div>
</template>

<script setup>
/**
 * 艺人详情页
 *
 * 功能：
 * 1. 展示艺人信息 + 热门歌曲 + 专辑
 * 2. 播放歌曲 / 跳转歌曲/专辑详情
 * 3. 关注/取消关注艺人（收藏）
 */
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { usePlaybackStore } from '../stores/playback'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'

const route = useRoute()
const playback = usePlaybackStore()
const authStore = useAuthStore()

const artist = ref(null)
const loading = ref(true)
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎤</text></svg>')
const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>')

function onAvatarError(e) { e.target.src = defaultAvatar }
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
        f.targetType === 'ARTIST' && Number(f.targetId) === Number(route.params.id))
    }
  } catch { /* ignore */ }
}

async function toggleFavorite() {
  if (!authStore.isLoggedIn) { alert('请先登录'); return }
  try {
    if (isFavorited.value) {
      await request.delete(`/favorites/artist/${artist.value.id}`)
      isFavorited.value = false
    } else {
      await request.post(`/favorites/artist/${artist.value.id}`)
      isFavorited.value = true
    }
  } catch { alert('操作失败') }
}

// ====== API ======

async function loadArtist() {
  try {
    const res = await request.get(`/artists/${route.params.id}`)
    if (res.code === 200) artist.value = res.data
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function playSong(song, idx) {
  if (artist.value?.songs) playback.replaceQueue(artist.value.songs, idx)
}

function formatDuration(sec) {
  if (!sec) return '--:--'
  return `${Math.floor(sec / 60)}:${(sec % 60).toString().padStart(2, '0')}`
}

onMounted(() => {
  loadArtist()
  checkFavorite()
})
</script>

<style scoped>
.artist-detail { min-height: 100vh; background: #f5f5f5; }
.main { max-width: 800px; margin: 0 auto; padding: 24px 20px; }

.artist-hero { display: flex; gap: 24px; background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 24px; align-items: center; }
.hero-avatar img { width: 120px; height: 120px; border-radius: 50%; object-fit: cover; }
.hero-info { flex: 1; }
.hero-info h2 { font-size: 24px; margin-bottom: 8px; }
.hero-meta { font-size: 13px; color: #999; margin-bottom: 12px; }
.hero-bio { font-size: 14px; color: #666; line-height: 1.6; }

.hero-actions { margin-top: 8px; }
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

.album-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; }
.album-card { cursor: pointer; border-radius: 8px; overflow: hidden; transition: transform 0.2s; }
.album-card:hover { transform: translateY(-2px); }
.album-card img { width: 100%; aspect-ratio: 1; object-fit: cover; }
.album-info { padding: 8px; }
.album-title { font-size: 13px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.album-year { font-size: 11px; color: #999; }
.loading { text-align: center; color: #999; padding: 60px; }
</style>
