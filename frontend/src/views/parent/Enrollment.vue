<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <b>我的孩子</b>
          <el-button type="primary" :icon="Plus" @click="openForm">新建入托申请</el-button>
        </div>
      </template>
      <el-table :data="children" v-loading="loadingChildren">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column label="出生日期" width="120" prop="birthDate" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="dType('childStatus', row.status)">{{ d('childStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="班级" width="100">
          <template #default="{ row }">{{ row.classroom?.name || '—' }}</template>
        </el-table-column>
        <el-table-column label="特殊照护" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.allergyMealRequired" type="warning" size="small" class="mr4">过敏餐</el-tag>
            <el-tag v-if="row.healthObservation" type="danger" size="small" class="mr4">保健观察</el-tag>
            {{ row.specialCareNeeds || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/parent/children/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="mt16">
      <template #header><b>我的入托申请</b></template>
      <el-table :data="applications">
        <el-table-column label="儿童" width="100">
          <template #default="{ row }">{{ row.child?.name }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="dType('appStatus', row.status)">{{ d('appStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="parentExpectations" label="家长期望" show-overflow-tooltip />
        <el-table-column label="需补充资料" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.status === 'NEED_SUPPLEMENT'" class="danger">{{ row.supplementRequest }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="170">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'NEED_SUPPLEMENT'" link type="danger"
                       @click="openSupplement(row)">补充资料</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建入托申请 -->
    <el-dialog v-model="formVisible" title="新建入托申请" width="760px" top="3vh" destroy-on-close>
      <el-form :model="form" label-width="110px" scroll-to-error>
        <el-divider content-position="left">儿童基本信息</el-divider>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="姓名" required><el-input v-model="form.childName" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性别" required>
              <el-select v-model="form.gender"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出生日期" required>
              <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">健康与照护信息</el-divider>
        <el-form-item label="过敏史"><el-input v-model="form.allergyHistory" type="textarea" :rows="2" placeholder="如：花生过敏，曾出现荨麻疹；无则填“无”" /></el-form-item>
        <el-form-item label="疫苗情况"><el-input v-model="form.vaccinationStatus" type="textarea" :rows="2" placeholder="疫苗接种情况" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="午睡习惯"><el-input v-model="form.napHabit" placeholder="如：需安抚巾，入睡约20分钟" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="如厕能力"><el-input v-model="form.toiletAbility" placeholder="如：如厕训练中/可自主如厕/纸尿裤" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="常用药"><el-input v-model="form.medications" placeholder="如：氯雷他定糖浆；无则填“无”" /></el-form-item>
        <el-form-item label="特殊照护需求"><el-input v-model="form.specialCareNeeds" type="textarea" :rows="2" placeholder="如：需过敏餐、分离焦虑需渐进适应等" /></el-form-item>

        <el-divider content-position="left">紧急联系人</el-divider>
        <div v-for="(c, i) in form.emergencyContacts" :key="i" class="dyn-row">
          <el-input v-model="c.name" placeholder="姓名" style="width: 140px" />
          <el-input v-model="c.relation" placeholder="关系" style="width: 120px" />
          <el-input v-model="c.phone" placeholder="电话" style="width: 170px" />
          <el-input-number v-model="c.priority" :min="1" :max="9" placeholder="优先级" style="width: 110px" />
          <el-button link type="danger" @click="form.emergencyContacts.splice(i, 1)">删除</el-button>
        </div>
        <el-button link type="primary" :icon="Plus" @click="form.emergencyContacts.push({ name: '', relation: '', phone: '', priority: form.emergencyContacts.length + 1 })">添加联系人</el-button>

        <el-divider content-position="left">接送授权人</el-divider>
        <div v-for="(a, i) in form.pickupAuthorizations" :key="i" class="dyn-row">
          <el-input v-model="a.name" placeholder="姓名" style="width: 140px" />
          <el-input v-model="a.relation" placeholder="关系" style="width: 120px" />
          <el-input v-model="a.phone" placeholder="电话" style="width: 160px" />
          <el-input v-model="a.idNumber" placeholder="证件号（核验用）" style="width: 210px" />
          <el-button link type="danger" @click="form.pickupAuthorizations.splice(i, 1)">删除</el-button>
        </div>
        <el-button link type="primary" :icon="Plus" @click="form.pickupAuthorizations.push({ name: '', relation: '', phone: '', idNumber: '' })">添加授权人</el-button>

        <el-divider content-position="left">家长期望</el-divider>
        <el-form-item label-width="0">
          <el-input v-model="form.parentExpectations" type="textarea" :rows="2" placeholder="如：希望先半天过渡、就近班级、关注社交能力等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 补充资料 -->
    <el-dialog v-model="supplementVisible" title="补充资料" width="520px" destroy-on-close>
      <el-alert type="warning" :closable="false" class="mb12">
        <template #title>保健老师要求补充：{{ currentApp?.supplementRequest }}</template>
      </el-alert>
      <el-input v-model="supplementNote" type="textarea" :rows="4" placeholder="请填写补充说明或资料内容" />
      <template #footer>
        <el-button @click="supplementVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitSupplement">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import http from '../../api'
import { d, dType } from '../../utils/dicts'

const children = ref([])
const applications = ref([])
const loadingChildren = ref(false)
const formVisible = ref(false)
const supplementVisible = ref(false)
const submitting = ref(false)
const currentApp = ref(null)
const supplementNote = ref('')

const emptyForm = () => ({
  childName: '', gender: '男', birthDate: '',
  allergyHistory: '', vaccinationStatus: '', napHabit: '', toiletAbility: '',
  medications: '', specialCareNeeds: '', parentExpectations: '',
  emergencyContacts: [{ name: '', relation: '', phone: '', priority: 1 }],
  pickupAuthorizations: [{ name: '', relation: '', phone: '', idNumber: '' }]
})
const form = reactive(emptyForm())

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loadingChildren.value = true
  try {
    children.value = await http.get('/children')
    applications.value = await http.get('/applications/my')
  } finally {
    loadingChildren.value = false
  }
}

function openForm() {
  Object.assign(form, emptyForm())
  formVisible.value = true
}

async function submit() {
  if (!form.childName || !form.birthDate) return ElMessage.warning('请填写儿童姓名与出生日期')
  if (!form.emergencyContacts.length || form.emergencyContacts.some((c) => !c.name || !c.phone)) {
    return ElMessage.warning('请完整填写至少一名紧急联系人')
  }
  if (!form.pickupAuthorizations.length || form.pickupAuthorizations.some((a) => !a.name)) {
    return ElMessage.warning('请完整填写至少一名接送授权人')
  }
  submitting.value = true
  try {
    await http.post('/applications', form)
    ElMessage.success('入托申请已提交，等待园方评估')
    formVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function openSupplement(app) {
  currentApp.value = app
  supplementNote.value = ''
  supplementVisible.value = true
}

async function submitSupplement() {
  if (!supplementNote.value.trim()) return ElMessage.warning('请填写补充资料')
  submitting.value = true
  try {
    await http.post(`/applications/${currentApp.value.id}/supplement`, { supplementNote: supplementNote.value })
    ElMessage.success('补充资料已提交，等待健康评估')
    supplementVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.mt16 { margin-top: 16px; }
.mb12 { margin-bottom: 12px; }
.mr4 { margin-right: 4px; }
.danger { color: #f56c6c; }
.dyn-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
</style>
