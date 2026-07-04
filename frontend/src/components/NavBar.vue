<template>
  <nav class="navbar">
    <div class="navbar-inner">
      <!-- Logo -->
      <router-link to="/" class="nav-logo">🎵 在线音乐</router-link>

      <!-- 导航链接 -->
      <div class="nav-links">
        <router-link to="/songs" class="nav-link">歌曲</router-link>
        <router-link to="/albums" class="nav-link">专辑</router-link>
        <router-link to="/artists" class="nav-link">艺人</router-link>
        <router-link to="/playlists" class="nav-link">歌单</router-link>
        <router-link to="/rooms" class="nav-link">歌房</router-link>
        <router-link to="/rankings" class="nav-link">榜单</router-link>
        <router-link v-if="authStore.isLoggedIn" to="/upload-music" class="nav-link">上传音乐</router-link>
      </div>

      <!-- 搜索框 -->
      <div class="nav-search">
        <input
          v-model="searchKeyword"
          @keyup.enter="goSearch"
          placeholder="搜索..."
          class="nav-search-input"
        />
      </div>

      <!-- 右侧：通知 + 用户 -->
      <div class="nav-right">
        <!-- 通知铃铛 -->
        <button v-if="authStore.isLoggedIn" class="nav-icon-btn" @click="showNotifications = !showNotifications" title="通知">
          🔔
          <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </button>

        <!-- 用户菜单 -->
        <template v-if="authStore.isLoggedIn">
          <router-link to="/profile" class="nav-user" :title="authStore.user?.nickname">
            <span class="user-avatar">
              <img v-if="authStore.user?.avatar" :src="authStore.user.avatar" class="avatar-img" @error="onAvatarError" />
              <span v-else>{{ (authStore.user?.nickname || 'U').charAt(0).toUpperCase() }}</span>
            </span>
            <span class="user-name">{{ authStore.user?.nickname || '用户' }}</span>
          </router-link>
          <button v-if="authStore.isAdmin" class="nav-admin-btn" @click="goAdmin">⚙️ 管理</button>
          <button class="nav-logout-btn" @click="handleLogout">退出</button>
        </template>
        <template v-else>
          <router-link to="/login" class="nav-auth-btn">登录</router-link>
          <router-link to="/register" class="nav-auth-btn nav-register">注册</router-link>
        </template>
      </div>
    </div>

    <!-- 通知面板 -->
    <Transition name="fade">
      <div v-if="showNotifications" class="notification-panel" @click.stop>
        <div class="panel-header">
          <h3>通知</h3>
          <button v-if="notifications.length > 0" class="btn-text" @click="markAllRead">全部已读</button>
        </div>
        <div class="panel-body">
          <div v-if="loadingNotif" class="panel-loading">加载中...</div>
          <div v-else-if="notifications.length === 0" class="panel-empty">暂无通知</div>
          <div v-else>
            <div v-for="n in notifications" :key="n.id" class="notif-item"
                 :class="{ unread: !n.isRead }" @click="markRead(n.id)">
              <div class="notif-title">{{ n.title || '系统通知' }}</div>
              <div class="notif-content">{{ n.content }}</div>
              <div class="notif-time">{{ formatTime(n.createdTime) }}</div>
            </div>
          </div>
        </div>
        <div class="panel-footer">
          <button class="btn-text" @click="loadNotifications">刷新</button>
          <router-link to="/notifications" class="btn-text" @click="showNotifications = false">查看全部</router-link>
        </div>
      </div>
    </Transition>
  </nav>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'

const router = useRouter()
const authStore = useAuthStore()

const showNotifications = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const loadingNotif = ref(false)
const searchKeyword = ref('')

function goSearch() {
  const q = searchKeyword.value.trim()
  if (q) {
    router.push({ path: '/search', query: { q } })
    searchKeyword.value = ''
  }
}

async function loadNotifications() {
  if (!authStore.isLoggedIn) return
  loadingNotif.value = true
  try {
    const [listRes, countRes] = await Promise.all([
      request.get('/notifications', { params: { page: 1, size: 20 } }),
      request.get('/notifications/unread-count')
    ])
    if (listRes.code === 200) notifications.value = listRes.data?.records || []
    if (countRes.code === 200) unreadCount.value = countRes.data || 0
  } catch { /* ignore */ }
  finally { loadingNotif.value = false }
}

