<template>
  <div>
    <!-- 待我确认的替换方案 -->
    <el-card v-if="pendingConfirm.length" class="mb16">
      <template #header><b class="warn">待我确认的替换方案（{{ pendingConfirm.length }}）</b></template>
      <div v-for="s in pendingConfirm" :key="s.id" class="confirm-row">
        <div class="grow">
          <b>{{ s.child?.name }}</b>
          <el-tag :type="dType('mealReason', s.reason)" size="small" class="ml4">{{ d('mealReason', s.reason) }}</el-tag>
          <div>原「<span class="old">{{ s.originalDish }}</span>」→ 替代「<b>{{ s.substituteDish }}</b>」（{{ s.substituteIngredients || '—' }}）</div>
          <div class="muted">{{ s.nutritionCheck }}</div>
          <div class="muted">保健老师 {{ s.healthConfirmedBy?.name }} 已确认 {{ fmt(s.healthConfirmedAt) }}<span v-if="s.healthNote">：{{ s.healthNote }}</span></div>
        </div>
        <div class="ops">
          <el-button size="small" type="primary" @click="confirm(s, true)">确认</el-button>
          <el-button size="small" type="danger" plain @click="confirm(s, false)">拒绝</el-button>
        </div>
      </div>
    </el-card>

    <el-card>
      <template #header><b>分餐页 · 替代餐标记与家长确认时间</b></template>
      <div class="toolbar">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="load" />
        <template v-if="board.menus?.length">
          <el-tag v-for="m in board.menus" :key="m.id" :type="dType('mealType', m.mealType)" effect="plain">
            {{ d('mealType', m.mealType) }}：{{ m.dishes }}
          </el-tag>
        </template>
        <el-tag v-else type="info" effect="plain">厨房尚未发布当日菜单</el-tag>
      </div>

      <el-alert v-if="manualCareRows.length" type="error" :closable="false" show-icon class="mb12"
                :title="`人工照护提示：${manualCareRows.map(r => r.child.name).join('、')} 的家长未确认替换方案（或已拒绝），禁止发放常规餐与未确认替代餐，请人工照护并联系家长/保健老师，避免误食。`" />

      <el-table :data="board.children || []" border :row-class-name="rowCls">
        <el-table-column label="儿童" width="90">
          <template #default="{ row }">{{ row.child.name }}</template>
        </el-table-column>
        <el-table-column label="过敏档案" min-width="150">
          <template #default="{ row }">
            <template v-if="row.allergens?.length">
              <el-tag v-for="a in row.allergens" :key="a" type="danger" size="small" class="mr4">{{ a }}</el-tag>
            </template>
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="当日餐食安排" min-width="300">
          <template #default="{ row }">
            <template v-if="row.substitutions?.length">
              <div v-for="s in row.substitutions" :key="s.id" class="sub-line">
                <el-tag type="warning" size="small" effect="dark">替代餐</el-tag>
                <span class="old ml4">{{ s.originalDish }}</span> → <b>{{ s.substituteDish }}</b>
                <el-tag :type="dType('mealSubStatus', s.status)" size="small" class="ml4">{{ d('mealSubStatus', s.status) }}</el-tag>
                <div class="muted">
                  家长确认时间：<b :class="{ danger: !s.parentConfirmedAt }">{{ s.parentConfirmedAt ? fmt(s.parentConfirmedAt) : '未确认' }}</b>
                  <span v-if="s.kitchenExecutedAt">｜厨房出餐 {{ fmt(s.kitchenExecutedAt) }}</span>
                  <span v-if="s.servedAt">｜已分餐 {{ fmt(s.servedAt) }}</span>
                </div>
              </div>
            </template>
            <span v-else class="muted">常规餐</span>
          </template>
        </el-table-column>
        <el-table-column label="提示" min-width="180">
          <template #default="{ row }">
            <el-tag v-if="row.manualCare" type="danger" effect="dark">人工照护 · 禁止发放常规餐</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <template v-for="s in row.substitutions" :key="s.id">
              <el-button v-if="s.status === 'EXECUTED'" size="small" type="primary"
                         :loading="acting === s.id" @click="serve(s)">确认分餐</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import http from '../../api'
import { d, dType } from '../../utils/dicts'

const date = ref(dayjs().format('YYYY-MM-DD'))
const board = ref({ menus: [], children: [] })
const acting = ref(null)

const fmt = (t) => (t ? dayjs(t).format('HH:mm') : '')

const pendingConfirm = computed(() =>
  (board.value.children || []).flatMap((r) => r.substitutions || [])
    .filter((s) => s.status === 'PENDING_TEACHER'))

const manualCareRows = computed(() => (board.value.children || []).filter((r) => r.manualCare))

const rowCls = ({ row }) => (row.manualCare ? 'manual-care-row' : '')

async function load() {
  board.value = await http.get('/meals/serving-board', { params: { date: date.value } })
}

async function confirm(s, approve) {
  const { value } = await ElMessageBox.prompt(
    approve ? '确认该替换方案？（将推送家长确认）' : '拒绝该替换方案？该儿童将进入人工照护提示',
    approve ? '班级老师确认' : '班级老师拒绝',
    { confirmButtonText: '提交', cancelButtonText: '取消', inputPlaceholder: approve ? '确认意见（可选）' : '请填写拒绝原因' }
  )
  await http.post(`/meals/substitutions/${s.id}/teacher-confirm`, { approve, note: value || '' })
  ElMessage.success(approve ? '已确认，等待家长确认' : '已拒绝')
  load()
}

async function serve(s) {
  acting.value = s.id
  try {
    await http.post(`/meals/substitutions/${s.id}/serve`)
    ElMessage.success('已确认分餐，当日饮食记录已同步')
    load()
  } finally {
    acting.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.mb12 { margin-bottom: 12px; }
.ml4 { margin-left: 4px; }
.mr4 { margin-right: 4px; }
.warn { color: #b88230; }
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; flex-wrap: wrap; }
.old { text-decoration: line-through; color: #909399; }
.muted { color: #909399; font-size: 12px; }
.danger { color: #f56c6c; }
.sub-line { margin: 4px 0; }
.confirm-row { display: flex; gap: 12px; align-items: center; padding: 8px 0; border-bottom: 1px dashed #e4e7ed; }
.confirm-row:last-child { border-bottom: none; }
.grow { flex: 1; }
.ops { display: flex; gap: 8px; }
:deep(.manual-care-row) { background: #fef0f0; }
</style>
