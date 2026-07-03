<template>
  <div class="user-page">
    <main v-if="user" class="main">
      <!-- 用户头部 -->
      <div class="user-hero">
        <div class="hero-avatar">{{ user.nickname ? user.nickname.charAt(0).toUpperCase() : '?' }}</div>
        <div class="hero-info">
          <h2>{{ user.nickname || '匿名用户' }}</h2>
          <p class="hero-email">{{ user.email || '' }}</p>
          <p class="hero-joined">加入时间：{{ formatDate(user.createdTime) }}</p>
          <div class="hero-stats">
            <span>📋 {{ playlists.length }} 个歌单</span>
            <span>⭐ {{ favoriteCount }} 个收藏</span>
          </div>
        </div>
      </div>

      <!-- 歌单列表 -->
      <section class="section">
        <h3>📋 创建的歌单</h3>
        <div v-if="playlists.length === 0" class="empty-hint">暂无歌单</div>
        <div v-else class="pl-grid">
          <div
            v-for="pl in playlists"
            :key="pl.id"
            class="pl-card"
            @click="$router.push('/playlists/' + pl.id)"
          >
            <img :src="pl.coverUrl || defaultCover" @error="onCoverError" alt="">
            <div class="pl-info">
              <div class="pl-title">{{ pl.title }}</div>
              <div class="pl-meta">{{ pl.songCount || 0 }} 首</div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 加载状态 -->
    <div v-else-if="loading" class="loading">加载中...</div>

    <!-- 404 状态 -->
    <div v-else class="loading">
      <p>用户不存在或已注销</p>
    </div>
  </div>
</template>

<script setup>
/**
 * 用户公开主页
 *
 * 展示用户公开信息、创建的歌单等。
 * 通过 /api/users/:id 获取用户信息。
 */
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'

const route = useRoute()

const user = ref(null)
const playlists = ref([])
const favoriteCount = ref(0)
const loading = ref(true)
const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">📋</text></svg>')

function onCoverError(e) { e.target.src = defaultCover }

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get(`/users/${route.params.id}`)
    if (res.code === 200) {
      user.value = res.data
      playlists.value = res.data.playlists || []
      favoriteCount.value = res.data.favoriteCount || 0
    }
  } catch { /* user not found */ }
  finally { loading.value = false }
})

function formatDate(t) {
  if (!t) return ''
  return new Date(t).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.user-page { min-height: 100vh; background: #f5f5f5; }
.main { max-width: 800px; margin: 0 auto; padding: 24px 20px; }

.user-hero {
  display: flex; gap: 24px; background: linear-gradient(135deg, #1565C0, #0D47A1);
  border-radius: 16px; padding: 32px; color: #fff; margin-bottom: 24px;
}
.hero-avatar {
  width: 80px; height: 80px; border-radius: 50%; background: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
  font-size: 32px; font-weight: 700; flex-shrink: 0;
}
.hero-info { flex: 1; }
.hero-info h2 { margin: 0 0 4px 0; font-size: 24px; }
.hero-email { font-size: 14px; opacity: 0.8; margin-bottom: 4px; }
.hero-joined { font-size: 13px; opacity: 0.7; margin-bottom: 12px; }
.hero-stats { display: flex; gap: 20px; font-size: 14px; }
.hero-stats span { background: rgba(255,255,255,0.15); padding: 4px 12px; border-radius: 12px; }

.section { background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 24px; }
.section h3 { font-size: 16px; margin-bottom: 16px; color: #333; }

.pl-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}
.pl-card {
  background: #fafafa; border-radius: 10px; overflow: hidden; cursor: pointer;
  transition: transform 0.2s;
}
.pl-card:hover { transform: translateY(-2px); }
.pl-card img { width: 100%; aspect-ratio: 1; object-fit: cover; }
.pl-info { padding: 8px 10px; }
.pl-title { font-size: 13px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pl-meta { font-size: 12px; color: #999; margin-top: 2px; }

.empty-hint { text-align: center; color: #ccc; padding: 40px; font-size: 14px; }
.loading { text-align: center; color: #999; padding: 60px; font-size: 14px; }
</style>
