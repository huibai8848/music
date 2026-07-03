<template>
  <div class="file-uploader" @dragover.prevent @drop.prevent="handleDrop"
       :class="{ 'is-dragover': isDragOver, 'is-uploading': uploading }">
    <!-- 隐藏的文件输入 -->
    <input ref="fileInput" type="file" :accept="acceptStr" @change="handleFileSelect" />

    <!-- 上传区域 -->
    <div class="upload-zone" v-if="!previewUrl">
      <div class="upload-icon">📁</div>
      <p class="upload-text">{{ uploading ? '上传中...' : (dragText || '拖拽文件到此处，或点击选择') }}</p>
      <p class="upload-hint" v-if="!uploading">{{ hintText }}</p>
      <button class="btn-select" @click="openFilePicker" :disabled="uploading">
        {{ buttonText || '选择文件' }}
      </button>
    </div>

    <!-- 预览区域 -->
    <div class="preview-zone" v-else>
      <!-- 图片预览 -->
      <img v-if="isImage" :src="previewUrl" class="preview-image" />
      <!-- 音频预览 -->
      <div v-else-if="isAudio" class="preview-audio">
        <div class="audio-icon">🎵</div>
        <p class="file-name">{{ fileName }}</p>
        <audio :src="previewUrl" controls class="audio-player"></audio>
      </div>
      <!-- 其他文件预览 -->
      <div v-else class="preview-generic">
        <div class="file-icon">📄</div>
        <p class="file-name">{{ fileName }}</p>
        <span class="file-size">{{ formatSize(fileSize) }}</span>
      </div>

      <!-- 操作按钮 -->
      <div class="preview-actions">
        <button class="btn-remove" @click="removeFile">重新选择</button>
        <button v-if="!uploadedUrl" class="btn-upload" @click="doUpload" :disabled="uploading">
          {{ uploading ? '上传中...' : '确认上传' }}
        </button>
      </div>
    </div>

    <!-- 上传进度条 -->
    <div class="progress-bar" v-if="uploading">
      <div class="progress-fill" :style="{ width: progress + '%' }"></div>
    </div>

    <!-- 上传成功结果 -->
    <div class="upload-result" v-if="uploadedUrl">
      <span class="result-success">✅ 上传成功</span>
      <div class="result-url">
        <input :value="uploadedUrl" readonly @click="$event.target.select()" />
        <button class="btn-copy" @click="copyUrl">复制</button>
      </div>
    </div>

    <!-- 错误提示 -->
    <p class="error-msg" v-if="errorMsg">{{ errorMsg }}</p>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import request from '../utils/request'

const props = defineProps({
  /** 文件类型：audio / cover / lyric / avatar / background / voice */
  fileType: { type: String, default: 'cover' },
  /** 房间 ID（语音时才需要） */
  roomId: { type: Number, default: null },
  /** 拖拽提示文字 */
  dragText: { type: String, default: '' },
  /** 选择按钮文字 */
  buttonText: { type: String, default: '' },
  /** 提示文字 */
  hintText: { type: String, default: '' },
  /** 已上传的 URL（用于编辑回显） */
  modelValue: { type: String, default: '' },
  /** 自动上传（选择后立即上传） */
  autoUpload: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'upload-success', 'upload-error'])

const fileInput = ref(null)
const selectedFile = ref(null)
const previewUrl = ref('')
const uploadedUrl = ref('')
const uploading = ref(false)
const progress = ref(0)
const errorMsg = ref('')
const isDragOver = ref(false)

const fileName = ref('')
const fileSize = ref(0)

// 根据文件类型计算 accept 属性
const acceptStr = computed(() => {
  switch (props.fileType) {
    case 'audio': return '.mp3,.MP3,audio/mpeg'
    case 'cover':
    case 'avatar':
    case 'background': return '.jpg,.jpeg,.png,image/jpeg,image/png'
    case 'lyric': return '.lrc,.txt,text/plain'
    case 'voice': return '.webm,.ogg,.wav,audio/webm'
    default: return '*/*'
  }
})

const isImage = computed(() =>
  ['cover', 'avatar', 'background'].includes(props.fileType) && previewUrl.value
)
const isAudio = computed(() =>
  props.fileType === 'audio' && previewUrl.value
)

// 监听外部传入的 URL（编辑回显）
watch(() => props.modelValue, (val) => {
  if (val) {
    uploadedUrl.value = val
  }
}, { immediate: true })

/** 打开文件选择器 */
function openFilePicker() {
  fileInput.value?.click()
}

/** 处理文件选择 */
function handleFileSelect(e) {
  const file = e.target.files[0]
  if (file) processFile(file)
}

/** 处理拖拽 */
function handleDrop(e) {
  isDragOver.value = false
  const file = e.dataTransfer?.files[0]
  if (file) processFile(file)
}

/** 处理文件（校验 + 预览） */
function processFile(file) {
  errorMsg.value = ''

  // 校验大小
  const maxSize = getMaxSize()
  if (file.size > maxSize) {
    errorMsg.value = `文件过大，最大允许 ${formatSize(maxSize)}`
    return
  }

  selectedFile.value = file
  fileName.value = file.name
  fileSize.value = file.size

  // 生成预览
  if (file.type.startsWith('image/')) {
    previewUrl.value = URL.createObjectURL(file)
  } else if (file.type.startsWith('audio/')) {
    previewUrl.value = URL.createObjectURL(file)
  } else {
    previewUrl.value = 'generic'
  }

  // 自动上传
  if (props.autoUpload) {
    doUpload()
  }
}

