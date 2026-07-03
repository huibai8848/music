<template>
  <div class="report-overlay" @click.self="$emit('close')">
    <div class="report-modal">
      <div class="report-header">
        <h3>🚨 举报</h3>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <!-- 举报目标信息 -->
      <div class="report-target" v-if="targetInfo">
        举报内容：{{ targetInfo }}
      </div>

      <!-- 举报原因 -->
      <div class="report-section">
        <label class="section-label">举报原因</label>
        <div class="reason-options">
          <button
            v-for="r in reasons"
            :key="r.value"
            class="reason-btn"
            :class="{ selected: selectedReason === r.value }"
            @click="selectedReason = r.value"
          >
            {{ r.label }}
          </button>
        </div>
        <p v-if="error && !selectedReason" class="field-error">请选择举报原因</p>
      </div>

      <!-- 补充说明 -->
      <div class="report-section">
        <label class="section-label">补充说明（可选）</label>
        <textarea
          v-model="description"
          placeholder="请详细描述违规内容..."
          maxlength="500"
          rows="4"
        ></textarea>
        <div class="char-count">{{ description.length }}/500</div>
      </div>

      <!-- 提交按钮 -->
      <div class="report-actions">
        <button class="btn-cancel" @click="$emit('close')">取消</button>
        <button class="btn-submit" @click="handleSubmit" :disabled="submitting">
          {{ submitting ? '提交中...' : '提交举报' }}
        </button>
      </div>

      <p v-if="error && selectedReason" class="error">{{ error }}</p>
      <p v-if="success" class="success">✅ 举报已提交，请等待处理</p>
    </div>
  </div>
</template>

<script setup>
/**
 * 举报弹窗组件
 *
 * 通用举报组件，支持对评论/歌曲/歌单等内容进行举报。
 * 调用 POST /api/reports 提交举报。
 *
 * 使用方式：
 * <ReportDialog
 *   :target-type="'COMMENT'"
 *   :target-id="comment.id"
 *   :target-info="'评论内容摘要'"
 *   @close="showReport = false"
 * />
 */
import { ref } from 'vue'
import request from '../utils/request'

const props = defineProps({
  targetType: { type: String, required: true },  // COMMENT / SONG / PLAYLIST
  targetId: { type: Number, required: true },
  targetInfo: { type: String, default: '' }
})

const emit = defineEmits(['close'])

const reasons = [
  { value: 'PORNOGRAPHY', label: '色情内容' },
  { value: 'AD', label: '广告垃圾' },
  { value: 'ABUSE', label: '人身攻击' },
  { value: 'COPYRIGHT', label: '侵犯版权' },
  { value: 'OTHER', label: '其他' }
]

const selectedReason = ref('')
const description = ref('')
const submitting = ref(false)
const error = ref('')
const success = ref(false)

async function handleSubmit() {
  if (!selectedReason.value) {
    error.value = '请选择举报原因'
    return
  }
  error.value = ''
  submitting.value = true
  try {
    const res = await request.post('/reports', {
      targetType: props.targetType,
      targetId: props.targetId,
      reason: selectedReason.value,
      description: description.value
    })
    if (res.code === 200) {
      success.value = true
      setTimeout(() => emit('close'), 2000)
    } else {
      error.value = res.message || '提交失败'
    }
  } catch (e) {
    error.value = e.message || '提交失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.report-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 3000;
}
.report-modal {
  background: #fff; border-radius: 14px; padding: 24px;
  width: 420px; max-width: 90vw; max-height: 80vh; overflow-y: auto;
}
.report-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.report-header h3 { margin: 0; font-size: 18px; }
.close-btn { background: none; border: none; font-size: 18px; cursor: pointer; color: #999; }
.close-btn:hover { color: #333; }
.report-target {
  background: #f5f5f5; border-radius: 8px; padding: 10px 14px;
  font-size: 13px; color: #666; margin-bottom: 16px; word-break: break-all;
}
.report-section { margin-bottom: 16px; }
.section-label { display: block; font-size: 14px; font-weight: 500; color: #333; margin-bottom: 8px; }
.reason-options { display: flex; flex-wrap: wrap; gap: 8px; }
.reason-btn {
  padding: 6px 16px; border: 1px solid #ddd; border-radius: 20px;
  background: #fff; font-size: 13px; cursor: pointer; transition: all 0.2s;
}
.reason-btn:hover { border-color: #1565C0; color: #1565C0; }
.reason-btn.selected { background: #1565C0; color: #fff; border-color: #1565C0; }
.report-section textarea {
  width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 8px;
  font-size: 14px; resize: vertical; outline: none; box-sizing: border-box;
}
.report-section textarea:focus { border-color: #1565C0; }
.char-count { text-align: right; font-size: 12px; color: #999; margin-top: 4px; }
.report-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 8px; }
.btn-cancel {
  padding: 8px 20px; background: #f5f5f5; border: 1px solid #ddd;
  border-radius: 8px; font-size: 14px; cursor: pointer;
}
.btn-submit {
  padding: 8px 20px; background: #E53935; color: #fff; border: none;
  border-radius: 8px; font-size: 14px; cursor: pointer;
}
.btn-submit:hover { background: #C62828; }
.btn-submit:disabled { background: #EF9A9A; cursor: not-allowed; }
.field-error { color: #E53935; font-size: 12px; margin-top: 4px; }
.error { color: #E53935; font-size: 13px; margin-top: 8px; text-align: center; }
.success { color: #2E7D32; font-size: 13px; margin-top: 8px; text-align: center; }
</style>
