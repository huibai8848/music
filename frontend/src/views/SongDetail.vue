<template>
  <div class="detail-page">
    <main v-if="song" class="main">
      <!-- 歌曲头部 -->
      <div class="song-hero">
        <div class="hero-cover">
          <DiscAnimation :cover-url="song.coverUrl || ''" :is-playing="playback.isPlaying" />
        </div>
        <div class="hero-info">
          <h2>{{ song.title }}</h2>
          <p class="hero-artist">
            <router-link v-if="song.artistId" :to="'/artists/' + song.artistId" class="hero-link">
              {{ song.artistName || '未知艺人' }}
            </router-link>
            <span v-else>{{ song.artistName || '未知艺人' }}</span>
          </p>
          <p class="hero-meta">
            <span>
              <router-link v-if="song.albumId" :to="'/albums/' + song.albumId" class="hero-link">
                {{ song.albumTitle || '未知专辑' }}
              </router-link>
              <span v-else>{{ song.albumTitle || '未知专辑' }}</span>
            </span>
            <span>·</span>
            <span>{{ song.genre || '-' }}</span>
            <span>·</span>
            <span>{{ song.language || '-' }}</span>
            <span>·</span>
            <span>{{ formatDuration(song.duration) }}</span>
            <span>·</span>
            <span>播放 {{ song.playCount || 0 }} 次</span>
            <span>·</span>
            <span>❤️ {{ song.likeCount || 0 }}</span>
            <span>·</span>
            <span>⭐ {{ song.favoriteCount || 0 }}</span>
          </p>
          <div class="hero-actions">
            <button class="btn-primary" @click="playCurrent">▶ 播放</button>

            <!-- 收藏按钮（toggle） -->
            <button class="btn-secondary" :class="{ favorited: isFavorited }" @click="toggleFavorite">
              {{ isFavorited ? '❤️ 已收藏' : '🤍 收藏' }}
            </button>

            <!-- 点赞按钮 -->
            <button class="btn-secondary" :class="{ liked: isLiked }" @click="toggleLike">
              {{ isLiked ? '👍 已赞' : '👍 点赞' }}
              <span v-if="likeCount > 0" class="count-badge">{{ likeCount }}</span>
            </button>

            <!-- 添加到歌单下拉 -->
            <div class="playlist-dropdown" ref="dropdownRef">
              <button class="btn-secondary" @click.stop="showPlaylistMenu = !showPlaylistMenu">
                📋 添加到歌单
              </button>
              <div v-if="showPlaylistMenu" class="dropdown-menu">
                <div v-if="myPlaylists.length === 0" class="dropdown-empty">暂无歌单</div>
                <div v-for="pl in myPlaylists" :key="pl.id" class="dropdown-item"
                     @click="addToPlaylist(pl)">
                  {{ pl.title }}
                </div>
              </div>
            </div>

            <!-- 举报按钮 -->
            <button class="btn-secondary report-btn" @click="showReport = true">
              🚨 举报
            </button>
          </div>
        </div>
      </div>

      <!-- 举报弹窗 -->
      <ReportDialog
        v-if="showReport && song"
        target-type="SONG"
        :target-id="song.id"
        :target-info="song.title"
        @close="showReport = false"
      />

      <!-- 歌词 -->
      <div class="lyrics-section">
        <h3>歌词</h3>
        <LyricsPanel :song-id="song.id" :current-time="playback.currentTime"
                     @seek="onSeek" class="lyrics-view" />
      </div>

      <!-- 评论 -->
      <div class="comments-section">
        <h3>评论</h3>
        <CommentSection :target-type="'SONG'" :target-id="song.id" />
      </div>
    </main>

    <div v-else-if="loading" class="loading">加载中...</div>
  </div>
</template>

<script setup>
/**
 * 歌曲详情页
 *
 * 功能：
 * 1. 展示歌曲完整信息
 * 2. 播放/收藏/点赞操作
 * 3. 添加到歌单
 * 4. 歌词同步（LyricsPanel）
 * 5. 评论区（CommentSection）
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { usePlaybackStore } from '../stores/playback'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'
import LyricsPanel from '../components/LyricsPanel.vue'
import DiscAnimation from '../components/DiscAnimation.vue'
import CommentSection from '../components/CommentSection.vue'
import ReportDialog from '../components/ReportDialog.vue'

const route = useRoute()
const playback = usePlaybackStore()
const authStore = useAuthStore()

const song = ref(null)
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
      if (Array.isArray(res.data)) {
        isFavorited.value = res.data.some(f => f.targetType === 'SONG' && Number(f.targetId) === Number(route.params.id))
      } else {
        isFavorited.value = list.some(f => f.targetType === 'SONG' && Number(f.targetId) === Number(route.params.id))
      }
    }
  } catch { /* ignore */ }
}

async function toggleFavorite() {
  if (!authStore.isLoggedIn) { alert('请先登录'); return }
  try {
    if (isFavorited.value) {
      await request.delete(`/favorites/song/${song.value.id}`)
      isFavorited.value = false
    } else {
      await request.post(`/favorites/song/${song.value.id}`)
      isFavorited.value = true
    }
  } catch { alert('操作失败') }
}

