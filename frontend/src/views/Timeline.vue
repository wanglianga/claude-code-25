<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <b>时间轴（按儿童 / 班级 / 事件核对环节连续性）</b>
          <el-space>
            <el-radio-group v-model="dimension" size="small" @change="onDimensionChange">
              <el-radio-button value="child">按儿童</el-radio-button>
              <el-radio-button value="class">按班级</el-radio-button>
              <el-radio-button value="event">按事件</el-radio-button>
            </el-radio-group>
            <el-select v-if="dimension === 'child'" v-model="childId" filterable placeholder="选择儿童"
                       style="width: 200px" size="small" @change="load">
              <el-option v-for="c in children" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select v-if="dimension === 'class'" v-model="classId" placeholder="选择班级" style="width: 160px"
                       size="small" @change="load">
              <el-option v-for="c in classrooms" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select v-if="dimension === 'event'" v-model="eventId" filterable placeholder="选择事件"
                       style="width: 260px" size="small" @change="load">
              <el-option v-for="e in events" :key="e.id" :label="`#${e.id} ${e.title}`" :value="e.id" />
            </el-select>
          </el-space>
        </div>
      </template>

      <el-timeline v-if="items.length" class="pt8">
        <el-timeline-item v-for="(item, i) in items" :key="i" :timestamp="fmt(item.time)" placement="top"
                          :type="tlDot(item.category)">
          <el-tag size="small" :type="dType('timelineCategory', item.category)">
            {{ d('timelineCategory', item.category) }}
          </el-tag>
          <b class="ml4">{{ item.title }}</b>
          <div class="detail">{{ item.detail }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="请选择对象查看时间轴" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import http from '../api'
import { d, dType } from '../utils/dicts'

const route = useRoute()
const dimension = ref('child')
const childId = ref(null)
const classId = ref(null)
const eventId = ref(null)
const children = ref([])
const classrooms = ref([])
const events = ref([])
const items = ref([])

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')
const tlDot = (cat) => ({ EVENT: 'danger', ALERT: 'danger', MORNING_CHECK: 'warning', PICKUP: 'primary' }[cat] || 'success')

async function load() {
  items.value = []
  if (dimension.value === 'child' && childId.value) {
    items.value = await http.get(`/timeline/child/${childId.value}`)
  } else if (dimension.value === 'class' && classId.value) {
    items.value = await http.get(`/timeline/class/${classId.value}`)
  } else if (dimension.value === 'event' && eventId.value) {
    items.value = await http.get(`/timeline/event/${eventId.value}`)
  }
}

async function onDimensionChange() {
  items.value = []
  if (dimension.value === 'class' && !classrooms.value.length) {
    classrooms.value = await http.get('/classrooms')
    if (classrooms.value.length) classId.value = classrooms.value[0].id
  }
  if (dimension.value === 'event' && !events.value.length) {
    events.value = await http.get('/events')
    if (events.value.length) eventId.value = events.value[0].id
  }
  load()
}

onMounted(async () => {
  children.value = await http.get('/children')
  if (route.query.childId) {
    childId.value = Number(route.query.childId)
  } else if (children.value.length) {
    childId.value = children.value[0].id
  }
  load()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pt8 { padding-top: 8px; }
.ml4 { margin-left: 4px; }
.detail { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
