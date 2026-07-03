<template>
  <div class="comment-section">
    <!-- 发表评论 -->
    <div class="comment-input" v-if="authStore.isLoggedIn">
      <textarea v-model="newComment" :placeholder="'发表评论...（最多500字）'" maxlength="500" rows="2"></textarea>
      <div class="input-actions">
        <span class="char-count">{{ newComment.length }}/500</span>
        <button @click="submitComment" :disabled="!newComment.trim() || submitting" class="btn-submit">
          {{ submitting ? '发送中...' : '发表评论' }}
        </button>
      </div>
    </div>
    <div v-else class="login-hint">
      <router-link to="/login">登录</router-link> 后可以发表评论
    </div>

    <!-- 评论列表 -->
    <div class="comment-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-avatar">
          {{ comment.nickname ? comment.nickname.charAt(0).toUpperCase() : '?' }}
        </div>
        <div class="comment-body">
          <div class="comment-header">
            <router-link :to="'/users/' + comment.userId" class="comment-user">{{ comment.nickname || '匿名' }}</router-link>
            <span class="comment-time">{{ formatTime(comment.createdTime) }}</span>
            <button v-if="canDelete(comment)" class="delete-btn" @click="deleteComment(comment.id)">删除</button>
            <button class="report-btn" @click="openReport(comment)" title="举报">🚨</button>
          </div>
          <div class="comment-content">{{ comment.content }}</div>

          <!-- 子评论 -->
          <div v-if="comment.replies && comment.replies.length > 0" class="replies">
            <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <router-link :to="'/users/' + reply.userId" class="reply-user">{{ reply.nickname || '匿名' }}</router-link>
              <span class="reply-content">{{ reply.content }}</span>
            </div>
          </div>

          <!-- 回复按钮 -->
          <div class="comment-actions">
            <button class="action-btn" @click="toggleReply(comment.id)">回复</button>
            <span v-if="comment.replyCount > 3" class="more-replies">
              共 {{ comment.replyCount }} 条回复
            </span>
          </div>

          <!-- 回复输入框 -->
          <div v-if="replyingTo === comment.id" class="reply-input">
            <div class="reply-label" v-if="replyTargetName">{{ replyTargetName }}</div>
            <input v-model="replyContent" :placeholder="'回复 @' + (comment.nickname || '匿名') + '...'" @keyup.enter="onReplyKeydown($event, comment.id)" maxlength="500" />
            <button @click="submitReply(comment.id)" :disabled="!replyContent.trim()">回复</button>
            <button class="reply-cancel" @click="cancelReply">取消</button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="comments.length === 0" class="empty-comments">暂无评论，来说点什么吧</div>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="load-more">
        <button @click="loadMore">加载更多评论</button>
      </div>
    </div>

    <!-- 举报弹窗 -->
    <ReportDialog
      v-if="showReport"
      :target-type="reportTarget.type"
      :target-id="reportTarget.id"
      :target-info="reportTarget.info"
      @close="showReport = false"
    />
  </div>
</template>

<script setup>
/**
 * 评论区块组件
 *
 * 功能：
 * 1. 发表评论和楼中楼回复
 * 2. 分页加载评论列表（每页显示 10 条）
 * 3. 自己或管理员可删除评论
 * 4. XSS 过滤由后端完成
 */
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'
import ReportDialog from './ReportDialog.vue'

const props = defineProps({
  targetType: { type: String, required: true },  // SONG or PLAYLIST
  targetId: { type: Number, required: true }
})

const authStore = useAuthStore()

const comments = ref([])
const newComment = ref('')
const submitting = ref(false)
const replyingTo = ref(null)
const replyContent = ref('')
const replyTargetName = ref('')

// 举报
const showReport = ref(false)
const reportTarget = ref({ type: '', id: 0, info: '' })

function openReport(comment) {
  reportTarget.value = {
    type: 'COMMENT',
    id: comment.id,
    info: (comment.content || '').substring(0, 100)
  }
  showReport.value = true
}

const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

const hasMore = computed(() => comments.value.length < total.value)


async function loadComments() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await request.get(`/comments/${props.targetType}/${props.targetId}`, {
      params: { page: page.value, size: size.value }
    })
    if (res.code === 200) {
      if (page.value === 1) {
        comments.value = res.data.records || []
      } else {
        comments.value = [...comments.value, ...(res.data.records || [])]
      }
      total.value = res.data.total || 0
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function loadMore() {
  page.value++
  await loadComments()
}

async function submitComment() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    const res = await request.post(`/comments/${props.targetType}/${props.targetId}`, {
      content: newComment.value
    })
    if (res.code === 200) {
      newComment.value = ''
      page.value = 1
      await loadComments()
    }
  } catch (e) {
    alert(e.message || '评论失败')
  } finally {
    submitting.value = false
  }
}

