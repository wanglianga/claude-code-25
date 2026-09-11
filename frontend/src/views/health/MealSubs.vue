<template>
  <div>
    <el-card>
      <template #header><b>替换餐确认（保健老师）</b></template>
      <el-alert type="info" :closable="false" show-icon class="mb12"
                title="核对平台比对结果（儿童过敏档案 × 替代食材 × 营养要求）后确认；确认后流转班级老师与家长。拒绝后该儿童当日进入人工照护提示。" />

      <el-table :data="subs" border>
        <el-table-column label="儿童" width="90">
          <template #default="{ row }">{{ row.child?.name }}</template>
        </el-table-column>
        <el-table-column label="日期/餐次" width="130">
          <template #default="{ row }">
            {{ row.mealDate }} <el-tag :type="dType('mealType', row.menu?.mealType)" size="small">{{ d('mealType', row.menu?.mealType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="替换方案" min-width="200">
          <template #default="{ row }">
            <div>
              <el-tag :type="dType('mealReason', row.reason)" size="small" class="mr4">{{ d('mealReason', row.reason) }}</el-tag>
              <span class="old">{{ row.originalDish }}</span> → <b>{{ row.substituteDish }}</b>
            </div>
            <div class="muted">替代食材：{{ row.substituteIngredients || '—' }}<span v-if="row.matchedAllergy">｜命中过敏源：{{ row.matchedAllergy }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="平台比对" min-width="240">
          <template #default="{ row }"><span class="check">{{ row.nutritionCheck }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="dType('mealSubStatus', row.status)">{{ d('mealSubStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING_HEALTH'">
              <el-button size="small" type="primary" @click="confirm(row, true)">确认</el-button>
              <el-button size="small" type="danger" plain @click="confirm(row, false)">拒绝</el-button>
            </template>
            <span v-else class="muted">{{ row.healthConfirmedBy?.name || '' }} {{ fmt(row.healthConfirmedAt) }}</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无替换餐方案" /></template>
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

const subs = ref([])
const fmt = (t) => (t ? dayjs(t).format('MM-DD HH:mm') : '')

async function load() {
  subs.value = await http.get('/meals/substitutions')
}

async function confirm(row, approve) {
  const { value } = await ElMessageBox.prompt(
    approve ? '确认该替换方案？（将流转班级老师与家长确认）' : '拒绝该替换方案？该儿童当日将进入人工照护提示',
    approve ? '保健确认' : '保健拒绝',
    { confirmButtonText: '提交', cancelButtonText: '取消', inputPlaceholder: approve ? '确认意见（可选）' : '请填写拒绝原因' }
  )
  await http.post(`/meals/substitutions/${row.id}/health-confirm`, { approve, note: value || '' })
  ElMessage.success(approve ? '已确认，流转班级老师' : '已拒绝')
  load()
}

onMounted(load)
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.old { text-decoration: line-through; color: #909399; }
.muted { color: #909399; font-size: 12px; }
.mr4 { margin-right: 4px; }
.check { font-size: 12px; color: #606266; }
</style>
