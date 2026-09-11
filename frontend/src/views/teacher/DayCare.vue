<template>
  <div>
    <el-card>
      <template #header><b>在园照护记录</b></template>
      <div class="toolbar">
        <el-select v-model="childId" placeholder="选择儿童" style="width: 200px" @change="loadDaily">
          <el-option v-for="c in children" :key="c.child.id" :label="c.child.name" :value="c.child.id" />
        </el-select>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="loadDaily" />
        <el-button type="primary" :icon="Plus" :disabled="!childId" @click="openRecord()">添加照护记录</el-button>
      </div>

      <template v-if="daily">
        <el-alert v-if="daily.morningCheck" :closable="false" class="mb12"
                  :type="daily.morningCheck.result === 'ENTER_CLASS' ? 'success' : 'warning'">
          <template #title>
            晨检：{{ d('morningResult', daily.morningCheck.result) }}｜体温 {{ daily.morningCheck.temperature ?? '—' }}℃
            <span v-if="daily.morningCheck.note">｜{{ daily.morningCheck.note }}</span>
          </template>
        </el-alert>
        <el-alert v-else type="info" :closable="false" class="mb12" title="该儿童当日尚未晨检" />

        <el-card v-if="daily.medicationRequests?.length" shadow="never" class="inner mb12">
          <template #header><b>今日喂药委托</b></template>
          <div v-for="m in daily.medicationRequests" :key="m.id" class="med-row">
            <el-tag type="warning" size="small">在服</el-tag>
            <span>{{ m.medicineName }} {{ m.dosage }}（{{ m.timePlan || '按医嘱' }}）</span>
            <el-button size="small" type="primary" plain @click="openRecord(m)">记录喂药</el-button>
          </div>
        </el-card>

        <el-timeline v-if="daily.careRecords?.length" class="pt8">
          <el-timeline-item v-for="r in daily.careRecords" :key="r.id" :timestamp="fmt(r.createdAt)" placement="top"
                            :type="r.severity === 'NORMAL' ? 'primary' : r.severity === 'ATTENTION' ? 'warning' : 'danger'">
            <el-tag :type="dType('careType', r.type)" size="small">{{ d('careType', r.type) }}</el-tag>
            <el-tag v-if="r.injuryType" size="small" type="danger" effect="plain" class="ml4">
              {{ dicts.injuryType[r.injuryType] }}
            </el-tag>
            <el-tag v-if="r.severity !== 'NORMAL'" :type="dType('severity', r.severity)" size="small" class="ml4">
              {{ d('severity', r.severity) }}
            </el-tag>
            <div>{{ r.detail }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="当日暂无照护记录" />
      </template>
      <el-empty v-else description="请选择儿童查看在园记录" />
    </el-card>

    <!-- 添加照护记录 -->
    <el-dialog v-model="visible" title="添加照护记录" width="520px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.type">
            <el-radio-button value="NOON_CARE">午间照护</el-radio-button>
            <el-radio-button value="MEDICATION">喂药</el-radio-button>
            <el-radio-button value="TOILET">如厕</el-radio-button>
            <el-radio-button value="SLEEP">睡眠</el-radio-button>
            <el-radio-button value="INJURY">活动伤情</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type === 'INJURY'" label="伤情类型">
          <el-select v-model="form.injuryType">
            <el-option v-for="(label, key) in dicts.injuryType" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="关注程度" required>
          <el-radio-group v-model="form.severity">
            <el-radio value="NORMAL">一般</el-radio>
            <el-radio value="ATTENTION">需关注</el-radio>
            <el-radio value="SERIOUS">严重</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert v-if="form.type === 'INJURY' && form.severity !== 'NORMAL'" type="warning" :closable="false"
                  class="mb12" title="需关注/严重的伤情将自动创建协同事件，通知家长、保健老师与园长" />
        <el-form-item label="详细情况">
          <el-input v-model="form.detail" type="textarea" :rows="3"
                    placeholder="如：午睡12:30-14:00；氯雷他定5ml已喂；户外滑倒膝盖轻微擦伤已消毒" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交</el-button>
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
import { d, dType, dicts } from '../../utils/dicts'

const children = ref([])
const childId = ref(null)
const date = ref(dayjs().format('YYYY-MM-DD'))
const daily = ref(null)
const visible = ref(false)
const submitting = ref(false)

const form = reactive({ type: 'NOON_CARE', severity: 'NORMAL', injuryType: 'BUMP', detail: '' })

const fmt = (t) => (t ? dayjs(t).format('HH:mm') : '')

async function loadChildren() {
  children.value = await http.get('/attendance/class-children', { params: { date: date.value } })
  if (children.value.length && !childId.value) {
    childId.value = children.value[0].child.id
    loadDaily()
  }
}

async function loadDaily() {
  if (!childId.value) return
  daily.value = await http.get('/attendance/daily', { params: { childId: childId.value, date: date.value } })
}

function openRecord(medication) {
  if (medication) {
    Object.assign(form, {
      type: 'MEDICATION',
      severity: 'NORMAL',
      injuryType: 'BUMP',
      detail: `${medication.medicineName} ${medication.dosage} 已按委托喂服`
    })
  } else {
    Object.assign(form, { type: 'NOON_CARE', severity: 'NORMAL', injuryType: 'BUMP', detail: '' })
  }
  visible.value = true
}

async function submit() {
  if (!form.detail.trim()) return ElMessage.warning('请填写详细情况')
  submitting.value = true
  try {
    await http.post('/attendance/care-records', {
      childId: childId.value,
      date: date.value,
      ...form
    })
    ElMessage.success('照护记录已添加')
    visible.value = false
    loadDaily()
  } finally {
    submitting.value = false
  }
}

onMounted(loadChildren)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; }
.mb12 { margin-bottom: 12px; }
.ml4 { margin-left: 4px; }
.pt8 { padding-top: 8px; }
.inner { background: #fafafa; }
.med-row { display: flex; align-items: center; gap: 10px; margin: 6px 0; }
</style>
