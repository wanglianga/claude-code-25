<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22" color="#fff"><School /></el-icon>
        <span>托育中心平台</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d" text-color="#bfcbd9"
               active-text-color="#ffd04b" class="menu">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon><span>工作台</span>
        </el-menu-item>
        <template v-for="item in visibleMenus" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span>
          </el-menu-item>
        </template>
        <el-menu-item index="/events">
          <el-icon><BellFilled /></el-icon><span>协同事件</span>
        </el-menu-item>
        <el-menu-item v-if="role !== 'PARENT'" index="/timeline">
          <el-icon><Timer /></el-icon><span>时间轴</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="breadcrumb">{{ $route.meta.title || '工作台' }}</div>
        <div class="user">
          <el-tag size="small" effect="plain">{{ auth.roleName }}</el-tag>
          <span class="name">{{ auth.user?.name }}</span>
          <el-button link type="danger" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const router = useRouter()
const role = computed(() => auth.role)

const menus = [
  { path: '/parent/enrollment', title: '入托申请', icon: 'DocumentAdd', roles: ['PARENT'] },
  { path: '/director/tasks', title: '入托评估任务', icon: 'Checked', roles: ['DIRECTOR'] },
  { path: '/director/classes', title: '班级管理', icon: 'School', roles: ['DIRECTOR'] },
  { path: '/director/alerts', title: '异常预警', icon: 'WarningFilled', roles: ['DIRECTOR', 'HEALTH'] },
  { path: '/health/tasks', title: '健康评估', icon: 'FirstAidKit', roles: ['HEALTH'] },
  { path: '/teacher/morning-check', title: '每日晨检', icon: 'Sunny', roles: ['TEACHER'] },
  { path: '/teacher/day-care', title: '在园照护记录', icon: 'Notebook', roles: ['TEACHER'] },
  { path: '/frontdesk/pickup', title: '接送核验', icon: 'Van', roles: ['FRONTDESK'] }
]

const visibleMenus = computed(() => menus.filter((m) => m.roles.includes(role.value)))

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #1f2d3d; }
.logo {
  height: 60px; display: flex; align-items: center; justify-content: center;
  gap: 8px; color: #fff; font-size: 16px; font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.menu { border-right: none; }
.header {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
}
.breadcrumb { font-size: 16px; font-weight: 600; color: #303133; }
.user { display: flex; align-items: center; gap: 10px; }
.user .name { font-weight: 500; }
.main { padding: 16px; overflow-y: auto; }
</style>
