<template>
  <div v-loading="loading">
    <template v-if="data.event">
      <el-card>
        <template #header>
          <div class="card-header">
            <el-space>
              <el-tag :type="dType('eventType', data.event.type)">{{ d('eventType', data.event.type) }}</el-tag>
              <b>{{ data.event.title }}</b>
              <el-tag :type="dType('eventStatus', data.event.status)" size="small">
                {{ d('eventStatus', data.event.status) }}
              </el-tag>
            </el-space>
            <el-space v-if="isStaff">
              <el-button v-if="data.event.status === 'OPEN'" type="warning" size="small"
                         @click="changeStatus('PROCESSING')">开始处理</el-button>
              <el-button v-if="data.event.status === 'PROCESSING'" type="success" size="small"
                         @click="resolveVisible = true">标记解决</el-button>
              <el-button v-if="data.event.status === 'RESOLVED'" type="info" size="small"
                         @click="changeStatus('CLOSED')">关闭事件</el-button>
            </el-space>
          </div>
        </template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="儿童">
            {{ data.event.child?.name }}（{{ data.event.classroom?.name || '未分班' }}）
          </el-descriptions-item>
          <el-descriptions-item label="创建">
            {{ data.event.createdBy?.name || '系统' }}｜{{ fmt(data.event.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="事件描述" :span="2">{{ data.event.description || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="data.event.resolutionNote" label="处理结论" :span="2">
            <span class="resolution">{{ data.event.resolutionNote }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="participants">
          <span class="label">参与方：</span>
          <el-tag v-for="p in data.participants" :key="p.id" size="small" class="mr4" effect="plain">
            {{ p.user?.name }}（{{ dicts.role[p.roleLabel] || p.roleLabel }}）
          </el-tag>
        </div>
        <el-button link type="primary" @click="showTimeline = !showTimeline">
          {{ showTimeline ? '收起' : '查看' }}事件时间轴（核对晨检/照护/接送/沟通是否连续）
        </el-button>
        <el-timeline v-if="showTimeline" class="mt12">
          <el-timeline-item v-for="(item, i) in eventTimeline" :key="i" :timestamp="fmt(item.time)" placement="top">
            <el-tag size="small" :type="dType('timelineCategory', item.category)">
              {{ d('timelineCategory', item.category) }}
            </el-tag>
            <b class="ml4">{{ item.title }}</b>
            <div class="detail">{{ item.detail }}</div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card class="mt16">
        <template #header><b>多方沟通</b></template>
        <div class="messages">
          <div v-for="m in data.messages" :key="m.id" class="message"
               :class="{ mine: m.sender?.id === auth.user?.id }">
            <div class="meta">
              <el-tag size="small" :type="m.sender?.role === 'PARENT' ? 'warning' : 'primary'" effect="plain">
                {{ m.sender?.name }}（{{ dicts.role[m.sender?.role] || '' }}）
              </el-tag>
              <span class="time">{{ fmt(m.createdAt) }}</span>
            </div>
            <div class="content">{{ m.content }}</div>
          </div>
          <el-empty v-if="!data.messages.length" description="暂无沟通记录" :image-size="60" />
        </div>
        <div v-if="data.event.status !== 'CLOSED'" class="reply">
          <el-input v-model="newMessage" type="textarea" :rows="2"
                    placeholder="发表处理进展、说明情况或回复其他方…" />
          <el-button type="primary" class="mt8" :loading="sending" @click="send">发送</el-button>
        </div>
      </el-card>
    </template>

    <el-dialog v-model="resolveVisible" title="标记解决" width="440px" destroy-on-close>
      <el-input v-model="resolveNote" type="textarea" :rows="3"
                placeholder="处理结论（将用于风险复盘），如：家长已接回就医，凭复课证明返园" />
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="success" @click="changeStatus('RESOLVED', resolveNote)">确认解决</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import http from '../api'
import { d, dType, dicts } from '../utils/dicts'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const auth = useAuthStore()
const eventId = route.params.id

const data = ref({})
const loading = ref(false)
const newMessage = ref('')
const sending = ref(false)
const resolveVisible = ref(false)
const resolveNote = ref('')
const showTimeline = ref(false)
const eventTimeline = ref([])

const isStaff = computed(() => auth.role !== 'PARENT')
const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loading.value = true
  try {
    data.value = await http.get(`/events/${eventId}`)
    eventTimeline.value = await http.get(`/timeline/event/${eventId}`)
  } finally {
    loading.value = false
  }
}

async function send() {
  if (!newMessage.value.trim()) return ElMessage.warning('请输入内容')
  sending.value = true
  try {
    await http.post(`/events/${eventId}/messages`, { content: newMessage.value })
    newMessage.value = ''
    load()
  } finally {
    sending.value = false
  }
}

async function changeStatus(status, note) {
  if (status === 'RESOLVED' && !note?.trim()) {
    return ElMessage.warning('请填写处理结论')
  }
  await http.put(`/events/${eventId}/status`, { status, note })
  ElMessage.success('状态已更新')
  resolveVisible.value = false
  resolveNote.value = ''
  load()
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.mt16 { margin-top: 16px; }
.mt12 { margin-top: 12px; }
.mt8 { margin-top: 8px; }
.ml4 { margin-left: 4px; }
.mr4 { margin-right: 4px; }
.participants { margin-top: 12px; }
.participants .label { color: #909399; font-size: 13px; }
.resolution { color: #67c23a; }
.messages { max-height: 420px; overflow-y: auto; }
.message { padding: 8px 12px; border-radius: 8px; background: #f4f4f5; margin-bottom: 10px; }
.message.mine { background: #ecf5ff; }
.meta { display: flex; justify-content: space-between; margin-bottom: 4px; }
.time { color: #909399; font-size: 12px; }
.content { white-space: pre-wrap; }
.reply { margin-top: 12px; }
.detail { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
