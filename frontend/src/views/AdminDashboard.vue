<template>
  <div class="admin-page">
    <!-- 顶部导航 -->
    <div class="admin-header">
      <h1>⚙️ 管理后台</h1>
      <div class="admin-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key; switchTab()"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <div class="admin-body">
      <!-- ==================== 1. 数据看板 ==================== -->
      <div v-if="activeTab === 'dashboard'" class="tab-content">
        <div v-if="loading.dashboard" class="loading">加载中...</div>
        <div v-else>
          <!-- 统计数字卡片 -->
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-value">{{ dash.totalUsers }}</div>
              <div class="stat-label">用户总数</div>
            </div>
            <div class="stat-card vip">
              <div class="stat-value">{{ dash.totalVipUsers }}</div>
              <div class="stat-label">会员数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ dash.totalSongs }}</div>
              <div class="stat-label">歌曲总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ dash.totalAlbums }}</div>
              <div class="stat-label">专辑总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ dash.totalArtists }}</div>
              <div class="stat-label">艺人总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ dash.totalPlaylists }}</div>
              <div class="stat-label">歌单总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ dash.totalComments }}</div>
              <div class="stat-label">评论总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ dash.totalPlayCount }}</div>
              <div class="stat-label">总播放量</div>
            </div>
            <div class="stat-card highlight">
              <div class="stat-value">{{ dash.todayNewUsers }}</div>
              <div class="stat-label">今日新增用户</div>
            </div>
            <div class="stat-card warn">
              <div class="stat-value">{{ dash.pendingReports }}</div>
              <div class="stat-label">待处理举报</div>
            </div>
            <div class="stat-card warn clickable" @click="switchToTab('songs', 'PENDING')">
              <div class="stat-value">{{ dash.pendingSongs }}</div>
              <div class="stat-label">待审核歌曲 →</div>
            </div>
          </div>

          <!-- 快捷入口 -->
          <div class="quick-actions">
            <button class="quick-btn" @click="switchToTab('songs')">🎵 歌曲管理</button>
            <button class="quick-btn" @click="switchToTab('users')">👥 用户管理</button>
            <button class="quick-btn" @click="switchToTab('albums')">💿 专辑管理</button>
            <button class="quick-btn" @click="switchToTab('reports')">🚨 举报管理</button>
            <button class="quick-btn" @click="switchToTab('banners')">🖼️ 轮播图</button>
            <button class="quick-btn" @click="switchToTab('notices')">📢 公告</button>
          </div>

          <!-- 图表区域 -->
          <div class="charts-row">
            <div class="chart-card">
              <h3 class="chart-title">📈 近 7 天新增用户</h3>
              <div class="chart-wrapper">
                <canvas ref="userChartRef"></canvas>
              </div>
            </div>
            <div class="chart-card">
              <h3 class="chart-title">📊 歌曲风格分布</h3>
              <div class="chart-wrapper">
                <canvas ref="genreChartRef"></canvas>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== 2. 用户管理 ==================== -->
      <div v-if="activeTab === 'users'" class="tab-content">
        <div class="toolbar">
          <input v-model="userKeyword" placeholder="搜索昵称/邮箱..." class="search-input" @keyup.enter="loadUsers(1)" />
          <button class="btn-primary" @click="loadUsers(1)">搜索</button>
        </div>
        <div v-if="loading.users" class="loading">加载中...</div>
        <table v-else class="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>昵称</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>状态</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in userList" :key="u.id">
              <td>{{ u.id }}</td>
              <td>{{ u.nickname || '-' }}</td>
              <td>{{ u.email || '-' }}</td>
              <td>{{ u.role === 'ADMIN' ? '管理员' : u.role === 'VIP' ? '会员' : '普通用户' }}</td>
              <td>
                <span :class="u.banned ? 'tag-danger' : 'tag-success'">{{ u.banned ? '已封禁' : '正常' }}</span>
              </td>
              <td>{{ formatTime(u.createdTime) }}</td>
              <td>
                <button v-if="!u.banned" class="btn-danger-sm" @click="banUser(u.id, true)">封禁</button>
                <button v-else class="btn-success-sm" @click="banUser(u.id, false)">解封</button>
              </td>
            </tr>
          </tbody>
        </table>
        <Pagination v-if="userPages > 1" :page="userPage" :total="userPages" @change="loadUsers" />
      </div>

      <!-- ==================== 3. 歌曲管理 ==================== -->
      <div v-if="activeTab === 'songs'" class="tab-content">
        <div class="toolbar">
          <select v-model="songStatusFilter" class="filter-select">
            <option value="">全部状态</option>
            <option value="ACTIVE">已通过</option>
            <option value="PENDING">待审核</option>
            <option value="REJECTED">已驳回</option>
          </select>
          <input v-model="songKeyword" placeholder="搜索歌曲名..." class="search-input" @keyup.enter="loadSongs(1)" />
          <button class="btn-primary" @click="loadSongs(1)">搜索</button>
        </div>
        <div v-if="loading.songs" class="loading">加载中...</div>
        <table v-else class="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>歌曲名</th>
              <th>艺人</th>
              <th>状态</th>
              <th>上传者</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in songList" :key="s.id">
              <td>{{ s.id }}</td>
              <td>{{ s.title }}</td>
              <td>{{ s.artistName || '-' }}</td>
              <td>
                <span :class="statusClass(s.status)">{{ statusLabel(s.status) }}</span>
              </td>
              <td>{{ s.uploaderNickname || '-' }}</td>
              <td class="action-cell">
                <button class="btn-sm" @click="$router.push('/songs/' + s.id)">查看</button>
                <button v-if="s.status === 'PENDING'" class="btn-success-sm" @click="auditSong(s, 'ACTIVE')">通过</button>
                <button v-if="s.status === 'PENDING'" class="btn-danger-sm" @click="showAuditReject(s)">驳回</button>
                <button class="btn-danger-sm" @click="deleteSong(s.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <Pagination v-if="songPages > 1" :page="songPage" :total="songPages" @change="loadSongs" />

        <!-- 驳回弹窗 -->
        <div v-if="showRejectModal" class="modal-overlay" @click.self="showRejectModal = false">
          <div class="modal-card">
            <h3>驳回歌曲</h3>
            <p class="modal-hint">歌曲：{{ rejectingSong?.title }}</p>
            <textarea v-model="rejectReason" placeholder="驳回原因..." class="modal-textarea" rows="3" maxlength="500"></textarea>
            <div class="modal-actions">
              <button class="btn-cancel" @click="showRejectModal = false">取消</button>
              <button class="btn-confirm danger" @click="confirmReject">确认驳回</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== 4. 专辑管理 ==================== -->
      <div v-if="activeTab === 'albums'" class="tab-content">
        <div v-if="loading.albums" class="loading">加载中...</div>
        <table v-else class="admin-table">
          <thead>
            <tr><th>ID</th><th>专辑名</th><th>艺人</th><th>歌曲数</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="a in albumList" :key="a.id">
              <td>{{ a.id }}</td>
              <td>{{ a.title }}</td>
              <td>{{ a.artistName || '-' }}</td>
              <td>{{ a.songCount || 0 }}</td>
              <td>
                <button class="btn-sm" @click="$router.push('/albums/' + a.id)">查看</button>
                <button class="btn-danger-sm" @click="deleteAlbum(a.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <Pagination v-if="albumPages > 1" :page="albumPage" :total="albumPages" @change="loadAlbums" />
      </div>

      <!-- ==================== 5. 艺人管理 ==================== -->
      <div v-if="activeTab === 'artists'" class="tab-content">
        <div v-if="loading.artists" class="loading">加载中...</div>
        <table v-else class="admin-table">
          <thead>
            <tr><th>ID</th><th>艺人名</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="a in artistList" :key="a.id">
              <td>{{ a.id }}</td>
              <td>{{ a.name }}</td>
              <td>
                <button class="btn-sm" @click="$router.push('/artists/' + a.id)">查看</button>
                <button class="btn-danger-sm" @click="deleteArtist(a.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <Pagination v-if="artistPages > 1" :page="artistPage" :total="artistPages" @change="loadArtists" />
      </div>

      <!-- ==================== 6. 评论管理 ==================== -->
      <div v-if="activeTab === 'comments'" class="tab-content">
        <div class="toolbar">
          <select v-model="commentTypeFilter" class="filter-select" @change="loadComments(1)">
            <option value="">全部类型</option>
            <option value="SONG">歌曲评论</option>
            <option value="PLAYLIST">歌单评论</option>
          </select>
        </div>
        <div v-if="loading.comments" class="loading">加载中...</div>
        <table v-else class="admin-table">
          <thead>
            <tr><th>ID</th><th>用户</th><th>内容</th><th>类型</th><th>时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="c in commentList" :key="c.id">
              <td>{{ c.id }}</td>
              <td>{{ c.nickname || '匿名' }}</td>
              <td class="cell-content">{{ (c.content || '').substring(0, 50) }}</td>
              <td>{{ c.targetType }}</td>
              <td>{{ formatTime(c.createdTime) }}</td>
              <td><button class="btn-danger-sm" @click="deleteComment(c.id)">删除</button></td>
            </tr>
          </tbody>
        </table>
        <Pagination v-if="commentPages > 1" :page="commentPage" :total="commentPages" @change="loadComments" />
      </div>

      <!-- ==================== 7. 举报管理 ==================== -->
      <div v-if="activeTab === 'reports'" class="tab-content">
        <div class="toolbar">
          <select v-model="reportStatusFilter" class="filter-select" @change="loadReports(1)">
            <option value="">全部状态</option>
            <option value="PENDING">待处理</option>
            <option value="RESOLVED">已处理</option>
            <option value="DISMISSED">已驳回</option>
          </select>
        </div>
        <div v-if="loading.reports" class="loading">加载中...</div>
        <table v-else class="admin-table">
          <thead>
            <tr><th>ID</th><th>举报人</th><th>类型</th><th>原因</th><th>说明</th><th>状态</th><th>时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="r in reportList" :key="r.id">
              <td>{{ r.id }}</td>
              <td>{{ r.reporterNickname || '-' }}</td>
              <td>{{ r.targetType }}</td>
              <td>{{ reasonLabel(r.reason) }}</td>
              <td class="cell-content">{{ (r.description || '').substring(0, 30) }}</td>
              <td>
                <span :class="statusClass(r.status)">{{ reportStatusLabel(r.status) }}</span>
              </td>
              <td>{{ formatTime(r.createdTime) }}</td>
              <td>
                <button v-if="r.status === 'PENDING'" class="btn-success-sm" @click="handleReport(r, 'RESOLVED')">处理</button>
                <button v-if="r.status === 'PENDING'" class="btn-warn-sm" @click="handleReport(r, 'DISMISSED')">驳回</button>
              </td>
            </tr>
          </tbody>
        </table>
        <Pagination v-if="reportPages > 1" :page="reportPage" :total="reportPages" @change="loadReports" />
      </div>

      <!-- ==================== 8. 轮播图管理 ==================== -->
      <div v-if="activeTab === 'banners'" class="tab-content">
        <div class="toolbar">
          <button class="btn-primary" @click="showBannerModal = true; bannerForm = { imageUrl: '', linkUrl: '', sortOrder: 0, isActive: true, title: '' }; bannerEditing = false">+ 新增轮播图</button>
        </div>
        <div v-if="loading.banners" class="loading">加载中...</div>
        <table v-else class="admin-table">
          <thead>
            <tr><th>ID</th><th>图片预览</th><th>标题</th><th>链接</th><th>排序</th><th>状态</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="b in bannerList" :key="b.id">
              <td>{{ b.id }}</td>
              <td><img :src="b.imageUrl" class="banner-preview" @error="onImgError" alt=""></td>
              <td>{{ b.title || '-' }}</td>
              <td class="cell-content">{{ b.linkUrl || '-' }}</td>
              <td>{{ b.sortOrder }}</td>
              <td>
                <span :class="b.isActive ? 'tag-success' : 'tag-muted'">{{ b.isActive ? '启用' : '禁用' }}</span>
              </td>
              <td>
                <button class="btn-sm" @click="editBanner(b)">编辑</button>
                <button class="btn-danger-sm" @click="deleteBanner(b.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 轮播图编辑弹窗 -->
        <div v-if="showBannerModal" class="modal-overlay" @click.self="showBannerModal = false">
          <div class="modal-card">
            <h3>{{ bannerEditing ? '编辑' : '新增' }}轮播图</h3>
            <input v-model="bannerForm.imageUrl" placeholder="图片 URL" class="modal-input" />
            <input v-model="bannerForm.title" placeholder="标题（可选）" class="modal-input" />
            <input v-model="bannerForm.linkUrl" placeholder="跳转链接（可选）" class="modal-input" />
            <input v-model.number="bannerForm.sortOrder" type="number" placeholder="排序序号" class="modal-input" />
            <label class="modal-checkbox">
              <input type="checkbox" v-model="bannerForm.isActive" /> 启用
            </label>
            <div class="modal-actions">
              <button class="btn-cancel" @click="showBannerModal = false">取消</button>
              <button class="btn-confirm" @click="saveBanner">{{ bannerEditing ? '保存' : '创建' }}</button>
            </div>
            <p v-if="bannerError" class="error">{{ bannerError }}</p>
          </div>
        </div>
      </div>

      <!-- ==================== 9. 公告管理 ==================== -->
      <div v-if="activeTab === 'notices'" class="tab-content">
        <div class="toolbar">
          <select v-model="noticeTypeFilter" class="filter-select" @change="loadNotices(1)">
            <option value="">全部类型</option>
            <option value="SYSTEM">系统公告</option>
            <option value="MAINTENANCE">维护通知</option>
            <option value="ACTIVITY">活动公告</option>
          </select>
          <button class="btn-primary" @click="showNoticeModal = true; noticeForm = { title: '', content: '', type: 'SYSTEM', isActive: true }; noticeEditing = false">+ 新增公告</button>
        </div>
        <div v-if="loading.notices" class="loading">加载中...</div>
        <table v-else class="admin-table">
          <thead>
            <tr><th>ID</th><th>标题</th><th>类型</th><th>内容</th><th>状态</th><th>时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="n in noticeList" :key="n.id">
              <td>{{ n.id }}</td>
              <td>{{ n.title }}</td>
              <td>{{ noticeTypeLabel(n.type) }}</td>
              <td class="cell-content">{{ (n.content || '').substring(0, 40) }}</td>
              <td>
                <span :class="n.isActive ? 'tag-success' : 'tag-muted'">{{ n.isActive ? '启用' : '禁用' }}</span>
              </td>
              <td>{{ formatTime(n.createdTime) }}</td>
              <td>
                <button class="btn-sm" @click="editNotice(n)">编辑</button>
                <button class="btn-danger-sm" @click="deleteNotice(n.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <Pagination v-if="noticePages > 1" :page="noticePage" :total="noticePages" @change="loadNotices" />

        <!-- 公告编辑弹窗 -->
        <div v-if="showNoticeModal" class="modal-overlay" @click.self="showNoticeModal = false">
          <div class="modal-card wide">
            <h3>{{ noticeEditing ? '编辑' : '新增' }}公告</h3>
            <input v-model="noticeForm.title" placeholder="公告标题" class="modal-input" maxlength="200" />
            <select v-model="noticeForm.type" class="modal-input">
              <option value="SYSTEM">系统公告</option>
              <option value="MAINTENANCE">维护通知</option>
              <option value="ACTIVITY">活动公告</option>
            </select>
            <textarea v-model="noticeForm.content" placeholder="公告内容" class="modal-textarea" rows="5" maxlength="5000"></textarea>
            <label class="modal-checkbox">
              <input type="checkbox" v-model="noticeForm.isActive" /> 启用
            </label>
            <div class="modal-actions">
              <button class="btn-cancel" @click="showNoticeModal = false">取消</button>
              <button class="btn-confirm" @click="saveNotice">{{ noticeEditing ? '保存' : '创建' }}</button>
            </div>
            <p v-if="noticeError" class="error">{{ noticeError }}</p>
          </div>
        </div>
      </div>

      <!-- ==================== 10. 操作日志 ==================== -->
      <div v-if="activeTab === 'logs'" class="tab-content">
        <div v-if="loading.logs" class="loading">加载中...</div>
        <table v-else class="admin-table">
          <thead>
            <tr><th>ID</th><th>操作人</th><th>操作类型</th><th>目标类型</th><th>目标 ID</th><th>详情</th><th>IP</th><th>时间</th></tr>
          </thead>
          <tbody>
            <tr v-for="l in logList" :key="l.id">
              <td>{{ l.id }}</td>
              <td>{{ l.operatorName || l.operatorId || '-' }}</td>
              <td>{{ l.action || '-' }}</td>
              <td>{{ l.targetType || '-' }}</td>
              <td>{{ l.targetId || '-' }}</td>
              <td class="cell-content">{{ (l.detail || '').substring(0, 50) }}</td>
              <td>{{ l.ip || '-' }}</td>
              <td>{{ formatTime(l.createdTime) }}</td>
            </tr>
          </tbody>
        </table>
        <Pagination v-if="logPages > 1" :page="logPage" :total="logPages" @change="loadLogs" />
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 管理后台页面
 *
 * 涵盖 10 个管理模块，对接 /api/admin/* 全部 24 个接口。
 * 需要管理员权限（路由守卫 + API 端拦截器双重校验）。
 */
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import Pagination from '../components/Pagination.vue'
import { Chart, registerables } from 'chart.js'
Chart.register(...registerables)

