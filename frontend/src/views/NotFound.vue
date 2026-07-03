<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <router-link to="/" class="back-link">← 返回首页</router-link>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: #999;
}
.not-found h1 { font-size: 72px; color: #ddd; margin-bottom: 8px; }
.not-found p { font-size: 18px; margin-bottom: 24px; }
.back-link { color: #1565C0; font-size: 14px; }
</style>
