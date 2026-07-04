<template>
  <div class="home-page">
    <!-- Banner 轮播 -->
    <div class="banner-carousel" v-if="banners.length > 0">
      <div class="banner-track" :style="{ transform: `translateX(-${bannerIndex * 100}%)` }">
        <div v-for="b in banners" :key="b.id" class="banner-slide">
          <a :href="b.linkUrl || '#'" target="_blank" v-if="b.linkUrl">
            <img :src="b.imageUrl" :alt="b.title" @error="onBannerError">
          </a>
          <img v-else :src="b.imageUrl" :alt="b.title" @error="onBannerError">
          <div v-if="b.title" class="banner-caption">{{ b.title }}</div>
        </div>
      </div>
      <div class="banner-dots">
        <span v-for="(b, i) in banners" :key="i" class="dot"
              :class="{ active: i === bannerIndex }" @click="bannerIndex = i"></span>
      </div>
    </div>

    <!-- 系统公告 -->
    <div v-if="notices.length > 0" class="notice-bar">
      <div v-for="n in notices" :key="n.id" class="notice-item" :class="'notice-' + (n.type || 'SYSTEM').toLowerCase()">
        <span class="notice-icon">{{ noticeIcon(n.type) }}</span>
        <div class="notice-content">
          <span class="notice-label">{{ noticeTypeLabel(n.type) }}：</span>
          <span class="notice-title">{{ n.title }}</span>
          <span v-if="n.content" class="notice-text">—— {{ n.content }}</span>
        </div>
      </div>
    </div>

    <!-- 导航快捷入口 -->
    <div class="quick-nav">
      <router-link to="/songs" class="quick-item">
        <span class="quick-icon">🎵</span>
        <span>歌曲</span>
      </router-link>
      <router-link to="/albums" class="quick-item">
        <span class="quick-icon">💿</span>
        <span>专辑</span>
      </router-link>
      <router-link to="/artists" class="quick-item">
        <span class="quick-icon">🎤</span>
        <span>艺人</span>
      </router-link>
      <router-link to="/playlists" class="quick-item">
        <span class="quick-icon">📋</span>
        <span>歌单</span>
      </router-link>
      <router-link to="/rooms" class="quick-item">
        <span class="quick-icon">🏠</span>
        <span>歌房</span>
      </router-link>
      <router-link v-if="authStore.isLoggedIn" to="/upload-music" class="quick-item">
        <span class="quick-icon">🎵</span>
        <span>上传音乐</span>
      </router-link>
    </div>

    <!-- 热门歌曲 -->
    <section class="section">
      <div class="section-header">
        <h2>🔥 热门歌曲</h2>
        <router-link to="/songs" class="more-link">更多 →</router-link>
      </div>
      <div class="song-grid">
        <div v-for="(song, idx) in hotSongs" :key="song.id" class="song-card"
             @dblclick="playSong(song)">
          <div class="card-cover" @click="goSongDetail(song)">
            <img :src="song.coverUrl || defaultCover" @error="onCoverError" alt="">
            <div class="play-overlay">▶</div>
            <span class="rank-badge">{{ idx + 1 }}</span>
          </div>
          <div class="card-info">
            <div class="card-title" @click="goSongDetail(song)">{{ song.title }}</div>
            <div class="card-artist">{{ song.artistName || '未知' }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 推荐歌单 -->
    <section class="section">
      <div class="section-header">
        <h2>📋 推荐歌单</h2>
        <router-link to="/playlists" class="more-link">更多 →</router-link>
      </div>
      <div v-if="playlists.length === 0" class="empty-hint">暂无歌单</div>
      <div v-else class="playlist-grid">
        <div v-for="pl in playlists" :key="pl.id" class="playlist-card"
             @click="$router.push('/playlists/' + pl.id)">
          <div class="card-cover playlist-cover">
            <img :src="pl.coverUrl || defaultCover" @error="onCoverError" alt="">
            <div class="play-overlay">📋</div>
          </div>
          <div class="card-info">
            <div class="card-title">{{ pl.title }}</div>
            <div class="card-artist">{{ pl.songCount || 0 }} 首 · {{ pl.nickname || '匿名' }}</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
/**
 * 首页
 *
 * 展示 Banner 轮播、快捷导航入口、热门歌曲和推荐歌单。
 * Banner 每 5 秒自动轮播。
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { usePlaybackStore } from '../stores/playback'
import request from '../utils/request'

const router = useRouter()
const authStore = useAuthStore()
const playbackStore = usePlaybackStore()

const banners = ref([])
const hotSongs = ref([])
const playlists = ref([])
const notices = ref([])
const bannerIndex = ref(0)
let bannerTimer = null

const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>')

function onCoverError(e) { e.target.src = defaultCover }
function onBannerError(e) { e.target.style.display = 'none' }

const NOTICE_ICONS = { SYSTEM: '📢', MAINTENANCE: '🔧', ACTIVITY: '🎉' }
function noticeIcon(type) { return NOTICE_ICONS[type] || '📢' }
function noticeTypeLabel(t) {
  const map = { SYSTEM: '系统公告', MAINTENANCE: '维护通知', ACTIVITY: '活动公告' }
  return map[t] || t || '公告'
}

async function loadData() {
  try {
    const [bannerRes, hotRes, plRes, noticeRes] = await Promise.all([
      request.get('/banners'),
      request.get('/songs/hot', { params: { limit: 8 } }),
      request.get('/playlists', { params: { page: 1, size: 6 } }),
      request.get('/notices')
    ])
    if (bannerRes.code === 200) banners.value = bannerRes.data || []
    if (hotRes.code === 200) hotSongs.value = hotRes.data || []
    if (plRes.code === 200) playlists.value = plRes.data?.records || []
    if (noticeRes.code === 200) notices.value = noticeRes.data || []
  } catch (e) {
    console.warn('加载首页数据失败', e)
  }
}

function playSong(song) {
  playbackStore.replaceQueue([song], 0)
}

function goSongDetail(song) {
  router.push('/songs/' + song.id)
}

function startBannerLoop() {
  bannerTimer = setInterval(() => {
    if (banners.value.length > 0) {
      bannerIndex.value = (bannerIndex.value + 1) % banners.value.length
    }
  }, 5000)
}

onMounted(() => {
  loadData()
  startBannerLoop()
})

onUnmounted(() => {
  if (bannerTimer) clearInterval(bannerTimer)
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f5f5f5;
}

/* ===== Banner 轮播 ===== */
.banner-carousel {
  position: relative;
  width: 100%;
  max-width: 1000px;
  margin: 16px auto 0;
  overflow: hidden;
  border-radius: 12px;
  background: #e0e0e0;
}

.banner-track {
  display: flex;
  transition: transform 0.5s ease;
}

.banner-slide {
  min-width: 100%;
  position: relative;
}

.banner-slide img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
}

