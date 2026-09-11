<template>
  <div>
    <el-card>
      <template #header><b>每日菜单</b></template>
      <div class="toolbar">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="loadMenus" />
        <el-button type="primary" :icon="Plus" @click="openMenu()">发布/更新菜单</el-button>
        <el-alert class="tip" type="info" :closable="false" show-icon
                  title="发布菜单后点击「比对受影响儿童」，平台将自动比对在托儿童过敏档案与菜单食材；命中或需过敏餐的儿童可一键发起替换方案。" />
      </div>

      <el-table :data="menus" border>
        <el-table-column label="餐次" width="90">
          <template #default="{ row }">
            <el-tag :type="dType('mealType', row.mealType)">{{ d('mealType', row.mealType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dishes" label="菜品" min-width="220" />
        <el-table-column prop="ingredients" label="主要食材" min-width="200" show-overflow-tooltip />
        <el-table-column prop="nutritionNotes" label="营养说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="warning" plain @click="openAffected(row)">比对受影响儿童</el-button>
            <el-button size="small" @click="openMenu(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="当日尚未发布菜单" /></template>
      </el-table>
    </el-card>

    <!-- 发布/更新菜单 -->
    <el-dialog v-model="menuVisible" :title="`${date} 菜单`" width="560px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="餐次" required>
          <el-radio-group v-model="menuForm.mealType">
            <el-radio-button value="LUNCH">午餐</el-radio-button>
            <el-radio-button value="SNACK">午点</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜品" required>
          <el-input v-model="menuForm.dishes" type="textarea" :rows="2"
                    placeholder="如：花生酱拌面、清炒时蔬、番茄蛋花汤" />
        </el-form-item>
        <el-form-item label="主要食材">
          <el-input v-model="menuForm.ingredients" type="textarea" :rows="2"
                    placeholder="用于平台过敏源比对，如：花生酱、面条、青菜、番茄、鸡蛋" />
        </el-form-item>
        <el-form-item label="营养说明">
          <el-input v-model="menuForm.nutritionNotes" placeholder="如：碳水+优质蛋白+绿叶蔬菜" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="menuVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMenu">保存（同一餐次重复保存为更新）</el-button>
      </template>
    </el-dialog>

    <!-- 平台比对：受影响儿童 -->
    <el-dialog v-model="affectedVisible" title="平台比对：受影响儿童" width="760px" destroy-on-close>
      <el-alert type="warning" :closable="false" show-icon class="mb12"
                :title="`菜单食材：${currentMenu?.ingredients || '—'}｜平台已比对在托儿童过敏档案`" />
      <el-table :data="affected" border>
        <el-table-column label="儿童" width="100">
          <template #default="{ row }">{{ row.child.name }}</template>
        </el-table-column>
        <el-table-column label="班级" width="100">
          <template #default="{ row }">{{ row.child.classroom?.name || '—' }}</template>
        </el-table-column>
        <el-table-column label="命中过敏源" width="130">
          <template #default="{ row }">
            <el-tag v-for="a in row.matchedAllergens" :key="a" type="danger" size="small" class="mr4">{{ a }}</el-tag>
            <span v-if="!row.matchedAllergens?.length">—</span>
          </template>
        </el-table-column>
        <el-table-column label="过敏档案" min-width="180">
          <template #default="{ row }">{{ row.child.allergyHistory }}</template>
        </el-table-column>
        <el-table-column label="状态" width="200">
          <template #default="{ row }">
            <template v-if="row.existingSubstitution">
              <el-tag :type="dType('mealSubStatus', row.existingSubstitution.status)" size="small">
                {{ d('mealSubStatus', row.existingSubstitution.status) }}
              </el-tag>
            </template>
            <el-button v-else size="small" type="primary" plain @click="openSub(row)">发起替换方案</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="无受影响儿童（未命中过敏源且无需过敏餐儿童）" /></template>
      </el-table>
    </el-dialog>

    <!-- 发起替换方案 -->
    <el-dialog v-model="subVisible" title="发起过敏餐临时替换" width="600px" destroy-on-close>
      <el-alert v-if="subForm.matchedAllergy" type="error" :closable="false" show-icon class="mb12"
                :title="`平台比对命中过敏源：${subForm.matchedAllergy}，替代食材经平台核验后方可提交`" />
      <el-form label-width="90px">
        <el-form-item label="儿童">
          <b>{{ subForm.childName }}</b>
          <span class="muted">（{{ subForm.allergy }}）</span>
        </el-form-item>
        <el-form-item label="触发原因" required>
          <el-radio-group v-model="subForm.reason">
            <el-radio-button value="ALLERGEN_RISK">过敏源风险</el-radio-button>
            <el-radio-button value="INGREDIENT_SHORTAGE">食材缺货</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="触发说明">
          <el-input v-model="subForm.triggerDetail" placeholder="如：今日午餐含花生酱 / 鲜虾临时缺货" />
        </el-form-item>
        <el-form-item label="原菜品" required>
          <el-input v-model="subForm.originalDish" placeholder="如：花生酱拌面" />
        </el-form-item>
        <el-form-item label="替代菜品" required>
          <el-input v-model="subForm.substituteDish" placeholder="如：鸡丝麻酱拌面" />
        </el-form-item>
        <el-form-item label="替代食材">
          <el-input v-model="subForm.substituteIngredients" placeholder="如：芝麻酱、鸡胸肉、面条、青菜" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon
                  title="提交后平台自动生成营养比对说明，并依次推送保健老师 → 班级老师 → 家长确认；家长确认后厨房方可出餐。" />
      </el-form>
      <template #footer>
        <el-button @click="subVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSub">提交并推送确认</el-button>
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
import { d, dType } from '../../utils/dicts'

const date = ref(dayjs().format('YYYY-MM-DD'))
const menus = ref([])
const affected = ref([])
const currentMenu = ref(null)
const menuVisible = ref(false)
const affectedVisible = ref(false)
const subVisible = ref(false)
const saving = ref(false)

const menuForm = reactive({ id: null, mealType: 'LUNCH', dishes: '', ingredients: '', nutritionNotes: '' })
const subForm = reactive({
  menuId: null, childId: null, childName: '', allergy: '', matchedAllergy: '',
  reason: 'ALLERGEN_RISK', triggerDetail: '', originalDish: '', substituteDish: '', substituteIngredients: ''
})

async function loadMenus() {
  menus.value = await http.get('/meals/menus', { params: { date: date.value } })
}

function openMenu(row) {
  if (row) {
    Object.assign(menuForm, {
      id: row.id, mealType: row.mealType, dishes: row.dishes,
      ingredients: row.ingredients || '', nutritionNotes: row.nutritionNotes || ''
    })
  } else {
    Object.assign(menuForm, { id: null, mealType: 'LUNCH', dishes: '', ingredients: '', nutritionNotes: '' })
  }
  menuVisible.value = true
}

async function saveMenu() {
  if (!menuForm.dishes.trim()) return ElMessage.warning('请填写菜品')
  saving.value = true
  try {
    await http.post('/meals/menus', { date: date.value, ...menuForm })
    ElMessage.success('菜单已保存')
    menuVisible.value = false
    loadMenus()
  } finally {
    saving.value = false
  }
}

async function openAffected(menu) {
  currentMenu.value = menu
  affected.value = await http.get(`/meals/menus/${menu.id}/affected-children`)
  affectedVisible.value = true
}

function openSub(row) {
  Object.assign(subForm, {
    menuId: currentMenu.value.id,
    childId: row.child.id,
    childName: row.child.name,
    allergy: row.child.allergyHistory || '无过敏档案',
    matchedAllergy: (row.matchedAllergens || []).join('、'),
    reason: row.matchedAllergens?.length ? 'ALLERGEN_RISK' : 'INGREDIENT_SHORTAGE',
    triggerDetail: row.matchedAllergens?.length ? `当日菜单含「${row.matchedAllergens.join('、')}」，命中儿童过敏档案` : '',
    originalDish: '', substituteDish: '', substituteIngredients: ''
  })
  subVisible.value = true
}

async function saveSub() {
  if (!subForm.originalDish.trim() || !subForm.substituteDish.trim()) {
    return ElMessage.warning('请填写原菜品与替代菜品')
  }
  saving.value = true
  try {
    await http.post('/meals/substitutions', subForm)
    ElMessage.success('替换方案已提交，待保健老师确认')
    subVisible.value = false
    affectedVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(loadMenus)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; flex-wrap: wrap; }
.tip { flex: 1; min-width: 320px; }
.mb12 { margin-bottom: 12px; }
.mr4 { margin-right: 4px; }
.muted { color: #909399; font-size: 12px; }
</style>
