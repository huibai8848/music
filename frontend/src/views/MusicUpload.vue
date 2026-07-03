<template>
  <div class="music-upload-page">
    <div class="page-header">
      <h1>🎵 上传音乐</h1>
      <p class="page-desc">一次性提交歌曲、封面、歌词与元数据，等待管理员审核</p>
      <p v-if="remainingUploads != null" class="page-hint">
        📊 今日还可上传 <strong>{{ remainingUploads }}</strong> 次
      </p>
    </div>

    <form class="upload-form" @submit.prevent="handleSubmit">
      <!-- ===== 左侧：文件上传区 ===== -->
      <div class="form-files">
        <h3>📂 文件</h3>

        <!-- 音频文件 -->
        <div class="file-field" :class="{ 'has-error': errors.audio }">
          <label class="file-label">音频文件 <span class="required">*</span></label>
          <div class="file-input-wrapper" @dragover.prevent @drop.prevent="e => handleFileDrop(e, 'audio')">
            <input ref="audioInput" type="file" accept=".mp3,.MP3,.aac,.AAC,.m4a,.M4A,audio/mpeg,audio/aac,audio/mp4" hidden @change="e => handleFileSelect(e, 'audio')" />
            <template v-if="!files.audio">
              <div class="file-placeholder" @click="audioInput.click()">
                <span class="file-icon">🎵</span>
                <span>点击选择 MP3 / AAC 文件（≤50MB）</span>
              </div>
            </template>
            <template v-else>
              <div class="file-preview">
                <span class="file-icon">🎵</span>
                <div class="file-info">
                  <span class="file-name">{{ files.audio.name }}</span>
                  <span class="file-size">{{ formatSize(files.audio.size) }}</span>
                </div>
                <button type="button" class="btn-remove-file" @click="removeFile('audio')">✕</button>
              </div>
            </template>
          </div>
          <p v-if="errors.audio" class="field-error">{{ errors.audio }}</p>
        </div>

        <!-- 封面图 -->
        <div class="file-field">
          <label class="file-label">封面图</label>
          <div class="file-input-wrapper" @dragover.prevent @drop.prevent="e => handleFileDrop(e, 'cover')">
            <input ref="coverInput" type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" hidden @change="e => handleFileSelect(e, 'cover')" />
            <template v-if="!files.cover">
              <div class="file-placeholder" @click="coverInput.click()">
                <span class="file-icon">🖼️</span>
                <span>点击选择封面图（JPG/PNG，≤5MB）</span>
              </div>
            </template>
            <template v-else>
              <div class="file-preview">
                <img v-if="coverPreview" :src="coverPreview" class="preview-thumb" />
                <div class="file-info">
                  <span class="file-name">{{ files.cover.name }}</span>
                  <span class="file-size">{{ formatSize(files.cover.size) }}</span>
                </div>
                <button type="button" class="btn-remove-file" @click="removeFile('cover')">✕</button>
              </div>
            </template>
          </div>
        </div>

        <!-- 歌词文件 -->
        <div class="file-field">
          <label class="file-label">歌词文件</label>
          <div class="file-input-wrapper" @dragover.prevent @drop.prevent="e => handleFileDrop(e, 'lyric')">
            <input ref="lyricInput" type="file" accept=".lrc,.txt,text/plain" hidden @change="e => handleFileSelect(e, 'lyric')" />
            <template v-if="!files.lyric">
              <div class="file-placeholder" @click="lyricInput.click()">
                <span class="file-icon">📝</span>
                <span>点击选择歌词文件（LRC/TXT，≤100KB）</span>
              </div>
            </template>
            <template v-else>
              <div class="file-preview">
                <span class="file-icon">📝</span>
                <div class="file-info">
                  <span class="file-name">{{ files.lyric.name }}</span>
                  <span class="file-size">{{ formatSize(files.lyric.size) }}</span>
                </div>
                <button type="button" class="btn-remove-file" @click="removeFile('lyric')">✕</button>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- ===== 右侧：元数据区 ===== -->
      <div class="form-meta">
        <h3>📋 歌曲信息</h3>

        <!-- 歌曲名 -->
        <div class="form-group" :class="{ 'has-error': errors.title }">
          <label>歌曲名 <span class="required">*</span></label>
          <input v-model="form.title" type="text" placeholder="输入歌曲名称" maxlength="100" />
          <p v-if="errors.title" class="field-error">{{ errors.title }}</p>
        </div>

        <!-- 艺人 -->
        <div class="form-row">
          <div class="form-group flex-1" :class="{ 'has-error': errors.artistName }">
            <label>艺人名 <span class="required">*</span></label>
            <input v-model="form.artistName" type="text" placeholder="输入艺人名称" maxlength="100" />
            <p v-if="errors.artistName" class="field-error">{{ errors.artistName }}</p>
          </div>
        </div>

        <!-- 艺人简介 -->
        <div class="form-group">
          <label>艺人简介</label>
          <textarea v-model="form.artistBio" placeholder="艺人简介（可选）" rows="2" maxlength="500"></textarea>
        </div>

        <!-- 专辑 -->
        <div class="form-group">
          <label>专辑名</label>
          <input v-model="form.albumTitle" type="text" placeholder="所属专辑（可选）" maxlength="100" />
        </div>

        <!-- 风格 + 语种 -->
        <div class="form-row">
          <div class="form-group flex-1">
            <label>风格</label>
            <select v-model="form.genre">
              <option value="">不限</option>
              <option>流行</option>
              <option>摇滚</option>
              <option>民谣</option>
              <option>电子</option>
              <option>嘻哈</option>
              <option>古典</option>
              <option>爵士</option>
              <option>R&amp;B</option>
              <option>乡村</option>
              <option>金属</option>
              <option>古风</option>
              <option>其他</option>
            </select>
          </div>
          <div class="form-group flex-1">
            <label>语种</label>
            <select v-model="form.language">
              <option value="">不限</option>
              <option>中文</option>
              <option>英文</option>
              <option>日文</option>
              <option>韩文</option>
              <option>粤语</option>
              <option>法语</option>
              <option>德语</option>
              <option>西班牙语</option>
              <option>其他</option>
            </select>
          </div>
          <div class="form-group flex-1">
            <label>发行年份</label>
            <input v-model="form.releaseYear" type="number" placeholder="可选" min="1900" :max="currentYear" />
          </div>
        </div>

        <!-- 提交 -->
        <div class="form-actions">
          <button type="submit" class="btn-submit" :disabled="submitting">
            {{ submitting ? '提交中...' : '📤 提交审核' }}
          </button>
          <p v-if="submitMsg" class="submit-msg" :class="{ success: submitSuccess, error: !submitSuccess }">
            {{ submitMsg }}
          </p>
        </div>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'

