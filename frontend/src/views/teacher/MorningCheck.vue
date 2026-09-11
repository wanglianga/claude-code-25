<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <b>每日晨检</b>
          <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false"
                          @change="load" />
        </div>
      </template>
      <el-alert v-if="date === today" type="info" :closable="false" class="mb12" show-icon
                title="晨检结果将影响孩子能否入班：发热（≥37.3℃）或异常情况会自动创建协同事件，通知家长、保健老师与园长。" />
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="儿童" width="100">
          <template #default="{ row }">{{ row.child.name }}</template>
        </el-table-column>
        <el-table-column label="班级" width="100">
          <template #default="{ row }">{{ row.child.classroom?.name }}</template>
        </el-table-column>
        <el-table-column label="喂药委托" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.hasMedicationToday" type="warning" size="small">今日需喂药</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="晨检状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.morningCheck" :type="dType('morningResult', row.morningCheck.result)">
              {{ d('morningResult', row.morningCheck.result) }}
            </el-tag>
            <el-tag v-else type="info" effect="plain">未晨检</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="体温" width="90">
          <template #default="{ row }">
            <span v-if="row.morningCheck" :class="{ danger: row.morningCheck.temperature >= 37.3 }">
              {{ row.morningCheck.temperature ?? '—' }}℃
            </span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="皮肤/情绪/饮食">
          <template #default="{ row }">
            <span v-if="row.morningCheck">
              {{ row.morningCheck.skinStatus || '—' }} / {{ row.morningCheck.mood || '—' }} / {{ row.morningCheck.diet || '—' }}
            </span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openCheck(row)">
              {{ row.morningCheck ? '修改' : '晨检登记' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 晨检登记 -->
    <el-dialog v-model="visible" :title="`晨检登记 · ${current?.child?.name || ''}`" width="560px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="体温（℃）" required>
          <el-input-number v-model="form.temperature" :min="35" :max="42" :step="0.1" :precision="1" />
          <span v-if="form.temperature >= 37.3" class="danger ml8">发热！建议隔离观察或通知家长接回</span>
        </el-form-item>
        <el-form-item label="皮肤">
          <el-select v-model="form.skinStatus">
            <el-option v-for="o in ['正常', '皮疹', '红肿', '其他']" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="情绪">
          <el-select v-model="form.mood">
            <el-option v-for="o in ['愉快', '正常', '低落', '烦躁', '哭闹']" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="饮食">
          <el-select v-model="form.diet">
            <el-option v-for="o in ['早餐吃完', '进食一般', '食欲不振', '未进食']" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="携带物品">
          <el-input v-model="form.carriedItems" placeholder="如：换洗衣物、安抚巾、药品" />
        </el-form-item>
        <el-form-item v-if="current?.hasMedicationToday" label="药品是否携带">
          <el-switch v-model="form.medicationBrought" active-text="已带" inactive-text="未带" />
          <div v-if="!form.medicationBrought" class="danger tip">该儿童今日有喂药委托，未带药将自动创建「药品漏带」事件</div>
        </el-form-item>
        <el-form-item label="晨检结果" required>
          <el-radio-group v-model="form.result">
            <el-radio value="ENTER_CLASS">正常入班</el-radio>
            <el-radio value="ISOLATION">隔离观察</el-radio>
            <el-radio value="PARENT_PICKUP">通知家长接回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" type="textarea" :rows="2" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import http from '../../api'
import { d, dType } from '../../utils/dicts'

const today = dayjs().format('YYYY-MM-DD')
const date = ref(today)
const rows = ref([])
const loading = ref(false)
const visible = ref(false)
const current = ref(null)
const submitting = ref(false)

const form = reactive({
  temperature: 36.5,
  skinStatus: '正常',
  mood: '愉快',
  diet: '早餐吃完',
  carriedItems: '',
  medicationBrought: true,
  result: 'ENTER_CLASS',
  note: ''
})

async function load() {
  loading.value = true
  try {
    rows.value = await http.get('/attendance/class-children', { params: { date: date.value } })
  } finally {
    loading.value = false
  }
}

function openCheck(row) {
  current.value = row
  const mc = row.morningCheck
  Object.assign(form, {
    temperature: mc?.temperature ?? 36.5,
    skinStatus: mc?.skinStatus || '正常',
    mood: mc?.mood || '愉快',
    diet: mc?.diet || '早餐吃完',
    carriedItems: mc?.carriedItems || '',
    medicationBrought: mc?.medicationBrought ?? true,
    result: mc?.result || 'ENTER_CLASS',
    note: mc?.note || ''
  })
  visible.value = true
}

async function submit() {
  if (form.temperature >= 37.3 && form.result === 'ENTER_CLASS') {
    return ElMessage.warning('体温 ≥37.3℃ 不能入班，请选择隔离观察或通知家长接回')
  }
  submitting.value = true
  try {
    await http.post('/attendance/morning-checks', {
      childId: current.value.child.id,
      date: date.value,
      ...form
    })
    ElMessage.success('晨检已登记')
    visible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.mb12 { margin-bottom: 12px; }
.ml8 { margin-left: 8px; }
.danger { color: #f56c6c; font-weight: 600; }
.muted { color: #c0c4cc; }
.tip { font-size: 12px; }
</style>
