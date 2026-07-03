import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
  // 不设置默认 Content-Type，让 Axios 根据请求数据自动设置
  // JSON 数据 → application/json，FormData → multipart/form-data (带 boundary)
})

// 请求拦截器：自动携带 Token
request.interceptors.request.use(
  config => {
    const token = sessionStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：统一错误处理 + Token 过期自动刷新
let isRefreshing = false
let refreshSubscribers = []

function onRefreshed(token) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

function addRefreshSubscriber(cb) {
  refreshSubscribers.push(cb)
}

request.interceptors.response.use(
  response => {
    const data = response.data
    // 业务码非 200 视为错误
    if (data.code && data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  async error => {
    const originalRequest = error.config
    // Token 过期（401）且不是刷新接口
    if (error.response?.status === 401 && !originalRequest._retry) {
      const authStore = useAuthStore()
      const refreshToken = sessionStorage.getItem('refresh_token')

      if (!refreshToken) {
        authStore.logout()
        router.push('/login')
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise(resolve => {
          addRefreshSubscriber(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const res = await axios.post('/api/auth/refresh', { refreshToken })
        const { accessToken } = res.data.data
        sessionStorage.setItem('access_token', accessToken)
        onRefreshed(accessToken)
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return request(originalRequest)
      } catch {
        authStore.logout()
        router.push('/login')
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(error)
  }
)

export default request