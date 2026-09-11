<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <b>异常预警（同一儿童多次异常时自动生成）</b>
          <el-radio-group v-model="statusFilter" size="small" @change="load">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="OPEN">待处理</el-radio-button>
            <el-radio-button value="HANDLED">已处理</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="alerts" v-loading="loading">
        <el-table-column label="儿童" width="100">
          <template #default="{ row }">{{ row.child?.name }}</template>
        </el-table-column>
        <el-table-column label="班级" width="100">
          <template #default="{ row }">{{ row.child?.classroom?.name || '—' }}</template>
        </el-table-column>
        <el-table-column label="异常次数" width="100">
          <template #default="{ row }">
            <el-tag type="danger" effect="plain">{{ row.anomalyCount }} 次 / {{ row.windowDays }} 天</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="系统建议" width="160">
          <template #default="{ row }">
            <el-tag :type="dType('suggestion', row.suggestion)">{{ d('suggestion', row.suggestion) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="统计明细" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="dType('alertStatus', row.status)" size="small">{{ d('alertStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="生成时间" width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/timeline?childId=${row.child.id}`)">时间轴</el-button>
            <el-button v-if="row.status === 'OPEN' && isDirector" link type="danger"
                       @click="openHandle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="handleVisible" title="处理预警" width="480px" destroy-on-close>
      <el-alert type="warning" :closable="false" class="mb12">
        <template #title>{{ current?.child?.name }}｜{{ d('suggestion', current?.suggestion) }}</template>
        {{ current?.reason }}
      </el-alert>
      <el-input v-model="handleNote" type="textarea" :rows="4"
                placeholder="处理说明，如：已调整至XX班 / 已安排保健老师每日观察 / 已约谈家长" />
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import http from '../../api'
import { d, dType } from '../../utils/dicts'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const isDirector = computed(() => auth.role === 'DIRECTOR')
const alerts = ref([])
const loading = ref(false)
const statusFilter = ref('')
const handleVisible = ref(false)
const current = ref(null)
const handleNote = ref('')

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loading.value = true
  try {
    alerts.value = await http.get('/alerts', { params: statusFilter.value ? { status: statusFilter.value } : {} })
  } finally {
    loading.value = false
  }
}

function openHandle(row) {
  current.value = row
  handleNote.value = ''
  handleVisible.value = true
}

async function submitHandle() {
  if (!handleNote.value.trim()) return ElMessage.warning('请填写处理说明')
  await http.post(`/alerts/${current.value.id}/handle`, { handleNote: handleNote.value })
  ElMessage.success('预警已处理')
  handleVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.mb12 { margin-bottom: 12px; }
</style>
