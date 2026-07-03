import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const accessToken = ref(sessionStorage.getItem('access_token') || '')
  const refreshToken = ref(sessionStorage.getItem('refresh_token') || '')

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  /** 是否为 VIP 会员（role="VIP" 且 vipExpireTime 未过期） */
  const isVip = computed(() => {
    if (!user.value) return false
    if (user.value.role === 'VIP') {
      // 如果有过期时间字段且已过期则视为非 VIP
      if (user.value.vipExpireTime) {
        return new Date(user.value.vipExpireTime) > new Date()
      }
      return true
    }
    return false
  })

  // 登录（明文密码）
  async function login(email, password) {
    const res = await request.post('/auth/login', { email, password })
    if (res.code === 200 && res.data) {
      accessToken.value = res.data.accessToken
      refreshToken.value = res.data.refreshToken
      user.value = res.data.user
      sessionStorage.setItem('access_token', res.data.accessToken)
      sessionStorage.setItem('refresh_token', res.data.refreshToken)
      sessionStorage.setItem('user', JSON.stringify(res.data.user))
    }
    return res
  }

  // 登录（RSA 加密密码脱敏传输）
  async function loginWithEncryption(email, encryptedPassword) {
    const res = await request.post('/auth/login', {
      email,
      encryptedPassword // 前端已 RSA 加密，后端自动解密
    })
    if (res.code === 200 && res.data) {
      accessToken.value = res.data.accessToken
      refreshToken.value = res.data.refreshToken
      user.value = res.data.user
      sessionStorage.setItem('access_token', res.data.accessToken)
      sessionStorage.setItem('refresh_token', res.data.refreshToken)
      sessionStorage.setItem('user', JSON.stringify(res.data.user))
    }
    return res
  }

  // 注册（仅注册，不自动登录）
  async function register(email, password, nickname) {
    const res = await request.post('/auth/register', { email, password, nickname })
    return res
  }

  // 登出
  function logout() {
    try { request.post('/auth/logout') } catch {}
    user.value = null
    accessToken.value = ''
    refreshToken.value = ''
    sessionStorage.removeItem('access_token')
    sessionStorage.removeItem('refresh_token')
    sessionStorage.removeItem('user')
  }

  // 获取当前用户（若 Token 无效则清除登录状态）
  async function fetchUser() {
    try {
      const res = await request.get('/users/me')
      if (res.code === 200 && res.data) {
        user.value = res.data
        sessionStorage.setItem('user', JSON.stringify(res.data))
        return true
      } else {
        // Token 无效，清除登录状态
        clearAuth()
        return false
      }
    } catch {
      // Token 过期或无效，清除登录状态
      clearAuth()
      return false
    }
  }

  // 清除所有认证状态
  function clearAuth() {
    user.value = null
    accessToken.value = ''
    refreshToken.value = ''
    sessionStorage.removeItem('access_token')
    sessionStorage.removeItem('refresh_token')
    sessionStorage.removeItem('user')
  }

  // 初始化：从 sessionStorage 恢复，并验证 Token 有效性
  async function init() {
    const saved = sessionStorage.getItem('user')
    const token = sessionStorage.getItem('access_token')
    if (token && saved) {
      try {
        user.value = JSON.parse(saved)
        accessToken.value = token
        refreshToken.value = sessionStorage.getItem('refresh_token') || ''
        // 异步验证 Token 有效性
        await fetchUser()
      } catch {
        clearAuth()
      }
    } else if (!token) {
      clearAuth()
    }
  }

  return {
    user, accessToken, refreshToken,
    isLoggedIn, isAdmin, isVip,
    login, loginWithEncryption, register, logout,
    fetchUser, init, clearAuth
  }
})