const authStore = useAuthStore()
const currentYear = new Date().getFullYear()

// 文件引用
const audioInput = ref(null)
const coverInput = ref(null)
const lyricInput = ref(null)

// 文件数据
const files = reactive({
  audio: null,
  cover: null,
  lyric: null
})
const coverPreview = ref('')

// 表单数据
const form = reactive({
  title: '',
  artistName: '',
  artistBio: '',
  albumTitle: '',
  genre: '',
  language: '',
  releaseYear: ''
})

const errors = reactive({
  audio: '',
  title: '',
  artistName: ''
})

const submitting = ref(false)
const submitMsg = ref('')
const submitSuccess = ref(false)
/** 当日剩余上传次数（来自上传响应） */
const remainingUploads = ref(null)

/**
 * 组件挂载时刷新用户信息（确保 VIP 状态为最新）
 * 解决开通 VIP 后页面未刷新导致仍无法上传的问题
 */
onMounted(async () => {
  if (authStore.isLoggedIn) {
    await authStore.fetchUser()
  }
})

/** 处理文件选择 */
function handleFileSelect(e, type) {
  const file = e.target.files[0]
  if (file) setFile(file, type)
}

/** 处理拖拽文件 */
function handleFileDrop(e, type) {
  const file = e.dataTransfer?.files[0]
  if (file) setFile(file, type)
}