const route = useRoute()
const router = useRouter()

const tabs = [
  { key: 'dashboard', label: '📊 数据看板' },
  { key: 'users', label: '👥 用户管理' },
  { key: 'songs', label: '🎵 歌曲管理' },
  { key: 'albums', label: '💿 专辑管理' },
  { key: 'artists', label: '🎤 艺人管理' },
  { key: 'comments', label: '💬 评论管理' },
  { key: 'reports', label: '🚨 举报管理' },
  { key: 'banners', label: '🖼️ 轮播图管理' },
  { key: 'notices', label: '📢 公告管理' },
  { key: 'logs', label: '📋 操作日志' }
]

// 支持从 query param ?tab=songs 直接跳转到指定标签页
const activeTab = ref(route.query.tab || 'dashboard')

// 各模块加载状态
const loading = reactive({
  dashboard: false, users: false, songs: false, albums: false,
  artists: false, comments: false, reports: false, banners: false,
  notices: false, logs: false
})

// ====== 1. 数据看板 ======
const dash = reactive({
  totalUsers: 0, totalVipUsers: 0, totalSongs: 0, totalAlbums: 0,
  totalArtists: 0, totalPlaylists: 0, totalComments: 0, todayNewUsers: 0,
  pendingReports: 0, pendingSongs: 0, totalPlayCount: 0,
  last7DaysLabels: [], last7DaysNewUsers: [], last7DaysPlayCount: [],
  genreDistribution: []
})

