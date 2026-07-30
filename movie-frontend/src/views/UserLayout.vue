<template>
  <div class="user-layout">
    <el-header class="user-header">
      <h1 @click="goHome" style="cursor: pointer;">电影管理系统</h1>
      <div class="header-right">
        <div class="user-info">
          <div v-if="avatar" class="header-avatar" :style="{ backgroundImage: `url(${avatar})` }" @click="goSettings" title="点击更换头像"></div>
          <el-icon v-else :size="22" color="#fff" @click="goSettings" style="cursor: pointer;" title="点击上传头像"><User /></el-icon>
          <span> {{ currentUser }}</span>
        </div>
        <el-button type="text" @click="goSettings">设置</el-button>
        <el-button type="text" @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-main class="user-main">
      <router-view />
    </el-main>
  </div>
</template>

<script>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User } from '@element-plus/icons-vue'

export default {
  name: 'UserLayout',
  components: { User },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const currentUser = ref(sessionStorage.getItem('nickname') || '用户')
    const hasAvatar = ref(sessionStorage.getItem('hasAvatar') === 'true')
    const avatar = ref(hasAvatar.value ? `/api/auth/avatar?username=${sessionStorage.getItem('username')}&v=${Date.now()}` : '')
    
    watch(() => route.path, () => {
      hasAvatar.value = sessionStorage.getItem('hasAvatar') === 'true'
      currentUser.value = sessionStorage.getItem('nickname') || '用户'
      if (hasAvatar.value) {
        avatar.value = `/api/auth/avatar?username=${sessionStorage.getItem('username')}&v=${Date.now()}`
      } else {
        avatar.value = ''
      }
    }, { immediate: true })
    
    const goHome = () => {
      router.push('/user')
    }
    
    const goSettings = () => {
      router.push('/user/settings')
    }
    
    const handleLogout = () => {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('username')
      sessionStorage.removeItem('nickname')
      sessionStorage.removeItem('role')
      sessionStorage.removeItem('avatar')
      sessionStorage.removeItem('hasAvatar')
      router.push('/login')
    }
    
    return {
      currentUser,
      hasAvatar,
      avatar,
      goHome,
      goSettings,
      handleLogout
    }
  }
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.user-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  padding: 0 30px;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.3);
}

.user-header h1 {
  font-size: 24px;
  margin: 0;
  font-weight: 600;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 20px;
  color: white;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  border: 2px solid rgba(255,255,255,0.8);
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.header-avatar:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0,0,0,0.25);
}

.header-right button {
  color: white;
}

.user-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
