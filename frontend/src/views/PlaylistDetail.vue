<template>
  <div class="detail-page">
    <main v-if="playlist" class="main">
      <!-- 歌单头部 -->
      <div class="pl-hero">
        <div class="hero-cover">
          <img :src="playlist.coverUrl || defaultCover" @error="onCoverError" alt="">
        </div>
        <div class="hero-info">
          <h2>{{ playlist.title }}</h2>
          <p class="hero-meta">{{ playlist.nickname || '匿名' }} · {{ playlist.songCount || 0 }} 首
            <span v-if="playlist.isPublic === false" class="badge-private">私密</span>
          </p>
          <p class="hero-desc">{{ playlist.description || '暂无描述' }}</p>
          <div class="hero-actions">
            <button class="btn-primary" @click="playAll">▶ 播放全部</button>
            <button v-if="isOwner" class="btn-edit" @click="showEditModal = true">✏️ 编辑</button>
            <button v-if="isOwner" class="btn-danger" @click="handleDelete">🗑️ 删除</button>
          </div>
        </div>
      </div>

      <!-- 歌曲列表 -->
      <div class="song-section">
        <h3>歌曲</h3>
        <div v-if="songs.length === 0" class="empty-hint">歌单为空</div>
        <div v-else class="song-table">
          <div v-for="(song, idx) in songs" :key="song.id" class="song-row"
               @dblclick="playSong(song, idx)">
            <span class="row-idx">{{ idx + 1 }}</span>
            <div class="row-cover">
              <img :src="song.coverUrl || defaultCover" @error="onCoverError" alt="">
            </div>
            <div class="row-info">
              <div class="row-title" @click="$router.push('/songs/' + song.id)">{{ song.title }}</div>
              <div class="row-artist">{{ song.artistName || '未知' }}</div>
            </div>
            <span class="row-duration">{{ formatDuration(song.duration) }}</span>
            <button class="play-btn" @click.stop="playSong(song, idx)">▶</button>
            <button v-if="isOwner" class="remove-btn" @click.stop="removeSong(song.id)" title="移除">✕</button>
          </div>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="section">
        <h3>评论</h3>
        <CommentSection :target-type="'PLAYLIST'" :target-id="playlist.id" />
      </div>
    </main>

    <!-- 编辑歌单弹窗 -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal-card">
        <h3>✏️ 编辑歌单</h3>

        <!-- 封面图上传 -->
        <div class="cover-upload" @click="openEditCoverPicker">
          <img v-if="editCoverPreview" :src="editCoverPreview" class="cover-preview" />
          <div v-else class="cover-placeholder">
            <span class="cover-icon">📷</span>
            <span class="cover-text">点击修改封面图</span>
          </div>
          <div v-if="editCoverUploading" class="cover-overlay">上传中...</div>
        </div>
        <input ref="editCoverInput" type="file" accept="image/jpeg,image/png" style="display:none" @change="handleEditCoverSelect" />

        <input v-model="editForm.title" placeholder="歌单名称" class="modal-input" maxlength="100" />
        <textarea v-model="editForm.description" placeholder="描述（可选）" class="modal-textarea" rows="3"></textarea>

        <!-- 公开/隐藏切换 -->
        <div class="vis-row">
          <span class="vis-label">公开歌单</span>
          <label class="switch">
            <input type="checkbox" v-model="editForm.isPublic" />
            <span class="slider"></span>
          </label>
        </div>
        <p class="vis-hint">{{ editForm.isPublic ? '公开 — 所有人可见' : '隐藏 — 仅自己可见' }}</p>

        <div class="modal-actions">
          <button @click="showEditModal = false" class="btn-cancel">取消</button>
          <button @click="handleEdit" class="btn-confirm" :disabled="editing">{{ editing ? '保存中...' : '保存' }}</button>
        </div>
        <p v-if="editError" class="error">{{ editError }}</p>
      </div>
    </div>

    <div v-else-if="loading" class="loading">加载中...</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { usePlaybackStore } from '../stores/playback'
import request from '../utils/request'
import CommentSection from '../components/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const playback = usePlaybackStore()

const playlist = ref(null)
const songs = ref([])
const loading = ref(true)
const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">📋</text></svg>')

const isOwner = computed(() => playlist.value && authStore.user?.id === playlist.value.userId)

// ====== 编辑歌单 ======
const showEditModal = ref(false)
const editForm = ref({ title: '', description: '', isPublic: true, coverUrl: '' })
const editing = ref(false)
const editError = ref('')

// 编辑封面图上传
const editCoverInput = ref(null)
const editCoverPreview = ref('')
const editCoverUrl = ref('')
const editCoverUploading = ref(false)

function openEditModal() {
  editForm.value = {
    title: playlist.value?.title || '',
    description: playlist.value?.description || '',
    isPublic: playlist.value?.isPublic !== false,
    coverUrl: playlist.value?.coverUrl || ''
  }
  editCoverPreview.value = playlist.value?.coverUrl || ''
  editCoverUrl.value = playlist.value?.coverUrl || ''
  editError.value = ''
  showEditModal.value = true
}

function openEditCoverPicker() {
  editCoverInput.value?.click()
}