// Chart.js 实例引用
const userChartRef = ref(null)
const genreChartRef = ref(null)
let userChart = null
let genreChart = null

function renderCharts() {
  // 销毁旧图表
  if (userChart) { userChart.destroy(); userChart = null }
  if (genreChart) { genreChart.destroy(); genreChart = null }

  // 用户趋势图
  if (userChartRef.value && dash.last7DaysLabels?.length) {
    userChart = new Chart(userChartRef.value, {
      type: 'line',
      data: {
        labels: dash.last7DaysLabels,
        datasets: [{
          label: '新增用户',
          data: dash.last7DaysNewUsers,
          borderColor: '#1565C0',
          backgroundColor: 'rgba(21,101,192,0.1)',
          fill: true,
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: true, ticks: { stepSize: 1, precision: 0 } }
        }
      }
    })
  }

  // 风格分布饼图
  if (genreChartRef.value && dash.genreDistribution?.length) {
    const colors = ['#1565C0','#E91E63','#4CAF50','#FF9800','#9C27B0','#00BCD4','#F44336','#607D8B']
    genreChart = new Chart(genreChartRef.value, {
      type: 'doughnut',
      data: {
        labels: dash.genreDistribution.map(g => g.name || '其他'),
        datasets: [{
          data: dash.genreDistribution.map(g => g.count || 0),
          backgroundColor: colors.slice(0, dash.genreDistribution.length),
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'right', labels: { font: { size: 11 } } }
        }
      }
    })
  }
}

