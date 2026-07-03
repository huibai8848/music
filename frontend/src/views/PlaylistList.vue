<template>
  <div class="list-page">
    <main class="main">
      <div class="page-header-row">
        <h2 class="page-title">📋 歌单</h2>
        <div class="header-actions">
          <button v-if="authStore.isLoggedIn" class="btn-create" @click="showCreate = true">+ 创建歌单</button>
        </div>
      </div>

      <!-- 选项卡 -->
      <div class="tabs">
        <button class="tab-btn" :class="{ active: activeTab === 'public' }" @click="activeTab = 'public'; page=1; loadPlaylists()">公共歌单</button>
        <button v-if="authStore.isLoggedIn" class="tab-btn" :class="{ active: activeTab === 'mine' }" @click="activeTab = 'mine'; page=1; loadPlaylists()">我的歌单</button>
      </div>

      <div class="playlist-grid">
        <div v-for="pl in playlists" :key="pl.id" class="playlist-card"
             @click="$router.push('/playlists/' + pl.id)">
          <img :src="pl.coverUrl || defaultCover" @error="onCoverError" alt="">
          <div class="card-info">
            <div class="card-title">{{ pl.title }}</div>
            <div class="card-meta">{{ pl.songCount || 0 }} 首 · {{ pl.nickname || '匿名' }}
              <span v-if="pl.isPublic === false" class="badge-private">私密</span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="playlists.length === 0" class="empty-hint">
        {{ activeTab === 'mine' ? '你还没有创建歌单' : '暂无公开歌单' }}
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="page <= 1" @click="page--; loadPlaylists()">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button :disabled="page >= totalPages" @click="page++; loadPlaylists()">下一页</button>
      </div>
    </main>

    <!-- 创建歌单弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal-card">
        <h3>创建歌单</h3>

        <!-- 封面图上传 -->
        <div class="cover-upload" @click="openCoverPicker">
          <img v-if="coverPreview" :src="coverPreview" class="cover-preview" />
          <div v-else class="cover-placeholder">
            <span class="cover-icon">📷</span>
            <span class="cover-text">点击上传封面图</span>
          </div>
          <div v-if="coverUploading" class="cover-overlay">上传中...</div>
        </div>
        <input ref="coverInput" type="file" accept="image/jpeg,image/png" style="display:none" @change="handleCoverSelect" />

        <input v-model="newTitle" placeholder="歌单名称" class="modal-input" maxlength="100" />
        <textarea v-model="newDesc" placeholder="描述（可选）" class="modal-textarea" rows="3"></textarea>

        <!-- 公开/隐藏切换 -->
        <div class="vis-row">
          <span class="vis-label">公开歌单</span>
          <label class="switch">
            <input type="checkbox" v-model="newIsPublic" />
            <span class="slider"></span>
          </label>
        </div>
        <p class="vis-hint">{{ newIsPublic ? '公开 — 所有人可见' : '隐藏 — 仅自己可见' }}</p>

        <div class="modal-actions">
          <button @click="closeCreate" class="btn-cancel">取消</button>
          <button @click="handleCreate" class="btn-confirm">创建</button>
        </div>
        <p v-if="createError" class="error">{{ createError }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'

const router = useRouter()
const authStore = useAuthStore()

const playlists = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const totalPages = ref(1)
const activeTab = ref('public')

const showCreate = ref(false)
const newTitle = ref('')
const newDesc = ref('')
const newIsPublic = ref(true)
const createError = ref('')

// 封面图上传
const coverInput = ref(null)
const coverPreview = ref('')
const coverUrl = ref('')
const coverUploading = ref(false)

function openCoverPicker() {
  coverInput.value?.click()
}

async function handleCoverSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  coverPreview.value = URL.createObjectURL(file)
  coverUploading.value = true
  const fd = new FormData()
  fd.append('file', file)
  fd.append('type', 'cover')
  try {
    const res = await request.post('/files/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 200 && res.data?.url) {
      coverUrl.value = res.data.url
    } else {
      coverPreview.value = ''
      throw new Error(res.message || '上传失败')
    }
  } catch {
    coverPreview.value = ''
    alert('封面上传失败')
  } finally {
    coverUploading.value = false
  }
}

function closeCreate() {
  showCreate.value = false
  createError.value = ''
  newTitle.value = ''
  newDesc.value = ''
  newIsPublic.value = true
  coverPreview.value = ''
  coverUrl.value = ''
  if (coverInput.value) coverInput.value.value = ''
}

const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">📋</text></svg>')

function onCoverError(e) { e.target.src = defaultCover }

