import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/**
 * 路由配置
 *
 * 路由守卫规则：
 * - requiresAuth=true → 未登录跳转登录页
 * - meta.guest=true   → 已登录跳转首页
 * - 公开页面（songs/albums/artists）无需登录
 */
const routes = [
  // ===== 认证页 =====
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { guest: true }
  },

  // ===== 主页 =====
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },

  // ===== 用户 =====
  {
    path: '/users/:id',
    name: 'UserProfile',
    component: () => import('../views/UserProfile.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },

  // ===== 音乐浏览（无需登录） =====
  {
    path: '/songs',
    name: 'SongList',
    component: () => import('../views/SongList.vue')
  },
  {
    path: '/songs/:id',
    name: 'SongDetail',
    component: () => import('../views/SongDetail.vue')
  },
  {
    path: '/albums',
    name: 'AlbumList',
    component: () => import('../views/AlbumList.vue')
  },
  {
    path: '/albums/:id',
    name: 'AlbumDetail',
    component: () => import('../views/AlbumDetail.vue')
  },
  {
    path: '/artists',
    name: 'ArtistList',
    component: () => import('../views/ArtistList.vue')
  },
  {
    path: '/artists/:id',
    name: 'ArtistDetail',
    component: () => import('../views/ArtistDetail.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('../views/Search.vue')
  },

  // ===== 排行榜 =====
  {
    path: '/rankings',
    name: 'Rankings',
    component: () => import('../views/Rankings.vue')
  },

  // ===== 歌单 =====
  {
    path: '/playlists',
    name: 'PlaylistList',
    component: () => import('../views/PlaylistList.vue')
  },
  {
    path: '/playlists/:id',
    name: 'PlaylistDetail',
    component: () => import('../views/PlaylistDetail.vue')
  },

  // ===== 歌房 =====
  {
    path: '/rooms',
    name: 'RoomList',
    component: () => import('../views/RoomList.vue')
  },
  {
    path: '/rooms/:id',
    name: 'RoomDetail',
    component: () => import('../views/RoomDetail.vue'),
    meta: { requiresAuth: true }
  },

  // ===== 通知 =====
  {
    path: '/notifications',
    name: 'NotificationList',
    component: () => import('../views/NotificationList.vue'),
    meta: { requiresAuth: true }
  },

  // ===== 管理后台 =====
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../views/AdminDashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },

  // ===== 音乐上传（统一提交文件+元数据，替代旧版 /files/upload 散件上传） =====
  {
    path: '/upload-music',
    name: 'MusicUpload',
    component: () => import('../views/MusicUpload.vue'),
    meta: { requiresAuth: true }
  },

  // ===== 兜底 404 =====
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 需要登录但未登录 → 跳转登录页
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return next('/login')
  }

  // 需要管理员权限但不是管理员 → 跳首页
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return next('/')
  }

  // 访客页面（登录/注册）但已登录 → 跳首页
  if (to.meta.guest && authStore.isLoggedIn) {
    return next('/')
  }

  // 未登录访问公开页面 → 正常放行（公开页面不需要登录）
  next()
})

// 全局后置守卫：页面切换后刷新用户数据，保证数据实时性
router.afterEach(async (to, from) => {
  // 仅在路由确实发生变化时刷新用户数据
  if (to.path !== from.path) {
    const authStore = useAuthStore()
    // 已登录的用户，在进入需要登录的页面时刷新用户信息
    if (authStore.isLoggedIn && to.meta.requiresAuth) {
      authStore.fetchUser()
    }
  }
})

export default router