async function loadDashboard() {
  loading.dashboard = true
  try {
    const res = await request.get('/admin/dashboard')
    if (res.code === 200 && res.data) {
      Object.assign(dash, res.data)
      await nextTick()
      renderCharts()
    }
  } catch { /* ignore */ }
  finally { loading.dashboard = false }
}

// ====== 2. 用户管理 ======
const userList = ref([])
const userPage = ref(1)
const userPages = ref(1)
const userKeyword = ref('')

async function loadUsers(page) {
  userPage.value = page || 1
  loading.users = true
  try {
    const res = await request.get('/admin/users', {
      params: { keyword: userKeyword.value, page: userPage.value, size: 20 }
    })
    if (res.code === 200) {
      userList.value = res.data?.records || []
      userPages.value = Math.ceil((res.data?.total || 0) / 20) || 1
    }
  } catch { /* ignore */ }
  finally { loading.users = false }
}

async function banUser(id, banned) {
  if (!confirm(banned ? '确定封禁此用户？' : '确定解封此用户？')) return
  try {
    await request.put(`/admin/users/${id}/ban`, null, { params: { banned } })
    loadUsers(userPage.value)
  } catch { alert('操作失败') }
}

// ====== 3. 歌曲管理 ======
const songList = ref([])
const songPage = ref(1)
const songPages = ref(1)
const songStatusFilter = ref('')
const songKeyword = ref('')
const showRejectModal = ref(false)
const rejectingSong = ref(null)
const rejectReason = ref('')

