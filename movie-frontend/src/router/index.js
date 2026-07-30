import { createRouter, createWebHistory } from 'vue-router'
import MovieManagement from '../views/MovieManagement.vue'
import PersonManagement from '../views/PersonManagement.vue'
import UserManagement from '../views/UserManagement.vue'
import RatingManagement from '../views/RatingManagement.vue'
import CommentManagement from '../views/CommentManagement.vue'
import SystemUserManagement from '../views/SystemUserManagement.vue'
import AdminStatsAnalysis from '../views/AdminStatsAnalysis.vue'
import UserProfileAnalysis from '../views/UserProfileAnalysis.vue'
import LoginRegister from '../views/LoginRegister.vue'
import UserDashboard from '../views/UserDashboard.vue'
import UserSettings from '../views/UserSettings.vue'
import AdminLayout from '../views/AdminLayout.vue'
import UserLayout from '../views/UserLayout.vue'
import AdminRecommendations from '../views/AdminRecommendations.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'LoginRegister',
    component: LoginRegister
  },
  // 用户布局路由
  {
    path: '/user',
    component: UserLayout,
    meta: { requireAuth: true, role: 'USER' },
    children: [
      {
        path: '',
        name: 'UserDashboard',
        component: UserDashboard
      },
      {
        path: 'settings',
        name: 'UserSettings',
        component: UserSettings
      }
    ]
  },
  // 管理员布局路由
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requireAuth: true, role: 'ADMIN' },
    children: [
      {
        path: 'movies',
        name: 'MovieManagement',
        component: MovieManagement
      },
      {
        path: 'persons',
        name: 'PersonManagement',
        component: PersonManagement
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: UserManagement
      },
      {
        path: 'ratings',
        name: 'RatingManagement',
        component: RatingManagement
      },
      {
        path: 'comments',
        name: 'CommentManagement',
        component: CommentManagement
      },
      {
        path: 'sys-users',
        name: 'SystemUserManagement',
        component: SystemUserManagement
      },
      {
        path: 'stats-analysis',
        name: 'AdminStatsAnalysis',
        component: AdminStatsAnalysis
      },
      {
        path: 'user-profile',
        redirect: 'stats-analysis'
      },
      {
        path: 'recommendations',
        name: 'AdminRecommendations',
        component: AdminRecommendations
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const isLoggedIn = sessionStorage.getItem('token')
  const userRole = sessionStorage.getItem('role')
  
  if (to.path === '/login') {
    next()
    return
  }
  
  if (!isLoggedIn) {
    next('/login')
    return
  }
  
  let routeRole = to.meta.role
  if (!routeRole && to.matched.length > 0) {
    for (let i = to.matched.length - 1; i >= 0; i--) {
      if (to.matched[i].meta && to.matched[i].meta.role) {
        routeRole = to.matched[i].meta.role
        break
      }
    }
  }
  
  if (routeRole === 'ADMIN' && userRole !== 'ADMIN') {
    next('/user')
    return
  }
  
  if (routeRole === 'USER' && userRole === 'ADMIN') {
    next()
    return
  }
  
  next()
})

export default router
