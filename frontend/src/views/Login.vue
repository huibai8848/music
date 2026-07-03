<template>
  <div class="login-page">
    <div class="auth-card">
      <h1 class="logo">🎵 在线音乐</h1>
      <h2>登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="email" type="email" placeholder="请输入邮箱" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" required />
        </div>
        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
        <button type="submit" :disabled="loading" class="btn-primary">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <p class="switch">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'
import JSEncrypt from 'jsencrypt'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')
const publicKey = ref('')

// 加载时获取 RSA 公钥
onMounted(async () => {
  try {
    const res = await request.get('/auth/public-key')
    if (res.code === 200 && res.data?.publicKey) {
      publicKey.value = res.data.publicKey
    }
  } catch {
    // 公钥加载失败时，使用明文密码（降级方案）
    console.warn('RSA 公钥加载失败，将使用明文传输')
  }
})

// 使用 RSA 公钥加密密码
function encryptPassword(plainPassword) {
  if (!publicKey.value || !plainPassword) return plainPassword
  try {
    const encrypt = new JSEncrypt()
    encrypt.setPublicKey(publicKey.value)
    const encrypted = encrypt.encrypt(plainPassword)
    return encrypted || plainPassword
  } catch {
    return plainPassword
  }
}

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    // 尝试 RSA 加密密码（脱敏传输），如果公钥未加载则使用明文
    const encryptedPwd = encryptPassword(password.value)
    const hasEncryption = encryptedPwd !== password.value && publicKey.value

    if (hasEncryption) {
      // RSA 加密成功，发送加密后的密码
      await authStore.loginWithEncryption(email.value, encryptedPwd)
    } else {
      // 加密不可用，使用明文密码（降级方案）
      await authStore.login(email.value, password.value)
    }
    router.push('/')
  } catch (e) {
    errorMsg.value = e.message || '登录失败，请检查邮箱和密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1565C0 0%, #0D47A1 100%);
}
.auth-card {
  background: #fff; border-radius: 16px; padding: 40px; width: 380px; box-shadow: 0 8px 32px rgba(0,0,0,0.2);
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
.switch { text-align: center; margin-top: 20px; font-size: 14px; color: #999; }
</style>