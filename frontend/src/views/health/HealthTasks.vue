<template>
  <div>
    <el-card>
      <template #header><b>健康评估任务（入托前）</b></template>
      <el-table :data="tasks" v-loading="loading">
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-descriptions :column="2" border size="small" class="expand-desc">
              <el-descriptions-item label="过敏史">
                <span class="danger">{{ row.child.allergyHistory || '无' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="疫苗情况">{{ row.child.vaccinationStatus || '—' }}</el-descriptions-item>
              <el-descriptions-item label="常用药">{{ row.child.medications || '—' }}</el-descriptions-item>
              <el-descriptions-item label="特殊照护需求">{{ row.child.specialCareNeeds || '—' }}</el-descriptions-item>
              <el-descriptions-item label="午睡习惯">{{ row.child.napHabit || '—' }}</el-descriptions-item>
              <el-descriptions-item label="如厕能力">{{ row.child.toiletAbility || '—' }}</el-descriptions-item>
              <el-descriptions-item v-if="row.supplementNote" label="家长补充资料" :span="2">
                {{ row.supplementNote }}
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column label="儿童" width="100">
          <template #default="{ row }">{{ row.child.name }}</template>
        </el-table-column>
        <el-table-column label="班级" width="100">
          <template #default="{ row }">{{ row.child.classroom?.name || '—' }}</template>
        </el-table-column>
        <el-table-column label="家长" width="100">
          <template #default="{ row }">{{ row.child.parent?.name }}</template>
        </el-table-column>
        <el-table-column label="过敏史" show-overflow-tooltip>
          <template #default="{ row }">{{ row.child.allergyHistory || '无' }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="待评估时间" width="160">
          <template #default="{ row }">{{ fmt(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openAssess(row)">健康评估</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !tasks.length" description="暂无待评估任务" />
    </el-card>

    <el-card class="mt16">
      <template #header>
        <div class="card-header">
          <b>今日晨检情况</b>
          <el-date-picker v-model="checkDate" type="date" value-format="YYYY-MM-DD" :clearable="false"
                          size="small" @change="loadChecks" />
        </div>
      </template>
      <el-table :data="morningChecks">
        <el-table-column label="儿童" width="100">
          <template #default="{ row }">{{ row.child?.name }}</template>
        </el-table-column>
        <el-table-column label="班级" width="100">
          <template #default="{ row }">{{ row.child?.classroom?.name || '—' }}</template>
        </el-table-column>
        <el-table-column label="体温" width="90">
          <template #default="{ row }">
            <span :class="{ danger: row.temperature >= 37.3 }">{{ row.temperature ?? '—' }}℃</span>
          </template>
        </el-table-column>
        <el-table-column prop="skinStatus" label="皮肤" width="90" />
        <el-table-column prop="mood" label="情绪" width="90" />
        <el-table-column label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="dType('morningResult', row.result)">{{ d('morningResult', row.result) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" show-overflow-tooltip />
      </el-table>
    </el-card>

    <!-- 健康评估对话框 -->
    <el-dialog v-model="assessVisible" title="入托前健康评估" width="560px" destroy-on-close>
      <template v-if="current">
        <el-descriptions :column="1" border size="small" class="mb12">
          <el-descriptions-item label="儿童">{{ current.child.name }}（{{ current.child.classroom?.name || '未分班' }}）</el-descriptions-item>
          <el-descriptions-item label="过敏史">{{ current.child.allergyHistory || '无' }}</el-descriptions-item>
          <el-descriptions-item label="疫苗">{{ current.child.vaccinationStatus || '—' }}</el-descriptions-item>
          <el-descriptions-item label="常用药">{{ current.child.medications || '—' }}</el-descriptions-item>
        </el-descriptions>
        <el-form label-width="110px">
          <el-form-item label="评估结论" required>
            <el-radio-group v-model="assessForm.result">
              <el-radio value="PASS">通过</el-radio>
              <el-radio value="ALLERGY_MEAL">通过（需过敏餐）</el-radio>
              <el-radio value="TEMP_OBSERVATION">通过（需临时观察）</el-radio>
              <el-radio value="NEED_SUPPLEMENT">需家长补充资料</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="assessForm.result === 'ALLERGY_MEAL' || assessForm.result === 'TEMP_OBSERVATION'" label="照护标记">
            <el-checkbox v-model="assessForm.allergyMealRequired">需要过敏餐</el-checkbox>
            <el-checkbox v-model="assessForm.observationRequired">需要保健观察</el-checkbox>
          </el-form-item>
          <el-form-item v-if="assessForm.result === 'NEED_SUPPLEMENT'" label="补充资料要求" required>
            <el-input v-model="assessForm.supplementRequest" type="textarea" :rows="2"
                      placeholder="如：请补充最新体检报告、疫苗本复印件" />
          </el-form-item>
          <el-form-item label="评估备注">
            <el-input v-model="assessForm.note" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="assessVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAssess">提交评估</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import http from '../../api'
import { d, dType } from '../../utils/dicts'

const tasks = ref([])
const morningChecks = ref([])
const loading = ref(false)
const checkDate = ref(dayjs().format('YYYY-MM-DD'))
const assessVisible = ref(false)
const current = ref(null)
const submitting = ref(false)
const assessForm = reactive({
  result: 'PASS',
  allergyMealRequired: false,
  observationRequired: false,
  supplementRequest: '',
  note: ''
})

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loading.value = true
  try {
    tasks.value = await http.get('/applications', { params: { status: 'PENDING_HEALTH' } })
  } finally {
    loading.value = false
  }
}

async function loadChecks() {
  morningChecks.value = await http.get('/attendance/morning-checks', { params: { date: checkDate.value } })
}

function openAssess(row) {
  current.value = row
  Object.assign(assessForm, {
    result: 'PASS',
    allergyMealRequired: false,
    observationRequired: false,
    supplementRequest: '',
    note: ''
  })
  assessVisible.value = true
}

async function submitAssess() {
  if (assessForm.result === 'NEED_SUPPLEMENT' && !assessForm.supplementRequest.trim()) {
    return ElMessage.warning('请填写需要家长补充的资料说明')
  }
  submitting.value = true
  try {
    await http.post(`/applications/${current.value.id}/health-assessment`, assessForm)
    ElMessage.success('健康评估已提交')
    assessVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  load()
  loadChecks()
})
</script>

<style scoped>
.mt16 { margin-top: 16px; }
.mb12 { margin-bottom: 12px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.expand-desc { padding: 8px 24px; }
.danger { color: #f56c6c; font-weight: 600; }
</style>
