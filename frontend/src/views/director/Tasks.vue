<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <b>入托评估任务</b>
          <el-radio-group v-model="statusFilter" size="small" @change="load">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="PENDING_CLASS">待分班</el-radio-button>
            <el-radio-button value="PENDING_HEALTH">待健康评估</el-radio-button>
            <el-radio-button value="NEED_SUPPLEMENT">需补充资料</el-radio-button>
            <el-radio-button value="COMPLETED">已入托</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="applications" v-loading="loading">
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-descriptions :column="2" border size="small" class="expand-desc">
              <el-descriptions-item label="过敏史">{{ row.child.allergyHistory || '—' }}</el-descriptions-item>
              <el-descriptions-item label="疫苗情况">{{ row.child.vaccinationStatus || '—' }}</el-descriptions-item>
              <el-descriptions-item label="午睡习惯">{{ row.child.napHabit || '—' }}</el-descriptions-item>
              <el-descriptions-item label="如厕能力">{{ row.child.toiletAbility || '—' }}</el-descriptions-item>
              <el-descriptions-item label="常用药">{{ row.child.medications || '—' }}</el-descriptions-item>
              <el-descriptions-item label="特殊照护需求">
                <span class="danger">{{ row.child.specialCareNeeds || '—' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="家长期望" :span="2">{{ row.parentExpectations || '—' }}</el-descriptions-item>
              <el-descriptions-item v-if="row.supplementNote" label="家长补充资料" :span="2">
                {{ row.supplementNote }}
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column label="儿童" width="90">
          <template #default="{ row }">{{ row.child.name }}</template>
        </el-table-column>
        <el-table-column label="年龄" width="80">
          <template #default="{ row }">{{ age(row.child.birthDate) }}</template>
        </el-table-column>
        <el-table-column label="家长" width="90">
          <template #default="{ row }">{{ row.child.parent?.name }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="dType('appStatus', row.status)">{{ d('appStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="班级" width="90">
          <template #default="{ row }">{{ row.child.classroom?.name || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING_CLASS'" type="primary" size="small"
                       @click="openAssign(row)">分班</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 分班对话框 -->
    <el-dialog v-model="assignVisible" title="安排班级" width="640px" destroy-on-close>
      <template v-if="current">
        <el-alert type="info" :closable="false" class="mb12">
          <template #title>
            {{ current.child.name }}｜特殊照护：{{ current.child.specialCareNeeds || '无' }}｜家长期望：{{ current.parentExpectations || '无' }}
          </template>
        </el-alert>
        <el-table :data="classrooms" highlight-current-row @current-change="(r) => (selectedClass = r)"
                  max-height="260">
          <el-table-column prop="name" label="班级" width="100" />
          <el-table-column prop="ageRange" label="适龄" width="90" />
          <el-table-column label="容量" width="110">
            <template #default="{ row }">
              <span :class="{ danger: row.remaining <= 0 }">{{ row.enrolledCount }}/{{ row.capacity }}</span>
            </template>
          </el-table-column>
          <el-table-column label="师幼比" width="100">
            <template #default="{ row }">{{ row.ratio }}</template>
          </el-table-column>
          <el-table-column label="带班老师">
            <template #default="{ row }">
              {{ row.teachers?.length ? row.teachers.map((t) => t.name).join('、') : '未分配' }}
            </template>
          </el-table-column>
        </el-table>
        <el-form class="mt16" label-width="90px">
          <el-form-item label="分班备注">
            <el-input v-model="assignNote" type="textarea" :rows="2"
                      placeholder="记录容量/师幼比/特殊照护/家长期望等考量" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAssign">确认分班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import http from '../../api'
import { d, dType } from '../../utils/dicts'

const applications = ref([])
const classrooms = ref([])
const loading = ref(false)
const statusFilter = ref('')
const assignVisible = ref(false)
const current = ref(null)
const selectedClass = ref(null)
const assignNote = ref('')
const submitting = ref(false)

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

function age(birth) {
  if (!birth) return '—'
  const months = dayjs().diff(dayjs(birth), 'month')
  return months < 12 ? `${months}个月` : `${Math.floor(months / 12)}岁${months % 12 ? (months % 12) + '个月' : ''}`
}

async function load() {
  loading.value = true
  try {
    applications.value = await http.get('/applications', { params: statusFilter.value ? { status: statusFilter.value } : {} })
  } finally {
    loading.value = false
  }
}

async function openAssign(row) {
  current.value = row
  selectedClass.value = null
  assignNote.value = ''
  classrooms.value = await http.get('/classrooms')
  assignVisible.value = true
}

async function submitAssign() {
  if (!selectedClass.value) return ElMessage.warning('请选择班级')
  submitting.value = true
  try {
    await http.post(`/applications/${current.value.id}/assign`, {
      classroomId: selectedClass.value.id,
      note: assignNote.value
    })
    ElMessage.success(`已分入「${selectedClass.value.name}」，待保健老师健康评估`)
    assignVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.expand-desc { padding: 8px 24px; }
.mb12 { margin-bottom: 12px; }
.mt16 { margin-top: 16px; }
.danger { color: #f56c6c; font-weight: 600; }
</style>
