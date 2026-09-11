<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card" @click="card.to && $router.push(card.to)">
          <div class="stat-num" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mt16">
      <template #header><b>快速入口</b></template>
      <el-space wrap>
        <el-button v-for="q in quickLinks" :key="q.to" type="primary" plain @click="$router.push(q.to)">
          {{ q.label }}
        </el-button>
      </el-space>
      <el-alert class="mt16" type="info" :closable="false" show-icon
                title="平台流程：家长提交入托申请 → 园长分班 → 保健老师健康评估 → 每日晨检/在园照护 → 前台接送核验；异常自动创建协同事件并生成预警。" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '../api'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const stats = ref({})

const cards = computed(() => {
  const s = stats.value
  const role = auth.role
  if (role === 'DIRECTOR') {
    return [
      { label: '待分班申请', value: s.pendingClassApps ?? '-', color: '#e6a23c', to: '/director/tasks' },
      { label: '待健康评估', value: s.pendingHealthApps ?? '-', color: '#409eff', to: '/director/tasks' },
      { label: '待处理预警', value: s.openAlerts ?? '-', color: '#f56c6c', to: '/director/alerts' },
      { label: '进行中事件', value: s.openEvents ?? '-', color: '#f56c6c', to: '/events' },
      { label: '今日替换餐', value: s.todayMealSubs ?? '-', color: '#b88230', to: '/timeline' },
      { label: '在册儿童', value: s.totalChildren ?? '-', color: '#67c23a' },
      { label: '班级数', value: s.totalClasses ?? '-', color: '#909399', to: '/director/classes' }
    ]
  }
  if (role === 'HEALTH') {
    return [
      { label: '待健康评估', value: s.pendingHealthApps ?? '-', color: '#e6a23c', to: '/health/tasks' },
      { label: '待确认替换餐', value: s.pendingMealSubs ?? '-', color: '#b88230', to: '/health/meal-subs' },
      { label: '今日晨检异常', value: s.todayAbnormalChecks ?? '-', color: '#f56c6c', to: '/health/tasks' },
      { label: '进行中事件', value: s.openEvents ?? '-', color: '#f56c6c', to: '/events' }
    ]
  }
  if (role === 'KITCHEN') {
    return [
      { label: '今日菜单', value: s.todayMenus ?? '-', color: '#409eff', to: '/kitchen/menus' },
      { label: '今日替换单', value: s.todayMealSubs ?? '-', color: '#b88230', to: '/kitchen/substitutions' },
      { label: '待出餐', value: s.toExecute ?? '-', color: '#e6a23c', to: '/kitchen/substitutions' },
      { label: '今日已出餐', value: s.executedToday ?? '-', color: '#67c23a', to: '/kitchen/substitutions' }
    ]
  }
  if (role === 'TEACHER') {
    return [
      { label: '本班儿童', value: s.myClassChildren ?? '-', color: '#409eff' },
      { label: '今日已晨检', value: s.todayChecked ?? '-', color: '#67c23a', to: '/teacher/morning-check' },
      { label: '今日待晨检', value: s.todayUnchecked ?? '-', color: '#e6a23c', to: '/teacher/morning-check' },
      { label: '待确认替换餐', value: s.pendingMealSubs ?? '-', color: '#b88230', to: '/teacher/meal-serving' },
      { label: '人工照护提示', value: s.manualCare ?? '-', color: '#f56c6c', to: '/teacher/meal-serving' },
      { label: '待分餐', value: s.toServe ?? '-', color: '#e6a23c', to: '/teacher/meal-serving' },
      { label: '我参与的事件', value: s.myOpenEvents ?? '-', color: '#f56c6c', to: '/events' }
    ]
  }
  if (role === 'FRONTDESK') {
    return [
      { label: '今日接送记录', value: s.todayPickups ?? '-', color: '#409eff', to: '/frontdesk/pickup' },
      { label: '今日拒绝放行', value: s.todayDenied ?? '-', color: '#f56c6c', to: '/frontdesk/pickup' },
      { label: '今日临时委托', value: s.todayDelegations ?? '-', color: '#e6a23c', to: '/frontdesk/pickup' },
      { label: '进行中事件', value: s.openEvents ?? '-', color: '#f56c6c', to: '/events' }
    ]
  }
  return [
    { label: '我的孩子', value: s.childrenCount ?? '-', color: '#409eff', to: '/parent/enrollment' },
    { label: '待确认替换餐', value: s.pendingMealSubs ?? '-', color: '#b88230', to: '/parent/meal-subs' },
    { label: '待补充资料', value: s.needSupplement ?? '-', color: '#e6a23c', to: '/parent/enrollment' },
    { label: '进行中的事件', value: s.myOpenEvents ?? '-', color: '#f56c6c', to: '/events' }
  ]
})

const quickLinks = computed(() => {
  const map = {
    DIRECTOR: [
      { to: '/director/tasks', label: '处理入托评估任务' },
      { to: '/director/classes', label: '班级与师资' },
      { to: '/director/alerts', label: '异常预警' },
      { to: '/timeline', label: '时间轴' }
    ],
    HEALTH: [
      { to: '/health/tasks', label: '健康评估任务' },
      { to: '/health/meal-subs', label: '替换餐确认' },
      { to: '/director/alerts', label: '异常预警' },
      { to: '/events', label: '协同事件' }
    ],
    KITCHEN: [
      { to: '/kitchen/menus', label: '发布每日菜单' },
      { to: '/kitchen/substitutions', label: '替换餐执行' }
    ],
    TEACHER: [
      { to: '/teacher/morning-check', label: '今日晨检' },
      { to: '/teacher/day-care', label: '在园照护记录' },
      { to: '/teacher/meal-serving', label: '分餐与替换餐' },
      { to: '/events', label: '协同事件' }
    ],
    FRONTDESK: [
      { to: '/frontdesk/pickup', label: '接送核验' },
      { to: '/events', label: '协同事件' }
    ],
    PARENT: [
      { to: '/parent/enrollment', label: '入托申请/我的孩子' },
      { to: '/parent/meal-subs', label: '替换餐确认' },
      { to: '/events', label: '协同事件' }
    ]
  }
  return map[auth.role] || []
})

onMounted(async () => {
  stats.value = await http.get('/dashboard')
})
</script>

<style scoped>
.stat-card { cursor: pointer; text-align: center; margin-bottom: 16px; }
.stat-num { font-size: 30px; font-weight: 700; }
.stat-label { color: #909399; margin-top: 4px; }
.mt16 { margin-top: 8px; }
</style>
