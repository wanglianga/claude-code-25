<template>
  <div>
    <el-card>
      <template #header><b>替换餐确认</b></template>
      <el-alert type="info" :closable="false" show-icon class="mb12"
                title="厨房因食材缺货或当日菜单含孩子过敏源发起临时替换，方案已经保健老师与班级老师审核。请您确认；若未及时确认，孩子当日将进入人工照护提示，园方不会发放常规餐，避免误食。" />

      <template v-if="subs.length">
        <el-card v-for="s in subs" :key="s.id" shadow="never" class="sub-card"
                 :class="{ pending: s.status === 'PENDING_PARENT' }">
          <div class="head">
            <b>{{ s.child?.name }}</b>
            <el-tag size="small" effect="plain">{{ s.mealDate }} {{ d('mealType', s.menu?.mealType) }}</el-tag>
            <el-tag :type="dType('mealReason', s.reason)" size="small">{{ d('mealReason', s.reason) }}</el-tag>
            <el-tag :type="dType('mealSubStatus', s.status)" size="small">{{ d('mealSubStatus', s.status) }}</el-tag>
          </div>
          <el-descriptions :column="1" border size="small" class="mt8">
            <el-descriptions-item label="触发说明">{{ s.triggerDetail || '—' }}</el-descriptions-item>
            <el-descriptions-item label="原菜品">{{ s.originalDish }}</el-descriptions-item>
            <el-descriptions-item label="替代菜品">
              <b>{{ s.substituteDish }}</b>（{{ s.substituteIngredients || '—' }}）
            </el-descriptions-item>
            <el-descriptions-item label="平台比对">{{ s.nutritionCheck }}</el-descriptions-item>
            <el-descriptions-item label="园内确认">
              保健老师 {{ s.healthConfirmedBy?.name || '—' }} {{ fmt(s.healthConfirmedAt) }}
              <span v-if="s.healthNote">（{{ s.healthNote }}）</span>；
              班级老师 {{ s.teacherConfirmedBy?.name || '—' }} {{ fmt(s.teacherConfirmedAt) }}
              <span v-if="s.teacherNote">（{{ s.teacherNote }}）</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="s.parentConfirmedAt" label="我的确认">
              {{ fmt(s.parentConfirmedAt) }}<span v-if="s.parentNote">（{{ s.parentNote }}）</span>
              <span v-if="s.kitchenExecutedAt">｜厨房出餐 {{ fmt(s.kitchenExecutedAt) }}</span>
              <span v-if="s.servedAt">｜已分餐 {{ fmt(s.servedAt) }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <div v-if="s.status === 'PENDING_PARENT'" class="ops">
            <el-button type="primary" @click="confirm(s, true)">同意替换</el-button>
            <el-button type="danger" plain @click="confirm(s, false)">不同意（孩子转入人工照护）</el-button>
          </div>
        </el-card>
      </template>
      <el-empty v-else description="暂无替换餐方案" />
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

async function confirm(s, approve) {
  const { value } = await ElMessageBox.prompt(
    approve ? `确认同意将「${s.originalDish}」替换为「${s.substituteDish}」？` : '不同意该替换方案？孩子当日将转入人工照护，园方不会发放常规餐',
    approve ? '家长确认' : '家长拒绝',
    { confirmButtonText: '提交', cancelButtonText: '取消', inputPlaceholder: '留言（可选）' }
  )
  await http.post(`/meals/substitutions/${s.id}/parent-confirm`, { approve, note: value || '' })
  ElMessage.success(approve ? '已确认，厨房将安排出餐' : '已拒绝，孩子将转入人工照护')
  load()
}

onMounted(load)
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.mt8 { margin-top: 8px; }
.sub-card { margin-bottom: 12px; }
.sub-card.pending { border-color: #e6a23c; }
.head { display: flex; gap: 8px; align-items: center; margin-bottom: 4px; }
.ops { margin-top: 10px; display: flex; gap: 10px; }
</style>