/** 设置文件并校验 */
function setFile(file, type) {
  const limits = { audio: 50 * 1024 * 1024, cover: 5 * 1024 * 1024, lyric: 100 * 1024 }
  const accepts = {
    audio: /\.(mp3|aac|m4a)$/i,
    cover: /\.(jpg|jpeg|png)$/i,
    lyric: /\.(lrc|txt)$/i
  }

  // 校验大小
  if (file.size > limits[type]) {
    errors[type === 'audio' ? 'audio' : type] = `文件过大，最大允许 ${limits[type] / 1024 / 1024}MB`
    return
  }

  // 校验扩展名
  if (!accepts[type].test(file.name)) {
    errors[type === 'audio' ? 'audio' : type] = '文件格式不支持'
    return
  }

  // 清除对应错误
  if (type === 'audio') errors.audio = ''

  files[type] = file

  // 封面临时预览
  if (type === 'cover') {
    if (coverPreview.value) URL.revokeObjectURL(coverPreview.value)
    coverPreview.value = URL.createObjectURL(file)
  }
}

/** 移除文件 */
function removeFile(type) {
  if (type === 'cover' && coverPreview.value) {
    URL.revokeObjectURL(coverPreview.value)
    coverPreview.value = ''
  }
  files[type] = null
  // 重置 input 值以便重复选择同一文件
  if (type === 'audio' && audioInput.value) audioInput.value.value = ''
  if (type === 'cover' && coverInput.value) coverInput.value.value = ''
  if (type === 'lyric' && lyricInput.value) lyricInput.value.value = ''
}

/** 前端校验 */
function validate() {
  let valid = true
  errors.audio = ''
  errors.title = ''
  errors.artistName = ''

  if (!files.audio) {
    errors.audio = '请选择音频文件'
    valid = false
  }
  if (!form.title.trim()) {
    errors.title = '请输入歌曲名称'
    valid = false
  }
  if (!form.artistName.trim()) {
    errors.artistName = '请输入艺人名称'
    valid = false
  }
  return valid
}

/** 提交表单 */
async function handleSubmit() {
  submitMsg.value = ''
  submitSuccess.value = false

  if (!validate()) return

  submitting.value = true

  try {
    const formData = new FormData()
    formData.append('audio', files.audio)
    if (files.cover) formData.append('cover', files.cover)
    if (files.lyric) formData.append('lyric', files.lyric)

    // 构建 songInfo JSON
    const songInfo = {
      title: form.title.trim(),
      artistName: form.artistName.trim(),
      artistBio: form.artistBio.trim() || undefined,
      albumTitle: form.albumTitle.trim() || undefined,
      genre: form.genre || undefined,
      language: form.language || undefined,
      releaseYear: form.releaseYear ? Number(form.releaseYear) : undefined
    }
    formData.append('songInfo', JSON.stringify(songInfo))

    const res = await request.post('/members/upload-music', formData, {
      timeout: 120000 // 上传文件可能较慢，加长超时
      // 不手动设置 Content-Type，让 Axios 自动生成 multipart boundary
    })

    if (res.code === 200) {
      submitSuccess.value = true
      // 从响应中读取剩余上传次数（后端返回 Map: { song, remainingUploads, dailyLimit }）
      const remaining = res.data?.remainingUploads
      const dailyLimit = res.data?.dailyLimit
      remainingUploads.value = remaining != null ? remaining : null
      if (remaining != null && dailyLimit != null) {
        submitMsg.value = '✅ 上传成功！等待管理员审核（今日还可上传 ' + remaining + '/' + dailyLimit + ' 次）'
      } else {
        submitMsg.value = '✅ 上传成功！等待管理员审核。'
      }
      // 清空表单
      resetForm()
    } else {
      submitMsg.value = res.message || '上传失败'
      remainingUploads.value = null
    }
  } catch (e) {
    submitMsg.value = e.message || '上传失败，请重试'
  } finally {
    submitting.value = false
  }
}

