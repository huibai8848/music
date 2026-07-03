<template>
  <div class="disc-container" :class="{ spinning: isPlaying, paused: !isPlaying }">
    <!-- 黑胶碟片 -->
    <div class="disc">
      <!-- 封面图作为碟片标签 -->
      <div class="disc-label" :style="{ backgroundImage: coverUrl ? `url(${coverUrl})` : 'none' }">
        <span v-if="!coverUrl" class="disc-placeholder">🎵</span>
      </div>
    </div>
    <!-- 唱针 -->
    <div class="tonearm" :class="{ playing: isPlaying }">
      <div class="tonearm-head"></div>
      <div class="tonearm-body"></div>
    </div>
  </div>
</template>

<script setup>
/**
 * 黑胶碟片旋转动画组件
 *
 * 模拟黑胶唱机播放效果：
 * - 封面图显示在碟片标签位置
 * - 播放时碟片匀速旋转，唱针落下
 * - 暂停时碟片停转，唱针抬起
 * - 无歌曲时显示默认占位符
 */
defineProps({
  /** 封面图 URL */
  coverUrl: { type: String, default: '' },
  /** 是否正在播放 */
  isPlaying: { type: Boolean, default: false }
})
</script>

<style scoped>
.disc-container {
  position: relative;
  width: 200px;
  height: 200px;
  margin: 0 auto;
}

/* 黑胶碟片 */
.disc {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: conic-gradient(from 0deg, #222, #444, #222, #444, #222);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
  animation: spin 4s linear infinite;
  animation-play-state: paused;
}

.disc-container.spinning .disc {
  animation-play-state: running;
}

/* 碟片中心标签（封面图位置） */
.disc-label {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  background-color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(255,255,255,0.1);
}

.disc-placeholder {
  font-size: 32px;
  opacity: 0.5;
}

/* 唱针 */
.tonearm {
  position: absolute;
  top: -15px;
  right: -30px;
  width: 60px;
  height: 80px;
  transform-origin: top right;
  transition: transform 0.5s ease;
  transform: rotate(-30deg) translateX(10px);
}

.tonearm.playing {
  transform: rotate(0deg) translateX(0);
}

.tonearm-head {
  width: 12px;
  height: 12px;
  background: #888;
  border-radius: 50%;
  position: absolute;
  bottom: 0;
  right: 0;
}

.tonearm-body {
  width: 4px;
  height: 70px;
  background: linear-gradient(to bottom, #999, #666);
  position: absolute;
  bottom: 6px;
  right: 4px;
  transform: rotate(15deg);
  transform-origin: bottom right;
  border-radius: 2px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
