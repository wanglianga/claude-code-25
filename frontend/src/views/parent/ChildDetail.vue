<template>
  <div v-loading="loading">
    <el-card v-if="detail.child">
      <template #header>
        <div class="card-header">
          <b>{{ detail.child.name }} 的资料</b>
          <el-tag :type="dType('childStatus', detail.child.status)">{{ d('childStatus', detail.child.status) }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="性别">{{ detail.child.gender }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ detail.child.birthDate }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ detail.child.classroom?.name || '待分班' }}</el-descriptions-item>
        <el-descriptions-item label="过敏史">{{ detail.child.allergyHistory || '—' }}</el-descriptions-item>
        <el-descriptions-item label="疫苗情况">{{ detail.child.vaccinationStatus || '—' }}</el-descriptions-item>
        <el-descriptions-item label="午睡习惯">{{ detail.child.napHabit || '—' }}</el-descriptions-item>
        <el-descriptions-item label="如厕能力">{{ detail.child.toiletAbility || '—' }}</el-descriptions-item>
        <el-descriptions-item label="常用药">{{ detail.child.medications || '—' }}</el-descriptions-item>
        <el-descriptions-item label="特殊照护">
          <el-tag v-if="detail.child.allergyMealRequired" type="warning" size="small" class="mr4">需过敏餐</el-tag>
          <el-tag v-if="detail.child.healthObservation" type="danger" size="small" class="mr4">保健观察</el-tag>
          {{ detail.child.specialCareNeeds || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="紧急联系人">
          <div v-for="c in detail.emergencyContacts" :key="c.id">{{ c.name }}（{{ c.relation }}）{{ c.phone }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="接送授权人">
          <div v-for="a in detail.pickupAuthorizations" :key="a.id">{{ a.name }}（{{ a.relation }}）{{ a.phone }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="mt16">
      <el-tabs v-model="tab">
        <!-- 在园记录 -->
        <el-tab-pane label="在园记录" name="daily">
          <div class="toolbar">
            <el-date-picker v-model="dailyDate" type="date" value-format="YYYY-MM-DD" :clearable="false"
                            @change="loadDaily" />
          </div>
          <div v-if="daily">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-card shadow="never" class="inner-card">
                  <template #header><b>晨检</b></template>
                  <template v-if="daily.morningCheck">
                    <el-tag :type="dType('morningResult', daily.morningCheck.result)">
                      {{ d('morningResult', daily.morningCheck.result) }}
                    </el-tag>
                    <div class="kv">体温：{{ daily.morningCheck.temperature ?? '未测' }}℃</div>
                    <div class="kv">皮肤：{{ daily.morningCheck.skinStatus || '—' }}｜情绪：{{ daily.morningCheck.mood || '—' }}｜饮食：{{ daily.morningCheck.diet || '—' }}</div>
                    <div class="kv">携带物品：{{ daily.morningCheck.carriedItems || '—' }}</div>
                    <div class="kv" v-if="daily.morningCheck.note">备注：{{ daily.morningCheck.note }}</div>
                  </template>
                  <el-empty v-else description="当日无晨检记录" :image-size="60" />
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card shadow="never" class="inner-card">
                  <template #header><b>接送</b></template>
                  <template v-if="daily.pickupRecords?.length">
                    <div v-for="p in daily.pickupRecords" :key="p.id" class="kv">
                      <el-tag :type="dType('pickupResult', p.result)" size="small">{{ d('pickupResult', p.result) }}</el-tag>
                      {{ p.pickupPersonName }}｜{{ dicts.verifyMethod[p.verifyMethod] }}
                      <span v-if="p.authType">｜{{ dicts.authType[p.authType] }}</span>
                      <span v-if="p.denyReason" class="danger">｜{{ p.denyReason }}</span>
                    </div>
                  </template>
                  <el-empty v-else description="当日无接送记录" :image-size="60" />
                </el-card>
              </el-col>
            </el-row>
            <el-card shadow="never" class="inner-card mt16">
              <template #header><b>照护记录（午间照护/喂药/如厕/睡眠/活动伤情）</b></template>
              <el-timeline v-if="daily.careRecords?.length">
                <el-timeline-item v-for="r in daily.careRecords" :key="r.id" :timestamp="fmt(r.createdAt)" placement="top">
                  <el-tag :type="dType('careType', r.type)" size="small">{{ d('careType', r.type) }}</el-tag>
                  <el-tag v-if="r.severity !== 'NORMAL'" :type="dType('severity', r.severity)" size="small" class="ml4">
                    {{ d('severity', r.severity) }}
                  </el-tag>
                  <span class="ml4">{{ r.detail }}</span>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="当日无照护记录" :image-size="60" />
            </el-card>
            <el-card shadow="never" class="inner-card mt16" v-if="daily.events?.length">
              <template #header><b>当日事件</b></template>
              <div v-for="e in daily.events" :key="e.id" class="kv">
                <el-tag :type="dType('eventType', e.type)" size="small">{{ d('eventType', e.type) }}</el-tag>
                <el-link type="primary" class="ml4" @click="$router.push(`/events/${e.id}`)">{{ e.title }}</el-link>
                <el-tag :type="dType('eventStatus', e.status)" size="small" class="ml4">{{ d('eventStatus', e.status) }}</el-tag>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <!-- 时间轴 -->
        <el-tab-pane label="成长时间轴" name="timeline">
          <el-timeline v-if="timeline.length" class="pt8">
            <el-timeline-item v-for="(item, i) in timeline" :key="i" :timestamp="fmt(item.time)" placement="top"
                              :type="tlType(item.category)">
              <el-tag size="small" :type="dType('timelineCategory', item.category)">
                {{ d('timelineCategory', item.category) }}
              </el-tag>
              <b class="ml4">{{ item.title }}</b>
              <div class="detail">{{ item.detail }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无记录" />
        </el-tab-pane>

        <!-- 喂药委托 -->
        <el-tab-pane label="喂药委托" name="medication">
          <div class="toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="medVisible = true">新增喂药委托</el-button>
          </div>
          <el-table :data="medications">
            <el-table-column prop="medicineName" label="药品" width="140" />
            <el-table-column prop="dosage" label="剂量" width="90" />
            <el-table-column prop="timePlan" label="服用时间" width="150" />
            <el-table-column label="有效期" width="200">
              <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
            </el-table-column>
            <el-table-column prop="parentNote" label="备注" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="dType('medicationStatus', row.status)" size="small">{{ d('medicationStatus', row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button v-if="row.status === 'ACTIVE'" link type="warning" @click="finishMedication(row)">结束</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 临时委托 -->
        <el-tab-pane label="临时接送委托" name="delegation">
          <div class="toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="delVisible = true">新增临时委托</el-button>
          </div>
          <el-table :data="delegations">
            <el-table-column prop="delegateName" label="被委托人" width="110" />
            <el-table-column prop="delegatePhone" label="电话" width="140" />
            <el-table-column prop="delegateIdNumber" label="证件号" width="180" />
            <el-table-column prop="validDate" label="委托日期" width="120" />
            <el-table-column prop="note" label="备注" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="dType('delegationStatus', row.status)" size="small">{{ d('delegationStatus', row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 接送留言 -->
        <el-tab-pane label="接送留言" name="message">
          <div class="toolbar">
            <el-input v-model="newMessage" placeholder="给前台的接送留言（如：今天外婆来接）" style="width: 380px" />
            <el-button type="primary" size="small" @click="sendMessage">发布今日留言</el-button>
          </div>
          <el-table :data="messages">
            <el-table-column prop="msgDate" label="日期" width="120" />
            <el-table-column prop="content" label="内容" />
            <el-table-column prop="createdAt" label="发布时间" width="170">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新增喂药委托 -->
    <el-dialog v-model="medVisible" title="新增喂药委托" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="药品名称" required><el-input v-model="medForm.medicineName" /></el-form-item>
        <el-form-item label="剂量" required><el-input v-model="medForm.dosage" placeholder="如：5ml" /></el-form-item>
        <el-form-item label="服用时间"><el-input v-model="medForm.timePlan" placeholder="如：每日午饭后" /></el-form-item>
        <el-form-item label="有效期" required>
          <el-date-picker v-model="medRange" type="daterange" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="medForm.parentNote" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="medVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMedication">提交</el-button>
      </template>
    </el-dialog>

    <!-- 新增临时委托 -->
    <el-dialog v-model="delVisible" title="新增临时接送委托" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="被委托人" required><el-input v-model="delForm.delegateName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="delForm.delegatePhone" /></el-form-item>
        <el-form-item label="证件号"><el-input v-model="delForm.delegateIdNumber" placeholder="前台核验用" /></el-form-item>
        <el-form-item label="委托日期" required>
          <el-date-picker v-model="delForm.validDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="delForm.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="delVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDelegation">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import http from '../../api'
import { d, dType, dicts } from '../../utils/dicts'

const route = useRoute()
const childId = route.params.id

const loading = ref(false)
const detail = ref({})
const tab = ref('daily')
const dailyDate = ref(dayjs().format('YYYY-MM-DD'))
const daily = ref(null)
const timeline = ref([])
const medications = ref([])
const delegations = ref([])
const messages = ref([])
const newMessage = ref('')

const medVisible = ref(false)
const medForm = reactive({ medicineName: '', dosage: '', timePlan: '', parentNote: '' })
const medRange = ref([dayjs().format('YYYY-MM-DD'), dayjs().add(7, 'day').format('YYYY-MM-DD')])

const delVisible = ref(false)
const delForm = reactive({ delegateName: '', delegatePhone: '', delegateIdNumber: '', validDate: dayjs().format('YYYY-MM-DD'), note: '' })

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')
const tlType = (cat) => ({ EVENT: 'danger', ALERT: 'danger', MORNING_CHECK: 'warning', PICKUP: 'primary' }[cat] || 'success')

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await http.get(`/children/${childId}`)
  } finally {
    loading.value = false
  }
}

async function loadDaily() {
  daily.value = await http.get('/attendance/daily', { params: { childId, date: dailyDate.value } })
}

async function loadTimeline() {
  timeline.value = await http.get(`/timeline/child/${childId}`)
}

async function loadMedications() {
  medications.value = await http.get(`/children/${childId}/medication-requests`)
}

async function loadDelegations() {
  delegations.value = await http.get(`/children/${childId}/delegations`)
}

async function loadMessages() {
  messages.value = await http.get(`/children/${childId}/pickup-messages`)
}

async function submitMedication() {
  if (!medForm.medicineName || !medForm.dosage || !medRange.value?.length) {
    return ElMessage.warning('请完整填写药品、剂量与有效期')
  }
  await http.post(`/children/${childId}/medication-requests`, {
    ...medForm,
    startDate: medRange.value[0],
    endDate: medRange.value[1]
  })
  ElMessage.success('喂药委托已提交')
  medVisible.value = false
  loadMedications()
}

async function finishMedication(row) {
  await http.post(`/medication-requests/${row.id}/finish`)
  ElMessage.success('已结束')
  loadMedications()
}

async function submitDelegation() {
  if (!delForm.delegateName || !delForm.validDate) return ElMessage.warning('请填写被委托人与委托日期')
  await http.post(`/children/${childId}/delegations`, delForm)
  ElMessage.success('临时委托已提交，前台核验时生效')
  delVisible.value = false
  loadDelegations()
}

async function sendMessage() {
  if (!newMessage.value.trim()) return ElMessage.warning('请输入留言内容')
  await http.post(`/children/${childId}/pickup-messages`, { content: newMessage.value })
  newMessage.value = ''
  ElMessage.success('留言已发布，前台接送时可见')
  loadMessages()
}

onMounted(() => {
  loadDetail()
  loadDaily()
  loadTimeline()
  loadMedications()
  loadDelegations()
  loadMessages()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.mt16 { margin-top: 16px; }
.ml4 { margin-left: 4px; }
.mr4 { margin-right: 4px; }
.pt8 { padding-top: 8px; }
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.inner-card { background: #fafafa; }
.kv { margin: 6px 0; color: #606266; font-size: 13px; }
.detail { color: #909399; font-size: 12px; margin-top: 4px; }
.danger { color: #f56c6c; }
</style>