// ====== 点赞 ======
const isLiked = ref(false)
const likeCount = ref(0)

async function checkLike() {
  try {
    const res = await request.get(`/likes/song/${route.params.id}`)
    if (res.code === 200) {
      isLiked.value = !!res.data.liked
      likeCount.value = res.data.count || 0
    }
  } catch { /* ignore */ }
}

async function toggleLike() {
  if (!authStore.isLoggedIn) { alert('请先登录'); return }
  try {
    if (isLiked.value) {
      await request.delete(`/likes/song/${song.value.id}`)
      isLiked.value = false
      likeCount.value = Math.max(0, likeCount.value - 1)
    } else {
      await request.post(`/likes/song/${song.value.id}`)
      isLiked.value = true
      likeCount.value += 1
    }
  } catch { alert('操作失败') }
}

// ====== 添加到歌单 ======
const showPlaylistMenu = ref(false)
const myPlaylists = ref([])
const dropdownRef = ref(null)

// 举报
const showReport = ref(false)

async function loadMyPlaylists() {
  if (!authStore.isLoggedIn) return
  try {
    const res = await request.get('/playlists/mine')
    if (res.code === 200) myPlaylists.value = res.data || []
  } catch { /* ignore */ }
}

async function addToPlaylist(pl) {
  showPlaylistMenu.value = false
  try {
    const res = await request.post(`/playlists/${pl.id}/songs`, {
      songId: song.value.id
    })
    if (res.code === 200) {
      alert(`✅ 已添加到「${pl.title}」`)
    } else {
      alert(res.message || '添加失败')
    }
  } catch (e) {
    alert(e.message || '添加失败')
  }
}

// 点击外部关闭下拉
function onClickOutside(e) {
  if (showPlaylistMenu.value && dropdownRef.value && !dropdownRef.value.contains(e.target)) {
    showPlaylistMenu.value = false
  }
}

// ====== API ======

async function loadSong() {
  try {
    const res = await request.get(`/songs/${route.params.id}`)
    if (res.code === 200) song.value = res.data
  } catch { song.value = null }
  finally { loading.value = false }
}

function playCurrent() {
  if (song.value) {
    playback.replaceQueue([song.value], 0)
    // 上报播放次数
    request.post(`/songs/${song.value.id}/play`).catch(() => {})
  }
}

function onSeek(timeMs) { playback.seekTo(timeMs) }

function formatDuration(sec) {
  if (!sec) return '--:--'
  return `${Math.floor(sec / 60)}:${(sec % 60).toString().padStart(2, '0')}`
}

onMounted(() => {
  loadSong()
  checkFavorite()
  checkLike()
  loadMyPlaylists()
  document.addEventListener('click', onClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
})
</script>

<style scoped>
.detail-page { min-height: 100vh; background: #f5f5f5; }

.main { max-width: 800px; margin: 0 auto; padding: 24px 20px; }

.song-hero { display: flex; gap: 24px; background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 24px; }
.hero-cover { width: 140px; height: 140px; flex-shrink: 0; }
.hero-info { flex: 1; display: flex; flex-direction: column; justify-content: center; }
.hero-info h2 { font-size: 24px; margin-bottom: 8px; }
.hero-artist { font-size: 16px; color: #666; margin-bottom: 12px; }
.hero-link { color: #1565C0; text-decoration: none; cursor: pointer; }
.hero-link:hover { text-decoration: underline; color: #0D47A1; }
.hero-meta { font-size: 13px; color: #999; display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }
.hero-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.btn-primary { padding: 10px 24px; background: #1565C0; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }
.btn-primary:hover { background: #0D47A1; }

.btn-secondary {
  padding: 10px 20px; background: #fff; color: #666;
  border: 1px solid #ddd; border-radius: 8px; font-size: 14px; cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.btn-secondary:hover { border-color: #1565C0; color: #1565C0; }
.btn-secondary.favorited { border-color: #E53935; color: #E53935; }
.btn-secondary.liked { border-color: #1565C0; color: #1565C0; background: #E3F2FD; }
.report-btn { border-color: #E53935; color: #E53935; }
.report-btn:hover { background: #FFEBEE; border-color: #C62828; color: #C62828; }

.count-badge {
  background: #E53935; color: #fff;
  font-size: 10px; padding: 1px 5px; border-radius: 6px;
  margin-left: 4px;
}

/* 歌单下拉 */
.playlist-dropdown { position: relative; }

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 4px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  z-index: 100;
  min-width: 160px;
  max-height: 240px;
  overflow-y: auto;
}

.dropdown-empty {
  padding: 16px; text-align: center; color: #999; font-size: 13px;
}

.dropdown-item {
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.dropdown-item:hover { background: #f5f5f5; }

/* 歌词 + 评论 */
.lyrics-section, .comments-section {
  background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 24px;
}
.lyrics-section h3, .comments-section h3 { font-size: 16px; margin-bottom: 16px; }
.lyrics-view { max-height: 300px; overflow-y: auto; }
.loading { text-align: center; color: #999; padding: 60px; }
</style>