async function markRead(id) {
  try {
    await request.put(`/notifications/${id}/read`)
    const n = notifications.value.find(x => x.id === id)
    if (n && !n.isRead) {
      n.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  } catch { /* ignore */ }
}

async function markAllRead() {
  try {
    await request.put('/notifications/read-all')
    notifications.value.forEach(n => { n.isRead = true })
    unreadCount.value = 0
  } catch { /* ignore */ }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function goAdmin() {
  router.push('/admin?tab=songs')
}

function onAvatarError(e) {
  e.target.style.display = 'none'
  e.target.parentElement.textContent = (authStore.user?.nickname || 'U').charAt(0).toUpperCase()
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = Math.floor((now - d) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  return d.toLocaleDateString('zh-CN')
}

// 点击外部关闭通知面板
function onClickOutside(e) {
  if (showNotifications.value && !e.target.closest('.navbar')) {
    showNotifications.value = false
  }
}

onMounted(() => {
  loadNotifications()
  document.addEventListener('click', onClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
})
</script>

<style scoped>
.navbar {
  background: #1565C0;
  color: #fff;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.navbar-inner {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 24px;
  max-width: 1400px;
  margin: 0 auto;
  gap: 8px;
}

.nav-logo {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  text-decoration: none;
  margin-right: 16px;
  white-space: nowrap;
}

.nav-links {
  display: flex;
  gap: 4px;
  flex: 1;
}

/* 导航栏搜索框 */
.nav-search { margin: 0 12px; }
.nav-search-input {
  width: 140px; padding: 5px 12px; border: none; border-radius: 6px;
  font-size: 13px; outline: none; background: rgba(255,255,255,0.2);
  color: #fff; transition: all 0.2s;
}
.nav-search-input::placeholder { color: rgba(255,255,255,0.6); }
.nav-search-input:focus { background: rgba(255,255,255,0.3); width: 180px; }

.nav-link {
  color: rgba(255,255,255,0.85);
  text-decoration: none;
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s;
  white-space: nowrap;
}

.nav-link:hover,
.nav-link.router-link-active {
  background: rgba(255,255,255,0.15);
  color: #fff;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.nav-icon-btn {
  background: none;
  border: none;
  color: rgba(255,255,255,0.9);
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  position: relative;
  line-height: 1;
}

.nav-icon-btn:hover {
  background: rgba(255,255,255,0.15);
}

.badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background: #E53935;
  color: #fff;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  font-weight: 600;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255,255,255,0.9);
  text-decoration: none;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 13px;
}

.nav-user:hover {
  background: rgba(255,255,255,0.15);
}

.user-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  overflow: hidden;
}

.user-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-admin-btn,
.nav-logout-btn,
.nav-auth-btn {
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.3);
  color: rgba(255,255,255,0.9);
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  text-decoration: none;
  white-space: nowrap;
}

.nav-admin-btn:hover,
.nav-logout-btn:hover,
.nav-auth-btn:hover {
  background: rgba(255,255,255,0.25);
}

.nav-register {
  background: #fff;
  color: #1565C0;
  border: none;
  font-weight: 600;
}

.nav-register:hover {
  background: #f0f0f0;
}

/* 通知面板 */
.notification-panel {
  position: absolute;
  top: 52px;
  right: 80px;
  width: 360px;
  max-height: 480px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
  color: #333;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #eee;
}

.panel-header h3 {
  margin: 0;
  font-size: 15px;
}

.btn-text {
  background: none;
  border: none;
  color: #1565C0;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 6px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  min-height: 100px;
}

.panel-loading,
.panel-empty {
  text-align: center;
  color: #999;
  padding: 40px 16px;
  font-size: 13px;
}

.notif-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;
}

.notif-item:hover {
  background: #f8f9ff;
}

.notif-item.unread {
  background: #E3F2FD;
}

.notif-item.unread:hover {
  background: #BBDEFB;
}

.notif-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
}

.notif-content {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
  line-height: 1.4;
}

.notif-time {
  font-size: 11px;
  color: #999;
}

.panel-footer {
  padding: 8px 16px;
  border-top: 1px solid #eee;
  text-align: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
