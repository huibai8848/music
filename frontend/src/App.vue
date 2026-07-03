<template>
  <div id="app-container" v-if="appReady">
    <!-- 全局顶部导航栏 -->
    <NavBar />
    <!-- 页面内容区域 -->
    <router-view />
    <!-- 全局底部播放条（在所有页面底部固定） -->
    <PlayerBar />
  </div>
  <div v-else class="app-loading">
    <div class="loading-spinner"></div>
    <p>加载中...</p>
  </div>
</template>

<script setup>
/**
 * 根组件
 *
 * 全局结构：
 * - NavBar：顶部导航栏（包含通知/用户菜单）
 * - router-view：页面内容区域
 * - PlayerBar：底部固定播放条（全局单例）
 *
 * 页面初始化时恢复登录状态
 */
import { ref, onMounted } from 'vue'
import { useAuthStore } from './stores/auth'
import NavBar from './components/NavBar.vue'
import PlayerBar from './components/PlayerBar.vue'

const authStore = useAuthStore()
const appReady = ref(false)

onMounted(async () => {
  // 尝试从 sessionStorage 恢复登录状态，同时验证 Token 有效性
  // 防止无效 Token 导致打开他人账号
  await authStore.init()
  appReady.value = true
})
</script>

<style>
/* 全局基础样式 */
* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
  background: #f5f5f5;
  color: #333;
  -webkit-font-smoothing: antialiased;
}

a {
  text-decoration: none;
  color: #1565C0;
}

/* 页面内容顶部留白（给全局导航栏让位）+ 底部留白给 PlayerBar */
#app-container {
  padding-top: 0;   /* NavBar 是 sticky，无需额外留白 */
  padding-bottom: 72px;  /* PlayerBar 高度 64px + 8px 间距 */
}

/* 通用按钮样式 */
button {
  cursor: pointer;
  font-family: inherit;
}

/* 应用加载中 */
.app-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  color: #999;
  font-size: 14px;
  gap: 16px;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e0e0e0;
  border-top-color: #1565C0;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 滚动条美化 */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #ccc; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #999; }

/* ===== 响应式设计：移动端/平板/桌面三档断点 ===== */

/* 平板及以下 (< 768px) */
@media (max-width: 768px) {
  #app-container { padding-bottom: 64px; }

  .song-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)) !important;
    gap: 10px !important;
  }
  .playlist-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)) !important;
    gap: 10px !important;
  }
}

/* 手机 (< 480px) */
@media (max-width: 480px) {
  #app-container { padding-bottom: 56px; }

  .song-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
  .playlist-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }

  .quick-nav {
    flex-wrap: wrap !important;
    gap: 8px !important;
  }
  .quick-item {
    min-width: 60px !important;
    padding: 8px 12px !important;
    font-size: 12px !important;
  }
  .quick-icon { font-size: 20px !important; }

  .banner-slide img { height: 140px !important; }
}
</style>