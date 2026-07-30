<template>
  <div class="admin-layout">
    <nav class="admin-nav">
      <div class="nav-left">
        <div class="logo"> 电影管理系统</div>
        <ul class="nav-menu">
          <li
            v-for="item in navItems"
            :key="item.path"
            :class="['nav-item', { active: activeMenu === item.path }]"
            @click="switchMenu(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </li>
        </ul>
      </div>
      <div class="nav-right">
        <span class="welcome-text">欢迎, {{ currentUser }}</span>
        <el-button type="danger" size="small" @click="handleLogout" plain>
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </nav>

    <main class="admin-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script>
import { computed, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Film,
  User,
  Avatar,
  Star,
  ChatDotRound,
  Key,
  DataAnalysis,
  UserFilled,
  SwitchButton,
  MagicStick
} from '@element-plus/icons-vue'

export default {
  name: 'AdminLayout',
  components: {
    Film,
    User,
    Avatar,
    Star,
    ChatDotRound,
    Key,
    DataAnalysis,
    UserFilled,
    SwitchButton,
    MagicStick
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const activeMenu = computed(() => route.path)
    const currentUser = computed(() => sessionStorage.getItem('nickname') || '管理员')

    const navItems = [
      { path: '/admin/movies', label: '电影管理', icon: markRaw(Film) },
      { path: '/admin/persons', label: '人员管理', icon: markRaw(User) },
      { path: '/admin/users', label: '评分用户', icon: markRaw(Avatar) },
      { path: '/admin/ratings', label: '评分管理', icon: markRaw(Star) },
      { path: '/admin/comments', label: '评论管理', icon: markRaw(ChatDotRound) },
      { path: '/admin/sys-users', label: '系统用户', icon: markRaw(Key) },
      { path: '/admin/stats-analysis', label: '数据分析', icon: markRaw(DataAnalysis) },
      { path: '/admin/recommendations', label: '用户推荐', icon: markRaw(MagicStick) }
    ]

    const switchMenu = (path) => {
      router.push(path)
    }

    const handleLogout = () => {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('username')
      sessionStorage.removeItem('nickname')
      sessionStorage.removeItem('role')
      router.push('/login')
    }

    return {
      activeMenu,
      currentUser,
      navItems,
      switchMenu,
      handleLogout
    }
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f0f2f5;
}

.admin-nav {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  height: 60px;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.3);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 30px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  white-space: nowrap;
}

.nav-menu {
  list-style: none;
  display: flex;
  gap: 4px;
  margin: 0;
  padding: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s ease;
  font-size: 14px;
  white-space: nowrap;
  color: rgba(255, 255, 255, 0.85);
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.15);
  color: white;
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.25);
  color: white;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-text {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.admin-main {
  padding: 20px;
  min-height: calc(100vh - 60px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