.banner-caption {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 20px;
  background: linear-gradient(transparent, rgba(0,0,0,0.6));
  color: #fff;
  font-size: 14px;
}

.banner-dots {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  cursor: pointer;
  transition: all 0.3s;
}

.dot.active {
  background: #fff;
  width: 20px;
  border-radius: 4px;
}

/* ===== 系统公告 ===== */
.notice-bar {
  max-width: 1000px;
  margin: 12px auto 0;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.notice-system {
  background: #e3f2fd;
  color: #1565C0;
  border: 1px solid #bbdefb;
}

.notice-maintenance {
  background: #fff3e0;
  color: #e65100;
  border: 1px solid #ffe0b2;
}

.notice-activity {
  background: #f3e5f5;
  color: #7b1fa2;
  border: 1px solid #e1bee7;
}

.notice-icon { font-size: 18px; flex-shrink: 0; margin-top: 1px; }

.notice-content {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.notice-label { font-weight: 600; }
.notice-title { font-weight: 500; }
.notice-text { color: inherit; opacity: 0.85; }

/* ===== 快捷导航 ===== */
.quick-nav {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 16px 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: #fff;
  border-radius: 12px;
  text-decoration: none;
  color: #333;
  font-size: 13px;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  min-width: 80px;
}

.quick-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}

.quick-icon {
  font-size: 24px;
}

/* ===== 内容区域 ===== */
.main { max-width: 1000px; margin: 0 auto; padding: 0 20px 24px; }
.section { max-width: 1000px; margin: 0 auto 24px; padding: 0 20px; }
.section-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.section-header h2 { font-size: 18px; color: #333; }
.more-link { font-size: 13px; color: #1565C0; }

/* 歌曲网格 */
.song-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

/* 歌单网格 */
.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.song-card, .playlist-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.song-card:hover, .playlist-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}

.card-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
}
.card-cover img { width: 100%; height: 100%; object-fit: cover; }

.playlist-cover img { border-radius: 0; }

.play-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
}
.song-card:hover .play-overlay { opacity: 1; }
.playlist-card:hover .play-overlay { opacity: 1; }

.rank-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.card-info { padding: 10px 12px 12px; }
.card-title {
  font-size: 14px; font-weight: 500; color: #333;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-title:hover { color: #1565C0; }
.card-artist { font-size: 12px; color: #999; margin-top: 4px; }
.empty-hint { text-align: center; color: #ccc; padding: 40px; }
</style>
