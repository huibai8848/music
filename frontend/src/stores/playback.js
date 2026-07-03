import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'

/**
 * 全局播放状态 Store
 *
 * 管理 Audio 实例、播放队列、播放模式、进度、音量等。
 * 配合 PlayerBar.vue 使用，跨页面保持播放状态不中断。
 *
 * 播放模式：
 * - SEQUENCE: 顺序播放，播完停止
 * - LOOP: 列表循环
 * - REPEAT_ONE: 单曲循环
 * - SHUFFLE: 随机播放
 */
export const usePlaybackStore = defineStore('playback', () => {
  // ==================== 状态 ====================

  /** 当前播放歌曲 */
  const currentSong = ref(null)

  /** 播放队列 */
  const queue = ref([])

  /** 当前在队列中的索引 */
  const queueIndex = ref(0)

  /** 播放模式 */
  const mode = ref('SEQUENCE')

  /** 音量 0-1 */
  const volume = ref(parseFloat(localStorage.getItem('player_volume') || '0.8'))

  /** 是否静音 */
  const isMuted = ref(false)

  /** 是否正在播放 */
  const isPlaying = ref(false)

  /** 当前播放进度（毫秒） */
  const currentTime = ref(0)

  /** 总时长（毫秒） */
  const duration = ref(0)

  /** 是否显示播放列表抽屉 */
  const showQueue = ref(false)

  /** 是否显示歌词面板 */
  const showLyrics = ref(false)

  /** 已播放过的歌曲 ID 集合（SHUFFLE 模式用） */
  const playedSet = ref(new Set())

  // ==================== 计算属性 ====================

  /** 当前播放模式的中文名 */
  const modeLabel = computed(() => {
    const map = { SEQUENCE: '顺序播放', LOOP: '列表循环', REPEAT_ONE: '单曲循环', SHUFFLE: '随机播放' }
    return map[mode.value] || '顺序播放'
  })

  /** 当前进度百分比 */
  const progressPercent = computed(() => {
    if (duration.value === 0) return 0
    return (currentTime.value / duration.value) * 100
  })

  // ==================== Audio 管理 ====================

  /** 全局唯一的 Audio 实例 */
  let audioElement = null

  /**
   * 获取或创建全局 Audio 实例
   * 该实例不随页面切换销毁，确保跨页面播放不中断
   */
  function getAudio() {
    if (!audioElement) {
      audioElement = new Audio()
      audioElement.preload = 'auto'

      // 加载元数据后获取总时长
      audioElement.addEventListener('loadedmetadata', () => {
        duration.value = audioElement.duration * 1000
      })

      // 播放结束处理
      audioElement.addEventListener('ended', () => {
        isPlaying.value = false
        next()
      })

      // 时间更新
      audioElement.addEventListener('timeupdate', () => {
        currentTime.value = audioElement.currentTime * 1000
      })

      // 加载错误
      audioElement.addEventListener('error', (e) => {
        console.warn('音频加载失败', e)
        isPlaying.value = false
        // 自动播放下一个
        next()
      })
    }
    return audioElement
  }

  // ==================== 核心操作 ====================

  /**
   * 播放指定歌曲
   * - 如果点击的是队列中已有的歌曲，跳转到对应位置
   * - 否则替换当前歌曲
   *
   * @param {Object} song 歌曲对象 { id, title, artistName, coverUrl, audioUrl, duration }
   * @param {number} indexInQueue 可选，如果在队列中则指定索引
   */
  function play(song, indexInQueue = -1) {
    if (!song || !song.audioUrl) return

    const audio = getAudio()

    // 更新当前歌曲
    currentSong.value = song

    // 如果在队列中指定了索引
    if (indexInQueue >= 0 && indexInQueue < queue.value.length) {
      queueIndex.value = indexInQueue
    } else {
      // 检查是否已在队列中
      const existIdx = queue.value.findIndex(s => s.id === song.id)
      if (existIdx >= 0) {
        queueIndex.value = existIdx
      } else {
        // 不在队列中：插入到当前+1位置
        const insertAt = Math.min(queueIndex.value + 1, queue.value.length)
        queue.value.splice(insertAt, 0, song)
        queueIndex.value = insertAt
      }
    }

    // 设置音频源并播放
    audio.src = song.audioUrl
    audio.play().then(() => {
      isPlaying.value = true
    }).catch(e => {
      console.warn('播放失败', e)
      isPlaying.value = false
    })

    // SHUFFLE 模式：标记当前歌曲为已播放
    if (mode.value === 'SHUFFLE') {
      playedSet.value = new Set(playedSet.value)
      playedSet.value.add(queueIndex.value)
    }

    // 记录播放历史
    recordPlayHistory(song.id)

    // 上报播放计数
    request.post(`/songs/${song.id}/play`).catch(() => {})

    // 查询播放进度
    checkProgress(song.id)
  }

  /**
   * 切换播放/暂停
   */
  function togglePlay() {
    if (!currentSong.value) return
    const audio = getAudio()

    if (isPlaying.value) {
      audio.pause()
      isPlaying.value = false
    } else {
      audio.play().then(() => {
        isPlaying.value = true
      }).catch(e => {
        console.warn('恢复播放失败', e)
      })
    }
  }

  /**
   * 下一首
   */
  function next() {
    if (queue.value.length === 0) return

    let nextIndex

    switch (mode.value) {
      case 'SEQUENCE':
        // 顺序：如果不在末尾则+1，在末尾则停止
        if (queueIndex.value < queue.value.length - 1) {
          nextIndex = queueIndex.value + 1
        } else {
          return // 停在末尾，不自动播放
        }
        break

      case 'LOOP':
        // 列表循环：+1，超长回到0
        nextIndex = (queueIndex.value + 1) % queue.value.length
        break

      case 'REPEAT_ONE':
        // 单曲循环：重新播放当前
        nextIndex = queueIndex.value
        break

      case 'SHUFFLE':
        // 随机：从未播放的歌曲中随机选
        nextIndex = getRandomUnplayed()
        break

      default:
        nextIndex = (queueIndex.value + 1) % queue.value.length
    }

    if (nextIndex !== undefined && nextIndex >= 0) {
      queueIndex.value = nextIndex
      play(queue.value[nextIndex], nextIndex)
    }
  }

  /**
   * 上一首（回到队列前一个位置）
   */
  function prev() {
    if (queue.value.length === 0) return

    // 如果进度超过 3 秒，重新播放当前歌曲
    if (currentTime.value > 3000) {
      seekTo(0)
      return
    }

    let prevIndex = queueIndex.value - 1
    if (prevIndex < 0) {
      prevIndex = 0
    }
    queueIndex.value = prevIndex
    play(queue.value[prevIndex], prevIndex)
  }

  /**
   * 跳转到指定进度
   * @param {number} ms 毫秒
   */
  function seekTo(ms) {
    const audio = getAudio()
    audio.currentTime = ms / 1000
    currentTime.value = ms
  }

  /**
   * 设置音量
   * @param {number} val 0-1
   */
  function setVolume(val) {
    const v = Math.max(0, Math.min(1, val))
    volume.value = v
    const audio = getAudio()
    audio.volume = v
    localStorage.setItem('player_volume', String(v))
  }

  /**
   * 切换静音
   */
  function toggleMute() {
    const audio = getAudio()
    isMuted.value = !isMuted.value
    audio.muted = isMuted.value
  }

  // ==================== 队列管理 ====================

  /**
   * 用一组歌曲替换整个队列
   * @param {Object[]} songs 歌曲列表
   * @param {number} playIndex 要播放的歌曲索引
   */
  function replaceQueue(songs, playIndex = 0) {
    queue.value = [...songs]
    queueIndex.value = playIndex
    playedSet.value = new Set()
    if (songs.length > 0) {
      play(songs[playIndex], playIndex)
    }
  }

  /**
   * 清空队列
   */
  function clearQueue() {
    queue.value = []
    queueIndex.value = 0
    currentSong.value = null
    playedSet.value = new Set()
    const audio = getAudio()
    audio.pause()
    audio.src = ''
    isPlaying.value = false
  }

  /**
   * 移除队列中指定歌曲
   */
  function removeFromQueue(index) {
    if (index < 0 || index >= queue.value.length) return
    queue.value.splice(index, 1)
    if (index === queueIndex.value) {
      // 移除的是当前播放的歌曲
      if (queue.value.length > 0) {
        const newIdx = Math.min(index, queue.value.length - 1)
        queueIndex.value = newIdx
        play(queue.value[newIdx], newIdx)
      } else {
        clearQueue()
      }
    } else if (index < queueIndex.value) {
      queueIndex.value--
    }
  }

  /**
   * 循环切换播放模式
   */
  function cycleMode() {
    const modes = ['SEQUENCE', 'LOOP', 'REPEAT_ONE', 'SHUFFLE']
    const idx = modes.indexOf(mode.value)
    mode.value = modes[(idx + 1) % modes.length]
  }

  // ==================== 私有方法 ====================

  /**
   * SHUFFLE 模式：从未播放过的歌曲中随机选一首
   */
  function getRandomUnplayed() {
    const unplayed = []
    queue.value.forEach((_, idx) => {
      if (!playedSet.value.has(idx)) {
        unplayed.push(idx)
      }
    })

    if (unplayed.length === 0) {
      // 全部播过一轮了，重置
      playedSet.value = new Set()
      return Math.floor(Math.random() * queue.value.length)
    }

    const pick = unplayed[Math.floor(Math.random() * unplayed.length)]
    return pick
  }

  /**
   * 记录播放历史到后端
   */
  function recordPlayHistory(songId) {
    request.post('/history', { songId }).catch(() => {})
  }

  /**
   * 查询播放进度（用于续播提示）
   */
  function checkProgress(songId) {
    request.get(`/progress/${songId}`).then(res => {
      if (res.code === 200 && res.data > 0) {
        // 进度 > 5 秒才提示续播
        if (res.data > 5000) {
          const sec = Math.floor(res.data / 1000)
          const min = Math.floor(sec / 60)
          const s = sec % 60
          if (confirm(`检测到上次听到 ${min}:${s.toString().padStart(2, '0')}，是否续播？`)) {
            seekTo(res.data)
          }
        }
      }
    }).catch(() => {})
  }

  /**
   * 上报进度到后端（每 10 秒由 PlayerBar 定时触发）
   */
  function reportProgress() {
    if (!currentSong.value || !isPlaying.value) return
    request.post('/progress', {
      songId: currentSong.value.id,
      progress: Math.floor(currentTime.value)
    }).catch(() => {})
  }

  // ==================== 持久化 ====================

  /**
   * 从 localStorage 恢复播放状态
   */
  function restoreState() {
    try {
      const saved = localStorage.getItem('playback_queue')
      if (saved) {
        const data = JSON.parse(saved)
        if (data.queue && data.queue.length > 0) {
          queue.value = data.queue
          queueIndex.value = data.queueIndex || 0
          mode.value = data.mode || 'SEQUENCE'
        }
      }
    } catch (e) {
      console.warn('恢复播放状态失败', e)
    }
  }

  /**
   * 保存播放状态到 localStorage
   */
  function saveState() {
    try {
      localStorage.setItem('playback_queue', JSON.stringify({
        queue: queue.value,
        queueIndex: queueIndex.value,
        mode: mode.value
      }))
    } catch (e) {
      // quota exceeded, ignore
    }
  }

  // 初始化
  restoreState()

  return {
    currentSong, queue, queueIndex, mode, volume, isMuted,
    isPlaying, currentTime, duration, showQueue, showLyrics,
    modeLabel, progressPercent,
    play, togglePlay, next, prev, seekTo, setVolume, toggleMute,
    replaceQueue, clearQueue, removeFromQueue, cycleMode,
    reportProgress, saveState
  }
})
