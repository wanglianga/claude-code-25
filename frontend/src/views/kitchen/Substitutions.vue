<template>
  <div>
    <el-card>
      <template #header><b>替换餐执行</b></template>
      <div class="toolbar">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="load" />
        <el-alert class="tip" type="info" :closable="false" show-icon
                  title="替换方案经保健老师、班级老师、家长依次确认后，厨房方可出餐；出餐后班级老师在分餐页看到替代餐标记与家长确认时间。" />
      </div>

      <el-table :data="subs" border>
        <el-table-column label="儿童" width="90">
          <template #default="{ row }">{{ row.child?.name }}</template>
        </el-table-column>
        <el-table-column label="餐次" width="80">
          <template #default="{ row }">
            <el-tag :type="dType('mealType', row.menu?.mealType)" size="small">{{ d('mealType', row.menu?.mealType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="替换方案" min-width="220">
          <template #default="{ row }">
            <div>
              <el-tag :type="dType('mealReason', row.reason)" size="small" class="mr4">{{ d('mealReason', row.reason) }}</el-tag>
              <span class="old">{{ row.originalDish }}</span> → <b>{{ row.substituteDish }}</b>
            </div>
            <div class="muted">{{ row.triggerDetail }}</div>
          </template>
        </el-table-column>
        <el-table-column label="确认进度" min-width="240">
          <template #default="{ row }">
            <div class="steps">
              <span :class="stepCls(row.healthConfirmedAt)">保健 {{ fmt(row.healthConfirmedAt) }}</span>
              <span :class="stepCls(row.teacherConfirmedAt)">老师 {{ fmt(row.teacherConfirmedAt) }}</span>
              <span :class="stepCls(row.parentConfirmedAt)">家长 {{ fmt(row.parentConfirmedAt) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="dType('mealSubStatus', row.status)">{{ d('mealSubStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'CONFIRMED'" size="small" type="primary"
                       :loading="acting === row.id" @click="execute(row)">出餐</el-button>
            <el-button v-if="['PENDING_HEALTH','PENDING_TEACHER','PENDING_PARENT','CONFIRMED'].includes(row.status)"
                       size="small" type="danger" plain @click="cancel(row)">取消</el-button>
            <span v-if="row.status === 'EXECUTED'" class="muted">已出餐 {{ fmt(row.kitchenExecutedAt) }}</span>
            <span v-if="row.status === 'SERVED'" class="muted">已分餐 {{ fmt(row.servedAt) }}</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="当日暂无替换单" /></template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import http from '../../api'
import { d, dType } from '../../utils/dicts'

const date = ref(dayjs().format('YYYY-MM-DD'))
const subs = ref([])
const acting = ref(null)

const fmt = (t) => (t ? dayjs(t).format('HH:mm') : '待确认')
const stepCls = (t) => (t ? 'step done' : 'step')

async function load() {
  subs.value = await http.get('/meals/substitutions', { params: { date: date.value } })
}

async function execute(row) {
  acting.value = row.id
  try {
    await http.post(`/meals/substitutions/${row.id}/execute`)
    ElMessage.success(`已出餐：${row.substituteDish}，当日饮食记录已同步`)
    load()
  } finally {
    acting.value = null
  }
}

async function cancel(row) {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消替换单', {
    confirmButtonText: '确认取消', cancelButtonText: '返回', inputPlaceholder: '如：食材已到货'
  })
  await http.post(`/meals/substitutions/${row.id}/cancel`, { note: value || '' })
  ElMessage.success('替换单已取消')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; flex-wrap: wrap; }
.tip { flex: 1; min-width: 320px; }
.old { text-decoration: line-through; color: #909399; }
.muted { color: #909399; font-size: 12px; }
.mr4 { margin-right: 4px; }
.steps { display: flex; gap: 8px; flex-wrap: wrap; }
.step { color: #c0c4cc; font-size: 12px; }
.step.done { color: #67c23a; font-weight: 600; }
</style>
