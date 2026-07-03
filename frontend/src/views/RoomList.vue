<template>
  <div class="room-list">
    <div class="page-header">
      <h1>🎵 歌房</h1>
      <button class="btn-create" @click="showCreate = true" v-if="authStore.isLoggedIn">
        + 创建歌房
      </button>
    </div>

    <!-- 创建歌房弹窗 -->
    <div class="modal-overlay" v-if="showCreate" @click.self="showCreate = false">
      <div class="modal">
        <h2>创建歌房</h2>
        <div class="form-group">
          <label>房间名称</label>
          <input v-model="createForm.name" placeholder="输入房间名称" maxlength="50" />
        </div>
        <div class="form-group">
          <label>
            <input type="checkbox" v-model="createForm.isPublic" />
            公开房间
          </label>
        </div>
        <div class="form-group" v-if="!createForm.isPublic">
          <label>房间密码</label>
          <input v-model="createForm.password" type="password" placeholder="设置密码" maxlength="20" />
        </div>
        <div class="form-group">
          <label>最大人数</label>
          <select v-model.number="createForm.maxMembers">
            <option :value="2">2 人</option>
            <option :value="4">4 人</option>
            <option :value="8" selected>8 人</option>
            <option :value="16">16 人</option>
          </select>
        </div>
        <div class="modal-actions">
          <button class="btn" @click="showCreate = false">取消</button>
          <button class="btn btn-primary" @click="handleCreate">创建</button>
        </div>
        <p class="error" v-if="createError">{{ createError }}</p>
      </div>
    </div>

    <!-- 房间列表 -->
    <div class="loading" v-if="loading">加载中...</div>
    <div class="empty" v-else-if="rooms.length === 0">
      <p>暂无公开歌房</p>
      <p class="hint">创建一个歌房，和朋友一起听歌吧！</p>
    </div>
    <div class="room-grid" v-else>
      <div class="room-card" v-for="room in rooms" :key="room.id" @click="enterRoom(room)">
        <div class="room-status" :class="room.status?.toLowerCase()">
          {{ statusText(room.status) }}
        </div>
        <h3>{{ room.name }}</h3>
        <div class="room-info">
          <span>👤 {{ room.memberCount || 0 }}/{{ room.maxMembers || 8 }}</span>
          <span>👑 {{ room.ownerNickname || '未知' }}</span>
          <span v-if="room.currentSongTitle">🎵 {{ room.currentSongTitle }}</span>
        </div>
        <div class="room-meta">
          <span v-if="room.hasPassword" class="tag-private">🔒 私密</span>
          <span v-else class="tag-public">🌐 公开</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const rooms = ref([])
const loading = ref(true)

// 创建表单
const showCreate = ref(false)
const createError = ref('')
const createForm = ref({
  name: '',
  isPublic: true,
  password: '',
  maxMembers: 8
})

onMounted(() => {
  fetchRooms()
})

async function fetchRooms() {
  loading.value = true
  try {
    const res = await request.get('/rooms')
    if (res.code === 200 && res.data) {
      rooms.value = res.data
    }
  } catch (e) {
    console.error('获取房间列表失败', e)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.name.trim()) {
    createError.value = '请输入房间名称'
    return
  }
  createError.value = ''
  try {
    const body = {
      name: createForm.value.name.trim(),
      isPublic: createForm.value.isPublic,
      maxMembers: createForm.value.maxMembers
    }
    if (!createForm.value.isPublic) {
      body.password = createForm.value.password
    }
    const res = await request.post('/rooms', body)
    if (res.code === 200 && res.data) {
      showCreate.value = false
      createForm.value = { name: '', isPublic: true, password: '', maxMembers: 8 }
      // 进入新创建的房间
      router.push(`/rooms/${res.data.id}`)
    }
  } catch (e) {
    createError.value = e.message || '创建失败'
  }
}

function enterRoom(room) {
  router.push(`/rooms/${room.id}`)
}

function statusText(status) {
  const map = { WAITING: '等待中', PLAYING: '播放中', PAUSED: '已暂停' }
  return map[status] || status || '等待中'
}
</script>

<style scoped>
.room-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
}

.btn-create {
  background: #1db954;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  font-weight: 600;
}

.btn-create:hover {
  background: #169c46;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.room-card {
  background: #1e1e1e;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
  border: 1px solid #333;
}

.room-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
  border-color: #1db954;
}

.room-status {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 600;
}

.room-status.playing { background: #1db954; color: #fff; }
.room-status.paused { background: #f59e0b; color: #fff; }
.room-status.waiting { background: #6b7280; color: #fff; }

.room-card h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #fff;
}

.room-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #999;
  margin-bottom: 12px;
}

.room-meta {
  display: flex;
  gap: 8px;
}

.tag-private {
  background: #7c3aed;
  color: #fff;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
}

.tag-public {
  background: #065f46;
  color: #6ee7b7;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
}

.loading, .empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.hint {
  font-size: 14px;
  margin-top: 8px;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal {
  background: #1e1e1e;
  border-radius: 16px;
  padding: 32px;
  width: 400px;
  max-width: 90vw;
  border: 1px solid #333;
}

.modal h2 {
  margin: 0 0 20px 0;
  color: #fff;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #ccc;
  font-size: 14px;
}

.form-group input[type="text"],
.form-group input[type="password"],
.form-group select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #444;
  border-radius: 8px;
  background: #2a2a2a;
  color: #fff;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input[type="checkbox"] {
  margin-right: 8px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn {
  padding: 10px 24px;
  border-radius: 20px;
  border: 1px solid #444;
  background: transparent;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background: #1db954;
  border: none;
  font-weight: 600;
}

.btn-primary:hover {
  background: #169c46;
}

.error {
  color: #ef4444;
  font-size: 13px;
  margin-top: 8px;
  text-align: center;
}
</style>
