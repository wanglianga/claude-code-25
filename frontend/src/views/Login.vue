<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="title">
        <el-icon :size="30" color="#409eff"><School /></el-icon>
        <h2>城市托育中心</h2>
        <p>入托评估与接送交接平台</p>
      </div>
      <el-form :model="form" @keyup.enter="doLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock"
                    size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doLogin">
          登 录
        </el-button>
      </el-form>
      <el-divider content-position="center">演示账号（密码均为 123456）</el-divider>
      <div class="demo-accounts">
        <el-tag v-for="acc in demoAccounts" :key="acc.username" class="acc" @click="fill(acc)">
          {{ acc.label }} {{ acc.username }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const demoAccounts = [
  { label: '园长', username: 'director' },
  { label: '保健老师', username: 'health' },
  { label: '老师', username: 'teacher1' },
  { label: '老师2', username: 'teacher2' },
  { label: '前台', username: 'frontdesk' },
  { label: '厨房', username: 'kitchen' },
  { label: '家长1', username: 'parent1' },
  { label: '家长2', username: 'parent2' },
  { label: '家长3', username: 'parent3' }
]

function fill(acc) {
  form.username = acc.username
  form.password = '123456'
}

async function doLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #74b9ff 0%, #a29bfe 100%);
}
.login-card { width: 420px; border-radius: 12px; }
.title { text-align: center; margin-bottom: 20px; }
.title h2 { margin: 8px 0 4px; color: #303133; }
.title p { margin: 0; color: #909399; font-size: 13px; }
.demo-accounts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.acc { cursor: pointer; }
</style>