async function loadSongs(page) {
  songPage.value = page || 1
  loading.songs = true
  try {
    const params = { page: songPage.value, size: 20 }
    if (songStatusFilter.value) params.status = songStatusFilter.value
    if (songKeyword.value) params.keyword = songKeyword.value
    const res = await request.get('/admin/songs', { params })
    if (res.code === 200) {
      songList.value = res.data?.records || []
      songPages.value = Math.ceil((res.data?.total || 0) / 20) || 1
    }
  } catch { /* ignore */ }
  finally { loading.songs = false }
}

async function auditSong(song, status) {
  try {
    await request.put(`/admin/songs/${song.id}/audit`, {
      status,
      rejectReason: status === 'REJECTED' ? rejectReason.value : ''
    })
    loadSongs(songPage.value)
  } catch { alert('操作失败') }
}

function showAuditReject(song) {
  rejectingSong.value = song
  rejectReason.value = ''
  showRejectModal.value = true
}

async function confirmReject() {
  await auditSong(rejectingSong.value, 'REJECTED')
  showRejectModal.value = false
}

async function deleteSong(id) {
  if (!confirm('确定删除此歌曲？')) return
  try {
    await request.delete(`/admin/songs/${id}`)
    loadSongs(songPage.value)
  } catch { alert('删除失败') }
}

// ====== 4. 专辑管理 ======
const albumList = ref([])
const albumPage = ref(1)
const albumPages = ref(1)

async function loadAlbums(page) {
  albumPage.value = page || 1
  loading.albums = true
  try {
    const res = await request.get('/admin/albums', { params: { page: albumPage.value, size: 20 } })
    if (res.code === 200) {
      albumList.value = res.data?.records || []
      albumPages.value = Math.ceil((res.data?.total || 0) / 20) || 1
    }
  } catch { /* ignore */ }
  finally { loading.albums = false }
}

async function deleteAlbum(id) {
  if (!confirm('确定删除此专辑？')) return
  try { await request.delete(`/admin/albums/${id}`); loadAlbums(albumPage.value) }
  catch { alert('删除失败') }
}

// ====== 5. 艺人管理 ======
const artistList = ref([])
const artistPage = ref(1)
const artistPages = ref(1)

async function loadArtists(page) {
  artistPage.value = page || 1
  loading.artists = true
  try {
    const res = await request.get('/admin/artists', { params: { page: artistPage.value, size: 20 } })
    if (res.code === 200) {
      artistList.value = res.data?.records || []
      artistPages.value = Math.ceil((res.data?.total || 0) / 20) || 1
    }
  } catch { /* ignore */ }
  finally { loading.artists = false }
}

