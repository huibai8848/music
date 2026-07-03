<template>
  <div class="list-page">
    <main class="main">
      <h2 class="page-title">💿 专辑列表</h2>

      <div v-if="loading" class="loading-text">加载中...</div>
      <div v-else-if="albums.length === 0" class="empty-text">暂无专辑</div>
      <div v-else class="album-grid">
        <div v-for="al in albums" :key="al.id" class="album-card"
             @click="$router.push('/albums/' + al.id)">
          <img :src="al.coverUrl || defaultCover" @error="onCoverError" alt="">
          <div class="album-info">
            <div class="album-title">{{ al.title }}</div>
            <div class="album-artist">{{ al.artistName || '未知' }}</div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="page <= 1" @click="page--; loadAlbums()">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button :disabled="page >= totalPages" @click="page++; loadAlbums()">下一页</button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'

const albums = ref([])
const loading = ref(true)
const page = ref(1)
const totalPages = ref(1)
const size = 20

const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">💿</text></svg>')

function onCoverError(e) { e.target.src = defaultCover }

async function loadAlbums() {
  loading.value = true
  try {
    const res = await request.get('/albums', { params: { page: page.value, size } })
    if (res.code === 200) {
      albums.value = res.data?.records || []
      const total = res.data?.total || 0
      totalPages.value = Math.ceil(total / size) || 1
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

onMounted(loadAlbums)
</script>

<style scoped>
.list-page { min-height: 100vh; background: #f5f5f5; }
.main { max-width: 1000px; margin: 0 auto; padding: 24px 20px; }

.page-title { font-size: 22px; margin-bottom: 20px; color: #333; }

.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.album-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.album-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}

.album-card img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
}

.album-info {
  padding: 10px 12px 12px;
}

.album-title {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.album-artist {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.loading-text, .empty-text {
  text-align: center;
  color: #999;
  padding: 60px;
  font-size: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
}

.pagination button {
  padding: 6px 16px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination span {
  font-size: 13px;
  color: #666;
}
</style>
