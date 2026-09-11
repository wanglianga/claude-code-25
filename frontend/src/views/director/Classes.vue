<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <b>班级管理（容量 / 师幼比 / 师资）</b>
          <el-button type="primary" :icon="Plus" @click="createVisible = true">新建班级</el-button>
        </div>
      </template>
      <el-table :data="classrooms" v-loading="loading">
        <el-table-column prop="name" label="班级" width="110" />
        <el-table-column prop="ageRange" label="适龄段" width="100" />
        <el-table-column label="在托/容量" width="110">
          <template #default="{ row }">
            <el-tag :type="row.remaining <= 0 ? 'danger' : 'success'" effect="plain">
              {{ row.enrolledCount }}/{{ row.capacity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="师幼比" width="100">
          <template #default="{ row }">
            <el-tag :type="row.ratio === '未配教师' ? 'danger' : 'primary'" effect="plain">{{ row.ratio }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="带班老师">
          <template #default="{ row }">
            <el-tag v-for="t in row.teachers" :key="t.id" class="mr4" closable
                    @close="removeTeacher(row, t)">{{ t.name }}</el-tag>
            <span v-if="!row.teachers?.length" class="muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" show-overflow-tooltip />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssignTeacher(row)">分配老师</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="新建班级" width="440px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="班级名称" required><el-input v-model="createForm.name" /></el-form-item>
        <el-form-item label="容量" required><el-input-number v-model="createForm.capacity" :min="1" :max="60" /></el-form-item>
        <el-form-item label="适龄段"><el-input v-model="createForm.ageRange" placeholder="如：2-3岁" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="createForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" :title="`分配老师 → ${current?.name || ''}`" width="420px" destroy-on-close>
      <el-select v-model="selectedTeacher" placeholder="选择老师" style="width: 100%">
        <el-option v-for="t in teachers" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="assignTeacher">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '../../api'

const classrooms = ref([])
const teachers = ref([])
const loading = ref(false)
const createVisible = ref(false)
const assignVisible = ref(false)
const current = ref(null)
const selectedTeacher = ref(null)
const createForm = reactive({ name: '', capacity: 15, ageRange: '', description: '' })

async function load() {
  loading.value = true
  try {
    classrooms.value = await http.get('/classrooms')
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!createForm.name) return ElMessage.warning('请填写班级名称')
  await http.post('/classrooms', createForm)
  ElMessage.success('班级已创建')
  createVisible.value = false
  load()
}

async function openAssignTeacher(row) {
  current.value = row
  selectedTeacher.value = null
  teachers.value = await http.get('/users', { params: { role: 'TEACHER' } })
  assignVisible.value = true
}

async function assignTeacher() {
  if (!selectedTeacher.value) return ElMessage.warning('请选择老师')
  await http.post(`/classrooms/${current.value.id}/teachers`, { teacherId: selectedTeacher.value })
  ElMessage.success('已分配')
  assignVisible.value = false
  load()
}

async function removeTeacher(row, teacher) {
  await http.delete(`/classrooms/${row.id}/teachers/${teacher.id}`)
  ElMessage.success('已移除')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.mr4 { margin-right: 4px; }
.muted { color: #909399; }
</style>