async function deleteArtist(id) {
  if (!confirm('确定删除此艺人？')) return
  try { await request.delete(`/admin/artists/${id}`); loadArtists(artistPage.value) }
  catch { alert('删除失败') }
}

// ====== 6. 评论管理 ======
const commentList = ref([])
const commentPage = ref(1)
const commentPages = ref(1)
const commentTypeFilter = ref('')

async function loadComments(page) {
  commentPage.value = page || 1
  loading.comments = true
  try {
    const params = { page: commentPage.value, size: 20 }
    if (commentTypeFilter.value) params.targetType = commentTypeFilter.value
    const res = await request.get('/admin/comments', { params })
    if (res.code === 200) {
      commentList.value = res.data?.records || []
      commentPages.value = Math.ceil((res.data?.total || 0) / 20) || 1
    }
  } catch { /* ignore */ }
  finally { loading.comments = false }
}

async function deleteComment(id) {
  if (!confirm('确定删除此评论？')) return
  try { await request.delete(`/admin/comments/${id}`); loadComments(commentPage.value) }
  catch { alert('删除失败') }
}

// ====== 7. 举报管理 ======
const reportList = ref([])
const reportPage = ref(1)
const reportPages = ref(1)
const reportStatusFilter = ref('')

async function loadReports(page) {
  reportPage.value = page || 1
  loading.reports = true
  try {
    const params = { page: reportPage.value, size: 20 }
    if (reportStatusFilter.value) params.status = reportStatusFilter.value
    const res = await request.get('/admin/reports', { params })
    if (res.code === 200) {
      reportList.value = res.data?.records || []
      reportPages.value = Math.ceil((res.data?.total || 0) / 20) || 1
    }
  } catch { /* ignore */ }
  finally { loading.reports = false }
}

async function handleReport(report, status) {
  if (!confirm(`确定${status === 'RESOLVED' ? '处理' : '驳回'}此举报？`)) return
  try {
    await request.put(`/admin/reports/${report.id}`, { status, handleNote: '' })
    loadReports(reportPage.value)
  } catch { alert('操作失败') }
}

// ====== 8. 轮播图管理 ======
const bannerList = ref([])
const showBannerModal = ref(false)
const bannerEditing = ref(false)
const bannerForm = reactive({ imageUrl: '', linkUrl: '', sortOrder: 0, isActive: true, title: '' })
const bannerError = ref('')
let editingBannerId = null

async function loadBanners() {
  loading.banners = true
  try {
    const res = await request.get('/admin/banners')
    if (res.code === 200) bannerList.value = res.data || []
  } catch { /* ignore */ }
  finally { loading.banners = false }
}

function editBanner(banner) {
  editingBannerId = banner.id
  bannerEditing.value = true
  Object.assign(bannerForm, {
    imageUrl: banner.imageUrl || '',
    title: banner.title || '',
    linkUrl: banner.linkUrl || '',
    sortOrder: banner.sortOrder || 0,
    isActive: banner.isActive !== false
  })
  showBannerModal.value = true
  bannerError.value = ''
}

async function saveBanner() {
  if (!bannerForm.imageUrl.trim()) { bannerError.value = '图片 URL 不能为空'; return }
  bannerError.value = ''
  try {
    const body = { ...bannerForm }
    if (bannerEditing.value) {
      await request.put(`/admin/banners/${editingBannerId}`, body)
    } else {
      await request.post('/admin/banners', body)
    }
    showBannerModal.value = false
    loadBanners()
  } catch (e) { bannerError.value = e.message || '保存失败' }
}

async function deleteBanner(id) {
  if (!confirm('确定删除此轮播图？')) return
  try { await request.delete(`/admin/banners/${id}`); loadBanners() }
  catch { alert('删除失败') }
}

// ====== 9. 公告管理 ======
const noticeList = ref([])
const noticePage = ref(1)
const noticePages = ref(1)
const noticeTypeFilter = ref('')
const showNoticeModal = ref(false)
const noticeEditing = ref(false)
const noticeForm = reactive({ title: '', content: '', type: 'SYSTEM', isActive: true })
const noticeError = ref('')
let editingNoticeId = null

async function loadNotices(page) {
  noticePage.value = page || 1
  loading.notices = true
  try {
    const params = { page: noticePage.value, size: 20 }
    if (noticeTypeFilter.value) params.type = noticeTypeFilter.value
    const res = await request.get('/admin/notices', { params })
    if (res.code === 200) {
      noticeList.value = res.data?.records || []
      noticePages.value = Math.ceil((res.data?.total || 0) / 20) || 1
    }
  } catch { /* ignore */ }
  finally { loading.notices = false }
}

function editNotice(notice) {
  editingNoticeId = notice.id
  noticeEditing.value = true
  Object.assign(noticeForm, {
    title: notice.title || '',
    content: notice.content || '',
    type: notice.type || 'SYSTEM',
    isActive: notice.isActive !== false
  })
  showNoticeModal.value = true
  noticeError.value = ''
}