function toggleReply(commentId) {
  const comment = comments.value.find(c => c.id === commentId)
  if (replyingTo.value === commentId) {
    replyingTo.value = null
    replyContent.value = ''
    replyTargetName.value = ''
    return
  }
  replyingTo.value = commentId
  replyTargetName.value = '@' + (comment?.nickname || '匿名')
  replyContent.value = replyTargetName.value + ' '
  // 将光标移到输入框末尾
  setTimeout(() => {
    const inputs = document.querySelectorAll('.reply-input input')
    const last = inputs[inputs.length - 1]
    if (last) {
      const len = last.value.length
      last.setSelectionRange(len, len)
      last.focus()
    }
  }, 50)
}

function cancelReply() {
  replyingTo.value = null
  replyContent.value = ''
  replyTargetName.value = ''
}

function onReplyKeydown(event, commentId) {
  if (event.isComposing || event.keyCode === 229) return
  submitReply(commentId)
}

async function submitReply(commentId) {
  if (!replyContent.value.trim()) return
  try {
    await request.post(`/comments/${props.targetType}/${props.targetId}`, {
      content: replyContent.value,
      parentId: commentId
    })
    replyContent.value = ''
    replyingTo.value = null
    replyTargetName.value = ''
    // 刷新评论列表
    page.value = 1
    await loadComments()
  } catch (e) {
    alert(e.message || '回复失败')
  }
}

function canDelete(comment) {
  return authStore.isAdmin || (authStore.user?.id === comment.userId)
}

async function deleteComment(commentId) {
  if (!confirm('确定删除此评论？')) return
  try {
    await request.delete(`/comments/${commentId}`)
    comments.value = comments.value.filter(c => c.id !== commentId)
  } catch { alert('删除失败') }
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

onMounted(loadComments)
</script>

<style scoped>
.comment-section { width: 100%; }

/* 输入框 */
.comment-input { margin-bottom: 20px; }
.comment-input textarea {
  width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 8px;
  font-size: 14px; resize: none; outline: none;
}
.comment-input textarea:focus { border-color: #1565C0; }
.input-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.char-count { font-size: 12px; color: #999; }
.btn-submit {
  padding: 6px 20px; background: #1565C0; color: #fff; border: none;
  border-radius: 6px; font-size: 13px;
}
.btn-submit:disabled { background: #90CAF9; cursor: not-allowed; }
.login-hint { text-align: center; color: #999; padding: 20px; font-size: 13px; }

/* 评论列表 */
.comment-item { display: flex; gap: 12px; padding: 16px 0; border-bottom: 1px solid #f0f0f0; }
.comment-avatar {
  width: 36px; height: 36px; border-radius: 50%; background: #1565C0;
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 14px; flex-shrink: 0;
}
.comment-body { flex: 1; min-width: 0; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.comment-user { font-size: 13px; font-weight: 500; color: #333; text-decoration: none; }
.comment-user:hover { color: #1565C0; text-decoration: underline; }
.comment-time { font-size: 11px; color: #999; }
.delete-btn { font-size: 11px; color: #E53935; background: none; border: none; margin-left: auto; display: none; }
.report-btn { font-size: 14px; background: none; border: none; cursor: pointer; opacity: 0; padding: 0 4px; }
.comment-item:hover .delete-btn,
.comment-item:hover .report-btn { display: inline; opacity: 1; }
.report-btn:hover { transform: scale(1.2); }
.comment-content { font-size: 14px; color: #333; line-height: 1.5; word-wrap: break-word; }

/* 子评论 */
.replies { margin-top: 8px; padding-left: 12px; border-left: 2px solid #e0e0e0; }
.reply-item { padding: 6px 0; font-size: 13px; }
.reply-user { font-weight: 500; color: #1565C0; margin-right: 8px; text-decoration: none; }
.reply-user:hover { text-decoration: underline; }
.reply-content { color: #333; }

/* 操作 */
.comment-actions { display: flex; gap: 12px; margin-top: 6px; }
.action-btn { font-size: 12px; color: #999; background: none; border: none; cursor: pointer; }
.action-btn:hover { color: #1565C0; }
.more-replies { font-size: 12px; color: #1565C0; }

/* 回复输入 */
.reply-input { display: flex; gap: 8px; margin-top: 8px; }
.reply-input input { flex: 1; padding: 6px 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; }
.reply-input button { padding: 6px 14px; background: #1565C0; color: #fff; border: none; border-radius: 6px; font-size: 12px; white-space: nowrap; }
.reply-input button:disabled { background: #90CAF9; }
.reply-label { font-size: 12px; color: #1565C0; font-weight: 500; margin-bottom: 4px; }
.reply-cancel { background: #f5f5f5; border: 1px solid #ddd; color: #666; padding: 6px 14px; border-radius: 6px; font-size: 12px; cursor: pointer; white-space: nowrap; }
.reply-cancel:hover { background: #eee; }

.empty-comments { text-align: center; color: #ccc; padding: 40px; font-size: 14px; }
.load-more { text-align: center; padding: 12px; }
.load-more button { padding: 6px 24px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; }
.load-more button:hover { background: #eee; }
</style>