async function handleEditCoverSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  editCoverPreview.value = URL.createObjectURL(file)
  editCoverUploading.value = true
  const fd = new FormData()
  fd.append('file', file)
  fd.append('type', 'cover')
  try {
    const res = await request.post('/files/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 200 && res.data?.url) {
      editCoverUrl.value = res.data.url
    } else {
      editCoverPreview.value = ''
      throw new Error(res.message || '上传失败')
    }
  } catch {
    editCoverPreview.value = ''
    alert('封面上传失败')
  } finally {
    editCoverUploading.value = false
  }
}

async function handleEdit() {
  if (!editForm.value.title.trim()) { editError.value = '歌单名称不能为空'; return }
  editing.value = true
  editError.value = ''
  try {
    const res = await request.put(`/playlists/${route.params.id}`, {
      title: editForm.value.title,
      description: editForm.value.description,
      coverUrl: editCoverUrl.value || null,
      isPublic: editForm.value.isPublic
    })
    if (res.code === 200) {
      playlist.value.title = editForm.value.title
      playlist.value.description = editForm.value.description
      playlist.value.coverUrl = editCoverUrl.value || playlist.value.coverUrl
      playlist.value.isPublic = editForm.value.isPublic
      showEditModal.value = false
    } else {
      editError.value = res.message || '保存失败'
    }
  } catch (e) {
    editError.value = e.message || '保存失败'
  } finally {
    editing.value = false
  }
}

function onCoverError(e) { e.target.src = defaultCover }

async function loadPlaylist() {
  loading.value = true
  try {
    const res = await request.get(`/playlists/${route.params.id}`)
    if (res.code === 200) {
      playlist.value = res.data
      songs.value = res.data.songs || []
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function playAll() {
  if (songs.value.length > 0) playback.replaceQueue(songs.value, 0)
}

function playSong(song, idx) {
  playback.replaceQueue(songs.value, idx)
}

async function removeSong(songId) {
  try {
    await request.delete(`/playlists/${route.params.id}/songs/${songId}`)
    songs.value = songs.value.filter(s => s.id !== songId)
    if (playlist.value) playlist.value.songCount = songs.value.length
  } catch { alert('移除失败') }
}

async function handleDelete() {
  if (!confirm('确定删除此歌单？')) return
  try {
    await request.delete(`/playlists/${route.params.id}`)
    router.push('/playlists')
  } catch { alert('删除失败') }
}

function formatDuration(sec) {
  if (!sec) return '--:--'
  return `${Math.floor(sec / 60)}:${(sec % 60).toString().padStart(2, '0')}`
}

onMounted(loadPlaylist)
</script>

<style scoped>
.detail-page { min-height: 100vh; background: #f5f5f5; }
.main { max-width: 800px; margin: 0 auto; padding: 24px 20px; }

.pl-hero { display: flex; gap: 24px; background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 24px; }
.hero-cover img { width: 180px; height: 180px; border-radius: 12px; object-fit: cover; }
.hero-info { flex: 1; }
.hero-info h2 { font-size: 22px; margin-bottom: 8px; }
.hero-meta { font-size: 13px; color: #999; margin-bottom: 12px; }
.hero-desc { font-size: 14px; color: #666; margin-bottom: 20px; }
.hero-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.btn-primary { padding: 10px 24px; background: #1565C0; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }
.btn-primary:hover { background: #0D47A1; }
.btn-edit { padding: 10px 24px; background: #fff; color: #1565C0; border: 1px solid #1565C0; border-radius: 8px; font-size: 14px; cursor: pointer; }
.btn-edit:hover { background: #E3F2FD; }
.btn-danger { padding: 10px 24px; background: #fff; color: #E53935; border: 1px solid #E53935; border-radius: 8px; font-size: 14px; cursor: pointer; }
.btn-danger:hover { background: #FFEBEE; }
.badge-private { display: inline-block; background: #FF9800; color: #fff; font-size: 11px; padding: 1px 8px; border-radius: 10px; margin-left: 6px; vertical-align: middle; }

.song-section, .section { background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 24px; }
.song-section h3, .section h3 { font-size: 16px; margin-bottom: 16px; }

.song-row {
  display: flex; align-items: center; gap: 12px; padding: 10px 0;
  border-bottom: 1px solid #f5f5f5; cursor: pointer;
}
.song-row:hover { background: #f8f9fa; }
.row-idx { width: 24px; font-size: 12px; color: #999; text-align: center; }
.row-cover img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; }
.row-info { flex: 1; min-width: 0; }
.row-title { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
.row-title:hover { color: #1565C0; }
.row-artist { font-size: 12px; color: #999; }
.row-duration { font-size: 12px; color: #999; width: 50px; text-align: right; }
.play-btn, .remove-btn { background: none; border: none; font-size: 14px; cursor: pointer; opacity: 0; padding: 4px 8px; }
.song-row:hover .play-btn, .song-row:hover .remove-btn { opacity: 1; }
.remove-btn { color: #E53935; }
.empty-hint { text-align: center; color: #ccc; padding: 40px; }
.loading { text-align: center; color: #999; padding: 60px; }

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
.btn-confirm:disabled { background: #90CAF9; cursor: not-allowed; }
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