async function saveNotice() {
  if (!noticeForm.title.trim()) { noticeError.value = '公告标题不能为空'; return }
  if (!noticeForm.content.trim()) { noticeError.value = '公告内容不能为空'; return }
  noticeError.value = ''
  try {
    const body = { ...noticeForm }
    if (noticeEditing.value) {
      await request.put(`/admin/notices/${editingNoticeId}`, body)
    } else {
      await request.post('/admin/notices', body)
    }
    showNoticeModal.value = false
    loadNotices(noticePage.value)
  } catch (e) { noticeError.value = e.message || '保存失败' }
}

async function deleteNotice(id) {
  if (!confirm('确定删除此公告？')) return
  try { await request.delete(`/admin/notices/${id}`); loadNotices(noticePage.value) }
  catch { alert('删除失败') }
}

// ====== 10. 操作日志 ======
const logList = ref([])
const logPage = ref(1)
const logPages = ref(1)

async function loadLogs(page) {
  logPage.value = page || 1
  loading.logs = true
  try {
    const res = await request.get('/admin/logs', { params: { page: logPage.value, size: 20 } })
    if (res.code === 200) {
      logList.value = res.data?.records || []
      logPages.value = Math.ceil((res.data?.total || 0) / 20) || 1
    }
  } catch { /* ignore */ }
  finally { loading.logs = false }
}

// ====== 工具函数 ======
function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

function statusLabel(s) {
  const map = { ACTIVE: '已通过', PENDING: '待审核', REJECTED: '已驳回' }
  return map[s] || s || '-'
}

function statusClass(s) {
  const map = { ACTIVE: 'tag-success', PENDING: 'tag-warn', REJECTED: 'tag-danger' }
  return map[s] || 'tag-muted'
}

function reasonLabel(r) {
  const map = { PORNOGRAPHY: '色情', AD: '广告', ABUSE: '人身攻击', COPYRIGHT: '侵权', OTHER: '其他' }
  return map[r] || r || '-'
}

function reportStatusLabel(s) {
  const map = { PENDING: '待处理', RESOLVED: '已处理', DISMISSED: '已驳回' }
  return map[s] || s || '-'
}

function noticeTypeLabel(t) {
  const map = { SYSTEM: '系统公告', MAINTENANCE: '维护通知', ACTIVITY: '活动公告' }
  return map[t] || t || '-'
}

function onImgError(e) { e.target.style.display = 'none' }

// ====== Tab 切换时按需加载 ======
function switchTab() {
  // 更新 URL 查询参数，不刷新页面
  router.replace({ query: { ...route.query, tab: activeTab.value } })
  switch (activeTab.value) {
    case 'dashboard': loadDashboard(); break
    case 'users': loadUsers(1); break
    case 'songs': loadSongs(1); break
    case 'albums': loadAlbums(1); break
    case 'artists': loadArtists(1); break
    case 'comments': loadComments(1); break
    case 'reports': loadReports(1); break
    case 'banners': loadBanners(); break
    case 'notices': loadNotices(1); break
    case 'logs': loadLogs(1); break
  }
}

/**
 * 跳转到指定标签页（支持设置筛选条件）
 * @param {string} tab 标签页 key
 * @param {string} [statusFilter] 可选的歌曲状态筛选
 */
function switchToTab(tab, statusFilter) {
  // 如果传入了状态筛选且是歌曲管理页，设置筛选条件
  if (tab === 'songs' && statusFilter) {
    songStatusFilter.value = statusFilter
  }
  activeTab.value = tab
  switchTab()
}

onMounted(async () => {
  // 如果 query 中有 tab 参数，直接跳转到对应标签页
  if (route.query.tab) {
    await nextTick()
    switchTab()
  } else {
    loadDashboard()
  }
})

onUnmounted(() => {
  if (userChart) { userChart.destroy(); userChart = null }
  if (genreChart) { genreChart.destroy(); genreChart = null }
})
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f0f2f5; }

.admin-header {
  background: #1a1a2e; color: #fff; padding: 16px 24px; position: sticky; top: 52px; z-index: 100;
}
.admin-header h1 { font-size: 20px; margin-bottom: 12px; }