/** 重置表单 */
function resetForm() {
  removeFile('audio')
  removeFile('cover')
  removeFile('lyric')
  form.title = ''
  form.artistName = ''
  form.artistBio = ''
  form.albumTitle = ''
  form.genre = ''
  form.language = ''
  form.releaseYear = ''
}

/** 格式化文件大小 */
function formatSize(bytes) {
  if (bytes >= 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return bytes + ' B'
}
</script>

<style scoped>
.music-upload-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
  color: #e0e0e0;
}

.page-header {
  margin-bottom: 28px;
}

.page-header h1 {
  margin: 0 0 6px;
  font-size: 26px;
  color: #fff;
}

.page-desc {
  color: #999;
  font-size: 14px;
  margin: 0;
}

.page-hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: #1db954;
}

/* 双列布局 */
.upload-form {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 24px;
  align-items: start;
}

@media (max-width: 720px) {
  .upload-form {
    grid-template-columns: 1fr;
  }
}

/* 标题 */
.form-files h3,
.form-meta h3 {
  margin: 0 0 16px;
  font-size: 15px;
  color: #fff;
}

/* ===== 文件上传区域 ===== */
.file-field {
  margin-bottom: 16px;
}

.file-label {
  display: block;
  font-size: 13px;
  color: #aaa;
  margin-bottom: 6px;
}

.required { color: #ef4444; }

.file-input-wrapper {
  background: #1a1a1a;
  border: 2px dashed #3a3a3a;
  border-radius: 10px;
  transition: border-color 0.2s, background 0.2s;
  cursor: pointer;
}

.file-input-wrapper:hover {
  border-color: #555;
  background: #1e1e1e;
}

.file-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 28px 16px;
  font-size: 13px;
  color: #888;
}

.file-icon { font-size: 28px; }

.file-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
}

.preview-thumb {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  object-fit: cover;
  background: #2a2a2a;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  display: block;
  font-size: 13px;
  color: #ccc;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 11px;
  color: #666;
}

.btn-remove-file {
  background: none;
  border: none;
  color: #888;
  font-size: 16px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}

.btn-remove-file:hover {
  background: #333;
  color: #ef4444;
}

/* ===== 表单区域 ===== */
.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: #aaa;
  margin-bottom: 6px;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #3a3a3a;
  border-radius: 8px;
  background: #1a1a1a;
  color: #e0e0e0;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-group textarea {
  resize: vertical;
  font-family: inherit;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #1db954;
}

.form-group select {
  cursor: pointer;
}

.form-group select option {
  background: #2a2a2a;
  color: #e0e0e0;
}

/* 错误状态 */
.has-error input,
.has-error .file-input-wrapper {
  border-color: #ef4444;
}

.field-error {
  color: #ef4444;
  font-size: 12px;
  margin: 4px 0 0;
}

/* 行内布局 */
.form-row {
  display: flex;
  gap: 12px;
}

.flex-1 { flex: 1; }

/* 提交按钮 */
.form-actions {
  margin-top: 24px;
}

.btn-submit {
  width: 100%;
  padding: 12px;
  background: #1db954;
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-submit:hover:not(:disabled) {
  background: #169c46;
}

.btn-submit:disabled {
  background: #2a5a3a;
  color: #888;
  cursor: not-allowed;
}

.submit-msg {
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
  padding: 8px 12px;
  border-radius: 6px;
}

.submit-msg.success {
  background: #0d2818;
  color: #1db954;
}

.submit-msg.error {
  background: #2a0d0d;
  color: #ef4444;
}
</style>
