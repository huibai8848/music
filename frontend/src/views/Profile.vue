<template>
  <div class="profile-page">
    <!-- 用户信息头部 -->
    <div class="profile-header">
      <div class="avatar-area">
        <div class="avatar">
          <img v-if="authStore.user?.avatar" :src="authStore.user.avatar" class="avatar-img" @error="onAvatarError" />
          <span v-else>{{ authStore.user?.nickname?.charAt(0)?.toUpperCase() || '?' }}</span>
        </div>
        <h2>{{ authStore.user?.nickname || '未知用户' }}</h2>
        <p class="email">{{ authStore.user?.email || '-' }}</p>
      </div>
      <div class="profile-meta">
        <div class="meta-row"><span class="label">角色</span>
          <span class="role-badge" :class="(authStore.user?.role || 'user').toLowerCase()">
            {{ roleText(authStore.user?.role) }}
          </span>
        </div>
        <div class="meta-row"><span class="label">会员</span>
          <span class="value">{{ authStore.user?.vipExpireTime ? '✅ 已开通至 ' + formatDate(authStore.user.vipExpireTime) : '未开通' }}</span>
        </div>
        <div class="meta-row"><span class="label">注册时间</span>
          <span class="value">{{ formatDate(authStore.user?.createdTime) }}</span>
        </div>
      </div>
      <div class="profile-actions">
        <button class="btn-action" @click="openEditModal">✏️ 编辑资料</button>
        <button class="btn-action" @click="openPasswordModal">🔑 修改密码</button>
        <button class="btn-action" @click="openMembershipModal">⭐ 升级会员</button>
      </div>
    </div>

    <!-- 数据标签页 -->
    <div class="tabs">
      <button class="tab-btn" :class="{ active: activeTab === 'favorites' }" @click="activeTab = 'favorites'">❤️ 我的收藏</button>
      <button class="tab-btn" :class="{ active: activeTab === 'history' }" @click="activeTab = 'history'">🕐 播放历史</button>
    </div>

    <!-- 收藏列表（按类型分组展示） -->
    <div class="tab-content" v-if="activeTab === 'favorites'">
      <div v-if="loadingFav" class="loading-text">加载中...</div>
      <div v-else-if="favorites.length === 0" class="empty-text">暂无收藏，去浏览歌曲或专辑吧</div>
      <div v-else class="fav-sections">
        <!-- 歌曲收藏 -->
        <div v-if="groupedFavs.SONG && groupedFavs.SONG.length > 0" class="fav-section">
          <h4 class="section-title">🎵 歌曲 ({{ groupedFavs.SONG.length }})</h4>
          <div class="fav-grid">
            <div v-for="fav in groupedFavs.SONG" :key="fav.id" class="fav-card" @click="goTarget(fav)">
              <img :src="fav.coverUrl || defaultCover" class="fav-cover" @error="onCoverError" />
              <div class="fav-card-info">
                <div class="fav-card-title">{{ fav.targetName }}</div>
                <div class="fav-card-sub">{{ fav.artistName || '' }}</div>
              </div>
              <button class="btn-unfav-sm" @click.stop="unFavorite(fav)" title="取消收藏">✕</button>
            </div>
          </div>
        </div>

        <!-- 艺人收藏 -->
        <div v-if="groupedFavs.ARTIST && groupedFavs.ARTIST.length > 0" class="fav-section">
          <h4 class="section-title">🎤 艺人 ({{ groupedFavs.ARTIST.length }})</h4>
          <div class="fav-grid artist-grid">
            <div v-for="fav in groupedFavs.ARTIST" :key="fav.id" class="fav-card artist-card" @click="goTarget(fav)">
              <div class="artist-avatar-wrap">
                <img :src="fav.coverUrl || defaultAvatar" class="artist-avatar" @error="onAvatarError2" />
              </div>
              <div class="fav-card-info">
                <div class="fav-card-title">{{ fav.targetName }}</div>
              </div>
              <button class="btn-unfav-sm" @click.stop="unFavorite(fav)" title="取消收藏">✕</button>
            </div>
          </div>
        </div>

        <!-- 专辑收藏 -->
        <div v-if="groupedFavs.ALBUM && groupedFavs.ALBUM.length > 0" class="fav-section">
          <h4 class="section-title">💿 专辑 ({{ groupedFavs.ALBUM.length }})</h4>
          <div class="fav-grid">
            <div v-for="fav in groupedFavs.ALBUM" :key="fav.id" class="fav-card" @click="goTarget(fav)">
              <img :src="fav.coverUrl || defaultCover" class="fav-cover" @error="onCoverError" />
              <div class="fav-card-info">
                <div class="fav-card-title">{{ fav.targetName }}</div>
              </div>
              <button class="btn-unfav-sm" @click.stop="unFavorite(fav)" title="取消收藏">✕</button>
            </div>
          </div>
        </div>

        <!-- 歌单收藏 -->
        <div v-if="groupedFavs.PLAYLIST && groupedFavs.PLAYLIST.length > 0" class="fav-section">
          <h4 class="section-title">📋 歌单 ({{ groupedFavs.PLAYLIST.length }})</h4>
          <div class="fav-grid">
            <div v-for="fav in groupedFavs.PLAYLIST" :key="fav.id" class="fav-card" @click="goTarget(fav)">
              <img :src="fav.coverUrl || defaultCover" class="fav-cover" @error="onCoverError" />
              <div class="fav-card-info">
                <div class="fav-card-title">{{ fav.targetName }}</div>
              </div>
              <button class="btn-unfav-sm" @click.stop="unFavorite(fav)" title="取消收藏">✕</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 播放历史 -->
    <div class="tab-content" v-if="activeTab === 'history'">
      <div v-if="loadingHist" class="loading-text">加载中...</div>
      <div v-else-if="historyList.length === 0" class="empty-text">暂无播放记录</div>
      <div v-else class="hist-list">
        <div v-for="h in historyList" :key="h.id" class="hist-item" @click="router.push('/songs/' + h.songId)">
          <img :src="h.coverUrl || defaultCover" class="hist-cover" @error="onCoverError">
          <div class="hist-info">
            <div class="hist-title">{{ h.title || '未知歌曲' }}</div>
          </div>
          <span class="hist-time">{{ formatTimeAgo(h.playedTime) }}</span>
        </div>
      </div>
    </div>

    <!-- ====== 编辑资料弹窗 ====== -->
    <div class="modal-overlay" v-if="showEditModal" @click.self="showEditModal = false">
      <div class="modal-card">
        <h2>✏️ 编辑个人资料</h2>
        <div class="form-group">
          <label>头像</label>
          <div class="avatar-upload">
            <div class="avatar-preview">
              <img v-if="editForm.avatarPreview" :src="editForm.avatarPreview" class="avatar-img" @error="onAvatarError" />
              <div v-else class="avatar-placeholder">{{ authStore.user?.nickname?.charAt(0)?.toUpperCase() || '?' }}</div>
            </div>
            <div class="avatar-input-area">
              <input ref="avatarInputRef" type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" hidden @change="onAvatarFileSelect" />
              <button type="button" class="btn-select-avatar" @click="avatarInputRef.click()">选择头像</button>
              <p class="avatar-hint">支持 JPG/PNG，最大 5MB</p>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input v-model="editForm.nickname" maxlength="30" />
        </div>
        <p v-if="editError" class="msg-error">{{ editError }}</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showEditModal = false">取消</button>
          <button class="btn-primary" @click="submitEdit" :disabled="editing">{{ editing ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- ====== 修改密码弹窗 ====== -->
    <div class="modal-overlay" v-if="showPasswordModal" @click.self="showPasswordModal = false">
      <div class="modal-card">
        <h2>🔑 修改密码</h2>
        <div class="form-group">
          <label>当前密码</label>
          <input v-model="pwdForm.oldPassword" type="password" />
        </div>
        <div class="form-group">
          <label>新密码（8-20位，含字母和数字）</label>
          <input v-model="pwdForm.newPassword" type="password" minlength="8" />
        </div>
        <div class="form-group">
          <label>确认新密码</label>
          <input v-model="pwdForm.confirmPassword" type="password" />
        </div>
        <p v-if="pwdError" class="msg-error">{{ pwdError }}</p>
        <p v-if="pwdSuccess" class="msg-success">{{ pwdSuccess }}</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showPasswordModal = false">取消</button>
          <button class="btn-primary" @click="submitPassword" :disabled="changingPwd">{{ changingPwd ? '修改中...' : '确认修改' }}</button>
        </div>
      </div>
    </div>

    <!-- ====== 会员升级弹窗 ====== -->
    <div class="modal-overlay" v-if="showMembershipModal" @click.self="showMembershipModal = false">
      <div class="modal-card">
        <h2>⭐ 升级 VIP 会员</h2>
        <p class="modal-desc">开通 VIP 会员，享受更多权益：上传歌曲、专属标识等</p>
        <div class="plan-list">
          <label class="plan-item" :class="{ selected: selectedPlan === 'monthly' }">
            <input type="radio" v-model="selectedPlan" value="monthly" />
            <div class="plan-info">
              <span class="plan-name">月度会员</span>
              <span class="plan-price">¥15/月</span>
            </div>
          </label>
          <label class="plan-item" :class="{ selected: selectedPlan === 'yearly' }">
            <input type="radio" v-model="selectedPlan" value="yearly" />
            <div class="plan-info">
              <span class="plan-name">年度会员</span>
              <span class="plan-price">¥120/年</span>
              <span class="plan-tag">省 ¥60</span>
            </div>
          </label>
        </div>
        <p v-if="membershipMsg" class="msg-success">{{ membershipMsg }}</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showMembershipModal = false">取消</button>
          <button class="btn-primary" @click="submitMembership" :disabled="upgrading">{{ upgrading ? '处理中...' : '立即开通' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 个人中心页面
 *
 * 功能：
 * 1. 展示用户信息（昵称/邮箱/角色/会员）
 * 2. 编辑个人资料（PUT /api/users/me）
 * 3. 修改密码（PUT /api/users/me/password）
 * 4. 升级会员（POST /api/users/me/membership）
 * 5. 收藏列表（GET /api/favorites / DELETE /api/favorites）— 按类型分组
 * 6. 播放历史（GET /api/history）
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'

const router = useRouter()
const authStore = useAuthStore()

const defaultCover = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="38" text-anchor="middle" font-size="24">🎵</text></svg>')
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><rect fill="#e0e0e0" width="60" height="60"/><text x="30" y="34" text-anchor="middle" font-size="20">👤</text></svg>')

function onCoverError(e) { e.target.src = defaultCover }
function onAvatarError(e) { e.target.style.display = 'none' }
function onAvatarError2(e) { e.target.src = defaultAvatar }

function roleText(role) {
  const map = { ADMIN: '管理员', VIP: 'VIP 会员', USER: '普通用户' }
  return map[role] || role || '未知'
}

function formatDate(t) {
  if (!t) return '-'
  return new Date(t).toLocaleDateString('zh-CN')
}

function formatTimeAgo(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = Math.floor((now - d) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  return d.toLocaleDateString('zh-CN')
}

function goTarget(fav) {
  const pathMap = { SONG: '/songs/', ARTIST: '/artists/', ALBUM: '/albums/', PLAYLIST: '/playlists/' }
  const base = pathMap[fav.targetType]
  if (base) router.push(base + fav.targetId)
}

// ====== 标签页 ======
const activeTab = ref('favorites')

// ====== 收藏列表 ======
const favorites = ref([])
const loadingFav = ref(false)

/** 按 targetType 分组的收藏 */
const groupedFavs = computed(() => {
  const groups = { SONG: [], ARTIST: [], ALBUM: [], PLAYLIST: [] }
  for (const fav of favorites.value) {
    const type = fav.targetType
    if (groups[type]) {
      groups[type].push(fav)
    } else {
      groups[type] = [fav]
    }
  }
  return groups
})

async function loadFavorites() {
  loadingFav.value = true
  try {
    const res = await request.get('/favorites')
    if (res.code === 200) {
      const records = res.data?.records || []
      favorites.value = Array.isArray(records) ? records : (Array.isArray(res.data) ? res.data : [])
    }
  } catch { /* ignore */ }
  finally { loadingFav.value = false }
}

async function unFavorite(fav) {
  if (!confirm('确定取消收藏？')) return
  try {
    await request.delete(`/favorites/${fav.targetType}/${fav.targetId}`)
    favorites.value = favorites.value.filter(f => f.id !== fav.id)
  } catch { alert('取消收藏失败') }
}

// ====== 播放历史 ======
const historyList = ref([])
const loadingHist = ref(false)

async function loadHistory() {
  loadingHist.value = true
  try {
    const res = await request.get('/history')
    if (res.code === 200) historyList.value = res.data || []
  } catch { /* ignore */ }
  finally { loadingHist.value = false }
}

// ====== 编辑资料 ======
const showEditModal = ref(false)
const editForm = ref({ nickname: '', avatarPreview: '' })
const editing = ref(false)
const editError = ref('')
const editAvatarFile = ref(null)
const avatarInputRef = ref(null)

function onAvatarFileSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    editError.value = '头像文件过大，最大 5MB'
    return
  }
  editAvatarFile.value = file
  editForm.value.avatarPreview = URL.createObjectURL(file)
  editError.value = ''
}

function openEditModal() {
  editForm.value = {
    nickname: authStore.user?.nickname || '',
    avatarPreview: authStore.user?.avatar || ''
  }
  editAvatarFile.value = null
  editError.value = ''
  showEditModal.value = true
}

async function submitEdit() {
  if (!editForm.value.nickname.trim()) { editError.value = '昵称不能为空'; return }
  editing.value = true
  editError.value = ''
  try {
    let avatarUrl = authStore.user?.avatar || ''

    if (editAvatarFile.value) {
      const uploadFormData = new FormData()
      uploadFormData.append('file', editAvatarFile.value)
      uploadFormData.append('type', 'avatar')
      const uploadRes = await request.post('/files/upload', uploadFormData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      if (uploadRes.code === 200 && uploadRes.data?.url) {
        avatarUrl = uploadRes.data.url
      } else {
        editError.value = '头像上传失败'
        editing.value = false
        return
      }
    }

    const body = { nickname: editForm.value.nickname }
    if (avatarUrl) { body.avatar = avatarUrl }
    const res = await request.put('/users/me', body)
    if (res.code === 200) {
      showEditModal.value = false
      await authStore.fetchUser()
    } else {
      editError.value = res.message || '保存失败'
    }
  } catch (e) {
    editError.value = e.message || '保存失败'
  } finally {
    editing.value = false
  }
}

// ====== 修改密码 ======
const showPasswordModal = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const changingPwd = ref(false)
const pwdError = ref('')
const pwdSuccess = ref('')

function openPasswordModal() {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdError.value = ''
  pwdSuccess.value = ''
  showPasswordModal.value = true
}

async function submitPassword() {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    pwdError.value = '请填写完整'; return
  }
  if (pwdForm.value.newPassword.length < 8 || pwdForm.value.newPassword.length > 20) { pwdError.value = '密码长度需在 8-20 位之间'; return }
  if (!/(?=.*[a-zA-Z])(?=.*\d)/.test(pwdForm.value.newPassword)) { pwdError.value = '密码需包含字母和数字'; return }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) { pwdError.value = '两次密码不一致'; return }
  changingPwd.value = true
  pwdError.value = ''
  pwdSuccess.value = ''
  try {
    const res = await request.put('/users/me/password', {
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    if (res.code === 200) {
      pwdSuccess.value = '✅ 密码修改成功，即将退出重新登录...'
      setTimeout(() => {
        showPasswordModal.value = false
        authStore.logout()
        router.push('/login')
      }, 2000)
    } else {
      pwdError.value = res.message || '修改失败'
    }
  } catch (e) {
    pwdError.value = e.message || '修改失败'
  } finally {
    changingPwd.value = false
  }
}

// ====== 会员升级 ======
const showMembershipModal = ref(false)
const selectedPlan = ref('monthly')
const upgrading = ref(false)
const membershipMsg = ref('')

function openMembershipModal() {
  selectedPlan.value = 'monthly'
  membershipMsg.value = ''
  showMembershipModal.value = true
}

async function submitMembership() {
  upgrading.value = true
  membershipMsg.value = ''
  try {
    const res = await request.post('/users/me/membership', null, {
      params: { plan: selectedPlan.value }
    })
    if (res.code === 200) {
      membershipMsg.value = '🎉 VIP 开通成功！'
      authStore.fetchUser()
      setTimeout(() => { showMembershipModal.value = false }, 2000)
    } else {
      membershipMsg.value = res.message || '开通失败'
    }
  } catch (e) {
    membershipMsg.value = e.message || '开通失败'
  } finally {
    upgrading.value = false
  }
}

onMounted(() => {
  loadFavorites()
  loadHistory()
})
</script>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px;
}

.profile-header {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  margin-bottom: 20px;
}

.avatar-area {
  text-align: center;
  padding: 32px 20px 16px;
  background: linear-gradient(135deg, #1565C0, #0D47A1);
  color: #fff;
}

.avatar {
  width: 72px; height: 72px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
  font-size: 30px; margin: 0 auto 12px;
  border: 3px solid rgba(255,255,255,0.3);
  overflow: hidden;
}

.avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-area h2 { margin: 0 0 4px; font-size: 20px; }
.avatar-area .email { font-size: 14px; opacity: 0.8; }

.profile-meta { padding: 16px 24px; }

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}
.meta-row:last-child { border-bottom: none; }
.meta-row .label { color: #999; }
.meta-row .value { font-weight: 500; }

.role-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}
.role-badge.admin { background: #E53935; color: #fff; }
.role-badge.vip { background: #f59e0b; color: #fff; }
.role-badge.user { background: #e0e0e0; color: #666; }

.profile-actions {
  display: flex;
  gap: 8px;
  padding: 0 24px 16px;
  flex-wrap: wrap;
}

.btn-action {
  background: #f5f5f5;
  border: 1px solid #ddd;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-action:hover { background: #1565C0; color: #fff; border-color: #1565C0; }

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
.tab-btn.active { background: #1565C0; color: #fff; border-color: #1565C0; }

.tab-content {
  background: #fff;
  border-radius: 0 8px 8px 8px;
  padding: 16px;
  min-height: 150px;
}

.loading-text, .empty-text { text-align: center; color: #999; padding: 40px; font-size: 14px; }

/* ===== 收藏按类型分组 ===== */
.fav-sections { display: flex; flex-direction: column; gap: 24px; }

.fav-section h4.section-title {
  margin: 0 0 12px;
  font-size: 15px;
  color: #333;
  border-bottom: 2px solid #1565C0;
  padding-bottom: 6px;
  display: inline-block;
}

.fav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.fav-card {
  background: #f9f9f9;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.fav-card:hover { background: #f0f0f0; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }

.fav-cover {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  background: #e0e0e0;
  display: block;
}

.fav-card-info {
  padding: 8px 10px 10px;
}

.fav-card-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fav-card-sub {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 艺人卡片圆形头像 */
.artist-grid { grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); }
.artist-card { text-align: center; padding: 16px 8px 8px; }
.artist-avatar-wrap { width: 64px; height: 64px; margin: 0 auto; border-radius: 50%; overflow: hidden; background: #e0e0e0; }
.artist-avatar { width: 100%; height: 100%; object-fit: cover; }

.btn-unfav-sm {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px; height: 22px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border: none;
  font-size: 11px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}
.fav-card:hover .btn-unfav-sm { opacity: 1; }
.btn-unfav-sm:hover { background: #E53935; }

/* 播放历史 */
.hist-list { display: flex; flex-direction: column; gap: 6px; }

.hist-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.hist-item:hover { background: #f5f5f5; }

.hist-cover {
  width: 44px; height: 44px;
  border-radius: 6px; object-fit: cover;
  background: #e0e0e0;
}

.hist-info { flex: 1; min-width: 0; }

.hist-title {
  font-size: 14px; font-weight: 500;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.hist-time { font-size: 12px; color: #999; white-space: nowrap; }

/* ====== 弹窗 ====== */
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 2000;
}

.modal-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  width: 420px;
  max-width: 90vw;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

.modal-card h2 { margin: 0 0 16px; font-size: 20px; }
.modal-desc { font-size: 14px; color: #666; margin-bottom: 16px; }

.form-group { margin-bottom: 14px; }
.form-group label { display: block; font-size: 13px; color: #666; margin-bottom: 4px; }
.form-group input {
  width: 100%; padding: 10px 14px;
  border: 1px solid #ddd; border-radius: 8px;
  font-size: 14px; outline: none;
}
.form-group input:focus { border-color: #1565C0; }

.msg-error { color: #E53935; font-size: 13px; margin-bottom: 8px; }
.msg-success { color: #1db954; font-size: 13px; margin-bottom: 8px; }

.avatar-upload { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.avatar-preview { flex-shrink: 0; }
.avatar-img { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; border: 2px solid #e0e0e0; }
.avatar-placeholder { width: 72px; height: 72px; border-radius: 50%; background: #1565C0; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 600; }
.avatar-input-area { display: flex; flex-direction: column; gap: 4px; }
.btn-select-avatar { padding: 6px 16px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; cursor: pointer; color: #333; transition: all 0.2s; }
.btn-select-avatar:hover { background: #1565C0; color: #fff; border-color: #1565C0; }
.avatar-hint { font-size: 11px; color: #999; margin: 0; }

.modal-actions {
  display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px;
}

.btn-cancel {
  padding: 8px 20px; background: #f5f5f5;
  border: 1px solid #ddd; border-radius: 8px; font-size: 14px; cursor: pointer;
}

.btn-primary {
  padding: 8px 20px; background: #1565C0; color: #fff;
  border: none; border-radius: 8px; font-size: 14px; cursor: pointer;
}
.btn-primary:disabled { background: #90CAF9; cursor: not-allowed; }

.plan-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; }

.plan-item {
  display: flex; align-items: center;
  padding: 14px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.plan-item.selected { border-color: #1565C0; background: #E3F2FD; }
.plan-item input { margin-right: 12px; }

.plan-info { flex: 1; display: flex; align-items: center; gap: 12px; }
.plan-name { font-size: 15px; font-weight: 500; }
.plan-price { font-size: 14px; color: #E53935; font-weight: 600; }
.plan-tag { background: #f59e0b; color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
</style>