.admin-tabs { display: flex; gap: 4px; overflow-x: auto; flex-wrap: wrap; }
.tab-btn {
  padding: 6px 14px; background: rgba(255,255,255,0.1); border: none; color: rgba(255,255,255,0.8);
  border-radius: 6px; font-size: 13px; cursor: pointer; white-space: nowrap; transition: all 0.2s;
}
.tab-btn:hover { background: rgba(255,255,255,0.2); }
.tab-btn.active { background: #1565C0; color: #fff; }

.admin-body { max-width: 1200px; margin: 0 auto; padding: 20px; }
.tab-content { background: #fff; border-radius: 12px; padding: 20px; min-height: 300px; }

/* 统计卡片 */
.stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px; }
.stat-card {
  text-align: center; padding: 24px 16px; background: #f8f9fa; border-radius: 12px;
  border: 1px solid #eee;
}
.stat-value { font-size: 32px; font-weight: 700; color: #333; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
.stat-card.vip .stat-value { color: #E91E63; }
.stat-card.highlight .stat-value { color: #4CAF50; }
.stat-card.warn .stat-value { color: #FF9800; }

/* 工具栏 */
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
.search-input {
  padding: 8px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; width: 240px;
}
.filter-select {
  padding: 8px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; background: #fff;
}

/* 表格 */
.admin-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.admin-table th {
  text-align: left; padding: 10px 12px; background: #f8f9fa; border-bottom: 2px solid #eee;
  font-weight: 600; color: #555; white-space: nowrap;
}
.admin-table td { padding: 10px 12px; border-bottom: 1px solid #f0f0f0; }
.admin-table tr:hover td { background: #f8f9ff; }
.cell-content { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 标签 */
.tag-success { background: #E8F5E9; color: #2E7D32; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.tag-danger { background: #FFEBEE; color: #C62828; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.tag-warn { background: #FFF3E0; color: #E65100; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.tag-muted { background: #f5f5f5; color: #999; padding: 2px 8px; border-radius: 4px; font-size: 12px; }

/* 操作按钮 */
.btn-primary {
  padding: 8px 20px; background: #1565C0; color: #fff; border: none; border-radius: 8px; font-size: 13px; cursor: pointer;
}
.btn-primary:hover { background: #0D47A1; }
.btn-sm {
  padding: 4px 10px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 4px; font-size: 12px; cursor: pointer; margin-right: 4px;
}
.btn-sm:hover { background: #eee; }
.btn-danger-sm {
  padding: 4px 10px; background: #FFEBEE; color: #C62828; border: 1px solid #FFCDD2;
  border-radius: 4px; font-size: 12px; cursor: pointer; margin-right: 4px;
}
.btn-danger-sm:hover { background: #FFCDD2; }
.btn-success-sm {
  padding: 4px 10px; background: #E8F5E9; color: #2E7D32; border: 1px solid #C8E6C9;
  border-radius: 4px; font-size: 12px; cursor: pointer; margin-right: 4px;
}
.btn-success-sm:hover { background: #C8E6C9; }
.btn-warn-sm {
  padding: 4px 10px; background: #FFF3E0; color: #E65100; border: 1px solid #FFE0B2;
  border-radius: 4px; font-size: 12px; cursor: pointer; margin-right: 4px;
}
.btn-warn-sm:hover { background: #FFE0B2; }
.action-cell { white-space: nowrap; }

/* 轮播图预览 */
.banner-preview { width: 60px; height: 34px; object-fit: cover; border-radius: 4px; }

/* 弹窗 */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 2000;
}
.modal-card {
  background: #fff; border-radius: 12px; padding: 24px; width: 420px; max-width: 90vw;
}
.modal-card.wide { width: 560px; }
.modal-card h3 { margin-bottom: 16px; font-size: 18px; }
.modal-hint { font-size: 13px; color: #666; margin-bottom: 12px; }
.modal-input {
  width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 8px;
  font-size: 14px; margin-bottom: 12px; box-sizing: border-box;
}
.modal-textarea {
  width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 8px;
  font-size: 14px; resize: vertical; margin-bottom: 12px; box-sizing: border-box;
}
.modal-checkbox { display: flex; align-items: center; gap: 8px; font-size: 14px; margin-bottom: 12px; }
.modal-actions { display: flex; gap: 12px; justify-content: flex-end; }
.btn-cancel {
  padding: 8px 20px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; cursor: pointer;
}
.btn-confirm {
  padding: 8px 20px; background: #1565C0; color: #fff; border: none; border-radius: 8px; font-size: 14px; cursor: pointer;
}
.btn-confirm.danger { background: #E53935; }
.btn-confirm:hover { opacity: 0.9; }

.loading { text-align: center; color: #999; padding: 60px; font-size: 14px; }
.error { color: #E53935; font-size: 13px; margin-top: 8px; text-align: center; }

/* 可点击的统计卡片 */
.stat-card.clickable { cursor: pointer; transition: transform 0.15s, box-shadow 0.15s; }
.stat-card.clickable:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

/* 快捷入口按钮组 */
.quick-actions {
  display: flex;
  gap: 8px;
  margin-top: 20px;
  flex-wrap: wrap;
}
.quick-btn {
  padding: 8px 16px;
  background: #f0f4ff;
  border: 1px solid #d0d8f0;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  color: #333;
}
.quick-btn:hover {
  background: #1565C0;
  color: #fff;
  border-color: #1565C0;
}

/* 图表区域 */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 24px;
}
.chart-card {
  background: #f8f9fa;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 20px;
}
.chart-title {
  font-size: 15px;
  color: #333;
  margin-bottom: 16px;
}
.chart-wrapper {
  position: relative;
  height: 220px;
}
@media (max-width: 768px) {
  .charts-row { grid-template-columns: 1fr; }
}
</style>
