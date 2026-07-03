<template>
  <div class="lyrics-panel" ref="panelRef">
    <!-- 加载中 -->
    <div v-if="loading" class="lyrics-status">加载歌词中...</div>

    <!-- 暂无歌词 -->
    <div v-else-if="lyrics.length === 0" class="lyrics-status">暂无歌词</div>

    <!-- 歌词列表 -->
    <div v-else class="lyrics-list" ref="listRef">
      <div
        v-for="(line, idx) in lyrics"
        :key="idx"
        class="lyric-line"
        :class="{ active: idx === activeIndex }"
        @click="jumpTo(line.time)"
        :ref="idx === activeIndex ? 'activeLineRef' : null"
      >
        {{ line.text || '...' }}
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 歌词面板组件
 *
 * 功能：
 * 1. 请求后端 API 获取解析后的歌词 JSON
 * 2. 根据 Audio.currentTime 定位当前歌词行
 * 3. 高亮当前行 + 自动滚动提前 2 行
 * 4. 点击歌词行跳转到对应进度
 */
import { ref, watch, nextTick, computed } from 'vue'
import request from '../utils/request'

const props = defineProps({
  /** 歌曲 ID */
  songId: { type: Number, default: null },
  /** 当前播放进度（毫秒） */
  currentTime: { type: Number, default: 0 }
})

const emit = defineEmits(['seek'])

/** 歌词数据 [{time: 秒, text: 字符串}] */
const lyrics = ref([])
const loading = ref(false)

/** 当前激活的歌词行索引 */
const activeIndex = computed(() => {
  if (lyrics.value.length === 0) return -1
  const seconds = props.currentTime / 1000

  // 从后往前找到第一个时间小于当前时间的行
  for (let i = lyrics.value.length - 1; i >= 0; i--) {
    if (lyrics.value[i].time <= seconds) {
      return i
    }
  }
  return -1
})

const panelRef = ref(null)
const listRef = ref(null)

// 监听 activeIndex 变化，自动滚动
watch(activeIndex, (newIdx) => {
  if (newIdx < 0 || !listRef.value) return
  nextTick(() => {
    const lines = listRef.value.querySelectorAll('.lyric-line')
    if (lines[newIdx]) {
      // 滚动到当前行提前 2 行的位置，使其居中偏下
      const targetLine = lines[Math.max(0, newIdx - 2)]
      targetLine.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  })
})

// 监听 songId 变化，加载歌词
watch(() => props.songId, (newId) => {
  if (newId) {
    loadLyrics(newId)
  } else {
    lyrics.value = []
  }
}, { immediate: true })

/**
 * 从后端加载解析后的歌词 JSON
 */
async function loadLyrics(songId) {
  loading.value = true
  try {
    const res = await request.get(`/songs/${songId}/lyrics`)
    if (res.code === 200 && res.data) {
      let data = res.data
      // 如果返回的是字符串 JSON，需要解析
      if (typeof data === 'string') {
        try { data = JSON.parse(data) } catch {}
      }
      if (Array.isArray(data)) {
        lyrics.value = data
      } else {
        lyrics.value = []
      }
    } else {
      lyrics.value = []
    }
  } catch {
    lyrics.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 点击歌词行跳转到对应时间
 */
function jumpTo(time) {
  emit('seek', time * 1000)
}
</script>

<style scoped>
.lyrics-panel {
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.lyrics-status {
  color: #999;
  font-size: 14px;
  text-align: center;
}

.lyrics-list {
  width: 100%;
  max-height: 100%;
  overflow-y: auto;
  padding: 60px 0;
  scroll-behavior: smooth;
}

/* 隐藏滚动条但保持可滚动 */
.lyrics-list::-webkit-scrollbar { width: 0; }

.lyric-line {
  text-align: center;
  padding: 10px 20px;
  font-size: 16px;
  color: #999;
  cursor: pointer;
  transition: all 0.3s ease;
  line-height: 1.6;
}

.lyric-line:hover {
  color: #ccc;
  transform: scale(1.02);
}

.lyric-line.active {
  color: #1565C0;
  font-size: 20px;
  font-weight: 600;
  transform: scale(1.05);
}
</style>
