<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card>
          <template #header><b>接送核验</b></template>
          <el-form label-width="90px">
            <el-form-item label="儿童" required>
              <el-select v-model="form.childId" filterable placeholder="选择儿童" style="width: 100%"
                         @change="onChildChange">
                <el-option v-for="c in children" :key="c.id" :label="`${c.name}（${c.classroom?.name || '未分班'}）`"
                           :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="接送人" required>
              <el-input v-model="form.name" placeholder="接送人姓名" />
            </el-form-item>
            <el-form-item label="证件号">
              <el-input v-model="form.idNumber" placeholder="证件号或后4位（如已登记）" />
            </el-form-item>
            <el-form-item label="核验方式" required>
              <el-radio-group v-model="form.verifyMethod">
                <el-radio-button value="FACE">人脸核验</el-radio-button>
                <el-radio-button value="ID_CARD">证件核验</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="verifying" @click="verify">核验身份</el-button>
            </el-form-item>
          </el-form>

          <template v-if="verifyResult">
            <el-alert :type="verifyResult.matched ? 'success' : 'error'" :closable="false" class="mb12"
                      :title="verifyResult.message" show-icon />
            <el-descriptions v-if="verifyResult.matched" :column="1" border size="small" class="mb12">
              <el-descriptions-item label="授权类型">
                {{ dicts.authType[verifyResult.authType] }}
              </el-descriptions-item>
              <el-descriptions-item v-if="verifyResult.authorization" label="授权人">
                {{ verifyResult.authorization.name }}（{{ verifyResult.authorization.relation }}）
                {{ verifyResult.authorization.phone }}
              </el-descriptions-item>
              <el-descriptions-item v-if="verifyResult.delegation" label="临时委托">
                {{ verifyResult.delegation.delegateName }}｜{{ verifyResult.delegation.note || '—' }}
              </el-descriptions-item>
            </el-descriptions>
            <el-space>
              <el-button type="success" :disabled="!verifyResult.matched" :loading="submitting"
                         @click="submitRecord('SUCCESS')">核验通过，放行</el-button>
              <el-button type="danger" :loading="submitting" @click="denyVisible = true">拒绝并上报</el-button>
            </el-space>
          </template>

          <el-divider v-if="todayMessages.length" content-position="left">今日家长留言</el-divider>
          <div v-for="m in todayMessages" :key="m.id" class="msg">
            <el-tag size="small" type="warning">留言</el-tag> {{ m.content }}
          </div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card>
          <template #header><b>今日接送记录</b></template>
          <el-table :data="records" size="small">
            <el-table-column label="儿童" width="90">
              <template #default="{ row }">{{ row.child?.name }}</template>
            </el-table-column>
            <el-table-column prop="pickupPersonName" label="接送人" width="90" />
            <el-table-column label="结果" width="80">
              <template #default="{ row }">
                <el-tag :type="dType('pickupResult', row.result)" size="small">{{ d('pickupResult', row.result) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="核验" width="90">
              <template #default="{ row }">{{ dicts.verifyMethod[row.verifyMethod] }}</template>
            </el-table-column>
            <el-table-column label="授权类型" width="100">
              <template #default="{ row }">{{ row.authType ? dicts.authType[row.authType] : '—' }}</template>
            </el-table-column>
            <el-table-column prop="denyReason" label="拒绝原因" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" width="90">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="mt16">
          <template #header><b>今日临时委托</b></template>
          <el-table :data="delegations" size="small">
            <el-table-column label="儿童" width="90">
              <template #default="{ row }">{{ row.child?.name }}</template>
            </el-table-column>
            <el-table-column prop="delegateName" label="被委托人" width="100" />
            <el-table-column prop="delegatePhone" label="电话" width="130" />
            <el-table-column prop="note" label="备注" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="dType('delegationStatus', row.status)" size="small">
                  {{ d('delegationStatus', row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 拒绝原因 -->
    <el-dialog v-model="denyVisible" title="拒绝接送并上报事件" width="440px" destroy-on-close>
      <el-alert type="error" :closable="false" class="mb12"
                title="拒绝后将自动创建「未授权人员到场」事件，通知家长、老师、保健老师与园长" />
      <el-input v-model="denyReason" type="textarea" :rows="3"
                placeholder="拒绝原因，如：未在授权名单，无法核实身份" />
      <template #footer>
        <el-button @click="denyVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitRecord('DENIED')">拒绝并上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import http from '../../api'
import { d, dType, dicts } from '../../utils/dicts'

const children = ref([])
const records = ref([])
const delegations = ref([])
const todayMessages = ref([])
const verifyResult = ref(null)
const verifying = ref(false)
const submitting = ref(false)
const denyVisible = ref(false)
const denyReason = ref('')

const form = reactive({ childId: null, name: '', idNumber: '', verifyMethod: 'FACE' })

const fmtTime = (t) => (t ? dayjs(t).format('HH:mm') : '—')

async function loadBase() {
  children.value = await http.get('/children', { params: { status: 'ENROLLED' } })
  records.value = await http.get('/pickup/records')
  delegations.value = await http.get('/delegations/today')
}

async function onChildChange() {
  verifyResult.value = null
  todayMessages.value = []
  if (form.childId) {
    todayMessages.value = await http.get(`/children/${form.childId}/pickup-messages`, {
      params: { date: dayjs().format('YYYY-MM-DD') }
    })
  }
}

async function verify() {
  if (!form.childId || !form.name) return ElMessage.warning('请选择儿童并输入接送人姓名')
  verifying.value = true
  try {
    verifyResult.value = await http.post('/pickup/verify', {
      childId: form.childId,
      name: form.name,
      idNumber: form.idNumber
    })
  } finally {
    verifying.value = false
  }
}

async function submitRecord(result) {
  if (result === 'DENIED' && !denyReason.value.trim()) {
    return ElMessage.warning('请填写拒绝原因')
  }
  submitting.value = true
  try {
    await http.post('/pickup/records', {
      childId: form.childId,
      pickupPersonName: form.name,
      pickupPersonIdNumber: form.idNumber,
      verifyMethod: form.verifyMethod,
      result,
      denyReason: result === 'DENIED' ? denyReason.value : null
    })
    ElMessage.success(result === 'SUCCESS' ? '已放行，接送记录已保存' : '已拒绝并创建协同事件')
    denyVisible.value = false
    denyReason.value = ''
    verifyResult.value = null
    form.name = ''
    form.idNumber = ''
    records.value = await http.get('/pickup/records')
    delegations.value = await http.get('/delegations/today')
  } finally {
    submitting.value = false
  }
}

onMounted(loadBase)
</script>

<style scoped>
.mt16 { margin-top: 16px; }
.mb12 { margin-bottom: 12px; }
.msg { margin: 6px 0; color: #606266; }
</style>
