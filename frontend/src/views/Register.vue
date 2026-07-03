<template>
  <div class="register-page">
    <div class="auth-card">
      <h1 class="logo">🎵 在线音乐</h1>
      <h2>注册</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="email" type="email" placeholder="请输入邮箱" required />
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input v-model="nickname" type="text" placeholder="请输入昵称" required maxlength="50" />
        </div>
        <div class="form-group">
          <label>密码（8-20位，含字母和数字）</label>
          <input v-model="password" type="password" placeholder="请输入密码" required />
        </div>
        <div class="form-group">
          <label>确认密码</label>
          <input v-model="confirmPassword" type="password" placeholder="再次输入密码" required />
        </div>
        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
        <p v-if="successMsg" class="success">{{ successMsg }}</p>
        <button type="submit" :disabled="loading" class="btn-primary">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <p class="switch">已有账号？<router-link to="/login">立即登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

async function handleRegister() {
  errorMsg.value = ''
  successMsg.value = ''

  if (password.value !== confirmPassword.value) {
    errorMsg.value = '两次密码输入不一致'
    return
  }
  if (password.value.length < 8 || password.value.length > 20) {
    errorMsg.value = '密码长度需在 8-20 位之间'
    return
  }
  if (!/(?=.*[a-zA-Z])(?=.*\d)/.test(password.value)) {
    errorMsg.value = '密码需包含字母和数字'
    return
  }

  loading.value = true
  try {
    const res = await authStore.register(email.value, password.value, nickname.value)
    if (res.code === 200) {
      successMsg.value = '✅ 注册成功！即将跳转到登录页...'
      setTimeout(() => router.push('/login'), 1500)
    } else {
      errorMsg.value = res.message || '注册失败'
    }
  } catch (e) {
    errorMsg.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1565C0 0%, #0D47A1 100%);
}
.auth-card {
  background: #fff; border-radius: 16px; padding: 40px; width: 400px; box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.logo { text-align: center; margin-bottom: 20px; font-size: 24px; color: #1565C0; }
h2 { text-align: center; margin-bottom: 24px; color: #333; font-size: 20px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; color: #666; }
.form-group input {
  width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px;
  outline: none; transition: border-color 0.2s;
}
.form-group input:focus { border-color: #1565C0; }
.btn-primary {
  width: 100%; padding: 12px; background: #1565C0; color: #fff; border: none; border-radius: 8px;
  font-size: 16px; cursor: pointer; transition: background 0.2s; margin-top: 8px;
}
.btn-primary:hover { background: #0D47A1; }
.btn-primary:disabled { background: #90CAF9; cursor: not-allowed; }
.error { color: #E53935; font-size: 13px; margin-bottom: 8px; text-align: center; }
.success { color: #43A047; font-size: 13px; margin-bottom: 8px; text-align: center; }
.switch { text-align: center; margin-top: 20px; font-size: 14px; color: #999; }
</style>