async function loadPlaylists() {
  try {
    let res
    if (activeTab.value === 'mine' && authStore.isLoggedIn) {
      res = await request.get('/playlists/mine')
      if (res.code === 200) {
        playlists.value = res.data || []
        totalPages.value = 1
        return
      }
    }
    res = await request.get('/playlists', { params: { page: page.value, size: size.value } })
    if (res.code === 200) {
      playlists.value = res.data.records || []
      total.value = res.data.total || 0
      totalPages.value = Math.ceil(total.value / size.value) || 1
    }
  } catch { playlists.value = [] }
}

async function handleCreate() {
  if (!newTitle.value.trim()) { createError.value = '请输入歌单名称'; return }
  createError.value = ''
  try {
    const res = await request.post('/playlists', {
      title: newTitle.value,
      description: newDesc.value,
      coverUrl: coverUrl.value || null,
      isPublic: newIsPublic.value
    })
    if (res.code === 200) {
      const id = res.data.id
      closeCreate()
      router.push('/playlists/' + id)
    } else {
      createError.value = res.message || '创建失败'
    }
  } catch { createError.value = '创建失败' }
}

onMounted(loadPlaylists)
</script>

<style scoped>
.list-page { min-height: 100vh; background: #f5f5f5; }
.main { max-width: 1000px; margin: 0 auto; padding: 24px 20px; }

.page-header-row {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}

.page-title { font-size: 22px; color: #333; }

.header-actions { display: flex; gap: 12px; align-items: center; }

.btn-create {
  padding: 8px 20px;
  background: #1565C0; color: #fff;
  border: none; border-radius: 8px;
  font-size: 14px; cursor: pointer;
}

.btn-create:hover { background: #0D47A1; }

/* 选项卡 */
.tabs { display: flex; gap: 4px; margin-bottom: 16px; }

.tab-btn {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px 8px 0 0;
  font-size: 14px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #1565C0;
  color: #fff;
  border-color: #1565C0;
}

.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.playlist-card {
  background: #fff; border-radius: 10px; overflow: hidden; cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.playlist-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.playlist-card img { width: 100%; aspect-ratio: 1; object-fit: cover; }
.card-info { padding: 10px 12px 12px; }
.card-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-meta { font-size: 12px; color: #999; margin-top: 4px; }
.badge-private { display: inline-block; background: #FF9800; color: #fff; font-size: 10px; padding: 1px 6px; border-radius: 8px; margin-left: 4px; vertical-align: middle; }

.empty-hint { text-align: center; color: #ccc; padding: 80px; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 20px; }
.pagination button { padding: 6px 16px; border: 1px solid #ddd; border-radius: 6px; background: #fff; font-size: 13px; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #666; }

/* 弹窗 */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center; z-index: 2000;
}
.modal-card { background: #fff; border-radius: 12px; padding: 24px; width: 380px; }
.modal-card h3 { margin-bottom: 16px; font-size: 18px; }
.modal-input { width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; margin-bottom: 12px; box-sizing: border-box; }
.modal-textarea { width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; resize: vertical; margin-bottom: 12px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 16px; }
.btn-cancel { padding: 8px 20px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; cursor: pointer; }
.btn-confirm { padding: 8px 20px; background: #1565C0; color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
.btn-confirm:hover { background: #0D47A1; }
.error { color: #E53935; font-size: 13px; margin-top: 8px; }

/* 封面图上传 */
.cover-upload {
  width: 100%; aspect-ratio: 16/9; border-radius: 8px;
  background: #f0f0f0; cursor: pointer; overflow: hidden;
  position: relative; margin-bottom: 12px;
  border: 2px dashed #ccc;
  transition: border-color 0.2s;
}
.cover-upload:hover { border-color: #1565C0; }
.cover-placeholder {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; height: 100%; gap: 8px;
}
.cover-icon { font-size: 32px; }
.cover-text { font-size: 13px; color: #999; }
.cover-preview { width: 100%; height: 100%; object-fit: cover; }
.cover-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 14px;
}

/* 可见性切换 */
.vis-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 0;
}
.vis-label { font-size: 14px; color: #333; }
.vis-hint { font-size: 12px; color: #999; margin-top: -4px; margin-bottom: 4px; }

/* Switch 开关 */
.switch { position: relative; display: inline-block; width: 44px; height: 24px; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider {
  position: absolute; cursor: pointer; inset: 0;
  background: #ccc; border-radius: 24px; transition: 0.3s;
}
.slider::before {
  content: ''; position: absolute; height: 18px; width: 18px;
  left: 3px; bottom: 3px; background: #fff; border-radius: 50%;
  transition: 0.3s;
}
.switch input:checked + .slider { background: #1565C0; }
.switch input:checked + .slider::before { transform: translateX(20px); }
</style>