/** 执行上传 */
async function doUpload() {
  if (!selectedFile.value) return

  uploading.value = true
  progress.value = 0
  errorMsg.value = ''

  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('type', props.fileType)
  if (props.roomId) {
    formData.append('roomId', props.roomId)
  }

  try {
    const res = await request.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        progress.value = e.total ? Math.round((e.loaded / e.total) * 100) : 0
      }
    })

    if (res.code === 200 && res.data?.url) {
      uploadedUrl.value = res.data.url
      emit('update:modelValue', res.data.url)
      emit('upload-success', res.data)
      progress.value = 100
    } else {
      throw new Error(res.message || '上传失败')
    }
  } catch (e) {
    errorMsg.value = e.message || '上传失败，请重试'
    emit('upload-error', e)
  } finally {
    uploading.value = false
  }
}

/** 移除文件 */
function removeFile() {
  selectedFile.value = null
  previewUrl.value = ''
  uploadedUrl.value = ''
  fileName.value = ''
  fileSize.value = 0
  errorMsg.value = ''
  progress.value = 0
  emit('update:modelValue', '')
  if (fileInput.value) fileInput.value.value = ''
}

/** 复制 URL */
async function copyUrl() {
  try {
    await navigator.clipboard.writeText(uploadedUrl.value)
    errorMsg.value = '已复制到剪贴板'
    setTimeout(() => { errorMsg.value = '' }, 2000)
  } catch {
    errorMsg.value = '复制失败，请手动选择复制'
  }
}

/** 获取当前类型的大小限制 */
function getMaxSize() {
  switch (props.fileType) {
    case 'audio': return 50 * 1024 * 1024
    case 'cover':
    case 'avatar':
    case 'background': return 5 * 1024 * 1024
    case 'lyric': return 100 * 1024
    case 'voice': return 5 * 1024 * 1024
    default: return 10 * 1024 * 1024
  }
}

/** 格式化文件大小 */
function formatSize(bytes) {
  if (bytes >= 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return bytes + ' B'
}
</script>

<style scoped>
.file-uploader {
  border: 2px dashed #444;
  border-radius: 12px;
  padding: 20px;
  background: #1a1a1a;
  transition: all 0.3s;
  position: relative;
}

.file-uploader.is-dragover {
  border-color: #1db954;
  background: #1a2e1a;
}

.file-uploader.is-uploading {
  opacity: 0.7;
  pointer-events: none;
}

input[type="file"] { display: none; }

/* 上传区域 */
.upload-zone {
  text-align: center;
  padding: 30px 20px;
  cursor: pointer;
}

.upload-icon { font-size: 48px; margin-bottom: 12px; }

.upload-text {
  color: #ccc;
  font-size: 15px;
  margin-bottom: 8px;
}

.upload-hint {
  color: #666;
  font-size: 12px;
  margin-bottom: 16px;
}

.btn-select {
  background: #333;
  color: #fff;
  border: 1px solid #555;
  padding: 8px 24px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}

.btn-select:hover { background: #444; }

/* 预览区域 */
.preview-zone { text-align: center; }

.preview-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.preview-audio {
  margin-bottom: 12px;
}

.audio-icon { font-size: 48px; margin-bottom: 8px; }

.audio-player {
  width: 100%;
  max-width: 300px;
  margin-top: 8px;
}

.file-icon { font-size: 48px; margin-bottom: 8px; }

.file-name {
  color: #ccc;
  font-size: 14px;
  margin-bottom: 4px;
  word-break: break-all;
}

.file-size { color: #666; font-size: 12px; }

.preview-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 16px;
}

.btn-remove {
  background: transparent;
  border: 1px solid #555;
  color: #ccc;
  padding: 8px 20px;
  border-radius: 20px;
  cursor: pointer;
}

.btn-remove:hover { background: #333; }

.btn-upload {
  background: #1db954;
  border: none;
  color: #fff;
  padding: 8px 24px;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 600;
}

.btn-upload:disabled { background: #444; color: #666; cursor: not-allowed; }

/* 进度条 */
.progress-bar {
  height: 4px;
  background: #333;
  border-radius: 2px;
  margin-top: 12px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #1db954;
  transition: width 0.3s;
  border-radius: 2px;
}

/* 上传结果 */
.upload-result {
  margin-top: 12px;
  text-align: center;
}

.result-success { color: #1db954; font-size: 14px; display: block; margin-bottom: 8px; }

.result-url {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.result-url input {
  flex: 1;
  max-width: 400px;
  padding: 6px 10px;
  border: 1px solid #444;
  border-radius: 6px;
  background: #2a2a2a;
  color: #ccc;
  font-size: 12px;
}

.btn-copy {
  background: #333;
  border: 1px solid #555;
  color: #ccc;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
}

.btn-copy:hover { background: #444; }

.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin-top: 8px;
  text-align: center;
}
</style>
