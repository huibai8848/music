<template>
  <div class="notif-page">
    <main class="main">
      <div class="page-header">
        <h2>🔔 通知</h2>
        <button v-if="notifications.length > 0" class="btn-mark-all" @click="markAllRead">全部已读</button>
      </div>

      <!-- 通知统计 -->
      <div class="stats-bar">
        <span>共 {{ total }} 条通知</span>
        <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }} 条未读</span>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading">加载中...</div>

      <!-- 空状态 -->
      <div v-else-if="notifications.length === 0" class="empty">
        <p>暂无通知</p>
      </div>

      <!-- 通知列表 -->
      <div v-else class="notif-list">
        <div
          v-for="n in notifications"
          :key="n.id"
          class="notif-card"
          :class="{ unread: !n.isRead }"
          @click="markRead(n.id)"
        >
          <div class="notif-dot" v-if="!n.isRead"></div>
          <div class="notif-body">
            <div class="notif-title">{{ n.title || '系统通知' }}</div>
            <div class="notif-content">{{ n.content }}</div>
            <div class="notif-time">{{ formatTime(n.createdTime) }}</div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="page <= 1" @click="page--; loadNotifications()">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button :disabled="page >= totalPages" @click="page++; loadNotifications()">下一页</button>
      </div>
    </main>
  </div>
</template>

<script setup>
/**
 * 通知列表页面
 *
 * 展示用户的所有通知，支持：
 * - 分页浏览
 * - 单条标记已读
 * - 全部标记已读
 * - 未读计数显示
 */
import { ref, onMounted } from 'vue'
import request from '../utils/request'

const notifications = ref([])
const unreadCount = ref(0)
const loading = ref(true)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const totalPages = ref(1)

onMounted(() => {
  loadNotifications()
  fetchUnreadCount()
})

async function loadNotifications() {
  loading.value = true
  try {
    const res = await request.get('/notifications', { params: { page: page.value, size: size.value } })
    if (res.code === 200) {
      notifications.value = res.data?.records || []
      total.value = res.data?.total || 0
      totalPages.value = Math.ceil(total.value / size.value) || 1
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function fetchUnreadCount() {
  try {
    const res = await request.get('/notifications/unread-count')
    if (res.code === 200) unreadCount.value = res.data || 0
  } catch { /* ignore */ }
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
</script>

<style scoped>
.notif-page { min-height: 100vh; background: #f5f5f5; }
.main { max-width: 700px; margin: 0 auto; padding: 24px 20px; }

.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;
}
.page-header h2 { font-size: 22px; color: #333; margin: 0; }
.btn-mark-all {
  padding: 6px 16px; background: #1565C0; color: #fff; border: none;
  border-radius: 6px; font-size: 13px; cursor: pointer;
}
.btn-mark-all:hover { background: #0D47A1; }

.stats-bar {
  display: flex; gap: 12px; align-items: center; font-size: 13px; color: #999; margin-bottom: 16px;
}
.unread-badge { color: #E53935; font-weight: 600; }

.loading, .empty { text-align: center; color: #999; padding: 60px; font-size: 14px; }

.notif-list { display: flex; flex-direction: column; gap: 8px; }

.notif-card {
  display: flex; gap: 12px; align-items: flex-start;
  background: #fff; border-radius: 10px; padding: 16px;
  cursor: pointer; transition: background 0.2s; border-left: 3px solid transparent;
}
.notif-card:hover { background: #f8f9ff; }
.notif-card.unread { border-left-color: #1565C0; background: #E3F2FD; }
.notif-card.unread:hover { background: #BBDEFB; }

.notif-dot {
  width: 8px; height: 8px; border-radius: 50%; background: #1565C0;
  flex-shrink: 0; margin-top: 6px;
}

.notif-body { flex: 1; min-width: 0; }
.notif-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 4px; }
.notif-content { font-size: 13px; color: #666; line-height: 1.5; margin-bottom: 6px; }
.notif-time { font-size: 12px; color: #999; }

.pagination {
  display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px;
}
.pagination button {
  padding: 6px 16px; border: 1px solid #ddd; border-radius: 6px;
  background: #fff; font-size: 13px; cursor: pointer;
}
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #666; }
</style>
