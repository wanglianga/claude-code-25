<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <b>协同事件</b>
          <el-space>
            <el-select v-model="typeFilter" placeholder="类型" clearable size="small" style="width: 140px"
                       @change="load">
              <el-option v-for="(v, k) in dicts.eventType" :key="k" :label="v.label" :value="k" />
            </el-select>
            <el-radio-group v-model="statusFilter" size="small" @change="load">
              <el-radio-button value="">全部</el-radio-button>
              <el-radio-button value="OPEN">待处理</el-radio-button>
              <el-radio-button value="PROCESSING">处理中</el-radio-button>
              <el-radio-button value="RESOLVED">已解决</el-radio-button>
              <el-radio-button value="CLOSED">已关闭</el-radio-button>
            </el-radio-group>
            <el-button v-if="isStaff" type="primary" size="small" :icon="Plus" @click="openCreate">上报事件</el-button>
          </el-space>
        </div>
      </template>
      <el-table :data="events" v-loading="loading" @row-click="(row) => $router.push(`/events/${row.id}`)"
                highlight-current-row style="cursor: pointer">
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="dType('eventType', row.type)" size="small">{{ d('eventType', row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column label="儿童" width="90">
          <template #default="{ row }">{{ row.child?.name }}</template>
        </el-table-column>
        <el-table-column label="班级" width="90">
          <template #default="{ row }">{{ row.classroom?.name || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="dType('eventStatus', row.status)" size="small">{{ d('eventStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建人" width="90">
          <template #default="{ row }">{{ row.createdBy?.name || '系统' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上报事件 -->
    <el-dialog v-model="createVisible" title="上报事件" width="520px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="事件类型" required>
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option v-for="(v, k) in dicts.eventType" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="儿童" required>
          <el-select v-model="createForm.childId" filterable style="width: 100%">
            <el-option v-for="c in children" :key="c.id" :label="`${c.name}（${c.classroom?.name || '未分班'}）`"
                       :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import http from '../api'
import { d, dType, dicts } from '../utils/dicts'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const isStaff = computed(() => auth.role !== 'PARENT')

const events = ref([])
const children = ref([])
const loading = ref(false)
const statusFilter = ref('')
const typeFilter = ref('')
const createVisible = ref(false)
const submitting = ref(false)
const createForm = reactive({ type: 'OTHER', childId: null, title: '', description: '' })

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value) params.status = statusFilter.value
    if (typeFilter.value) params.type = typeFilter.value
    events.value = await http.get('/events', { params })
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  children.value = await http.get('/children')
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.childId || !createForm.title) return ElMessage.warning('请选择儿童并填写标题')
  submitting.value = true
  try {
    await http.post('/events', createForm)
    ElMessage.success('事件已上报，相关人员已加入处理')
    createVisible.value = false
    Object.assign(createForm, { type: 'OTHER', childId: null, title: '', description: '' })
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
