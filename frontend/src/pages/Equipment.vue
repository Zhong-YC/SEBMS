<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh, InfoFilled, Basketball } from '@element-plus/icons-vue'
import { api } from '../api'
import { auth } from '../auth'
import { showError, isCancel, trimmedRequired, textLength } from '../utils'

const route = useRoute()
const router = useRouter()
const managing = computed(() => route.path === '/manage/equipment')
const canBorrow = computed(() => auth.user?.role === 'USER')
const loading = ref(false)
const saving = ref(false)
const busyId = ref(null)
const items = ref([])
const categories = ref([])
const filter = reactive({ keyword: '', categoryId: '', availability: '' })
const page = ref(1)
const pageSize = ref(10)
const loadError = ref('')
let loadSequence = 0
const filtered = computed(() => items.value.filter(item => (managing.value || item.enabled) && (
  !filter.availability || filter.availability === 'available' && item.enabled && item.availableQuantity > 0 ||
  filter.availability === 'empty' && item.availableQuantity === 0 || filter.availability === 'disabled' && !item.enabled)))
const rows = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
const dialogOpen = ref(false)
const editingId = ref(null)
const borrowedQuantity = ref(0)
const formRef = ref()
const form = reactive({ name: '', description: '', categoryId: null, totalQuantity: 1, enabled: true })
const rules = {
  name: [trimmedRequired('器材名称'), textLength(1, 100, '器材名称')],
  description: [{ max: 500, message: '描述不能超过 500 个字符', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择器材分类', trigger: 'change' }],
  totalQuantity: [{ validator: (_rule, value, done) => Number.isInteger(value) && value >= borrowedQuantity.value && value <= 1000000 ? done() : done(new Error('总数量须为 ' + borrowedQuantity.value + '–1000000 的整数')), trigger: 'blur' }],
}
async function load(resetPage = false) {
  const sequence = ++loadSequence
  loading.value = true
  loadError.value = ''
  if (resetPage) page.value = 1
  try {
    const [equipment, categoryList] = await Promise.all([
      api.equipment({ keyword: filter.keyword.trim() || undefined, categoryId: filter.categoryId || undefined }), api.categories(),
    ])
    if (sequence !== loadSequence) return
    items.value = equipment
    categories.value = categoryList
    if ((page.value - 1) * pageSize.value >= filtered.value.length) page.value = 1
  } catch (error) { if (sequence === loadSequence) { loadError.value = error.message; showError(error) } }
  finally { if (sequence === loadSequence) loading.value = false }
}
function reset() { Object.assign(filter, { keyword: '', categoryId: '', availability: '' }); load(true) }
function edit(item) {
  editingId.value = item?.id || null
  borrowedQuantity.value = item ? item.totalQuantity - item.availableQuantity : 0
  Object.assign(form, item ? { name: item.name, description: item.description || '', categoryId: item.categoryId, totalQuantity: item.totalQuantity, enabled: item.enabled } : { name: '', description: '', categoryId: categories.value[0]?.id || null, totalQuantity: 1, enabled: true })
  dialogOpen.value = true
}
async function save() {
  if (saving.value || !await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    await api.saveEquipment(editingId.value, { ...form, name: form.name.trim(), description: form.description.trim() })
    ElMessage.success(editingId.value ? '器材资料已更新' : '器材已添加')
    dialogOpen.value = false
    await load()
  } catch (error) { showError(error) }
  finally { saving.value = false }
}
async function borrow(item) {
  if (busyId.value !== null) return
  busyId.value = item.id
  try {
    await ElMessageBox.confirm('确认已领取「' + item.name + '」1 件？请在线下交接完成后登记借用。', '登记借用', { confirmButtonText: '确认借用 1 件', cancelButtonText: '取消', type: 'info' })
    await api.borrow(item.id)
    ElMessage.success('借用成功，可在“我的借还”中查看和归还')
    await load()
  } catch (error) { if (!isCancel(error)) { showError(error); if ([400, 409].includes(error.status)) await load() } }
  finally { busyId.value = null }
}
async function remove(item) {
  if (busyId.value !== null) return
  busyId.value = item.id
  try {
    await ElMessageBox.confirm('确认删除器材「' + item.name + '」？存在借用历史的器材无法删除，可改为停用。', '删除器材', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' })
    await api.deleteEquipment(item.id)
    ElMessage.success('器材已删除')
    await load()
  } catch (error) { if (!isCancel(error)) showError(error) }
  finally { busyId.value = null }
}
onMounted(() => load())
</script>

<template>
  <div v-if="!managing && canBorrow" class="notice-strip"><el-icon><InfoFilled /></el-icon><span>每次借用登记 <strong>1 件</strong> 器材。请先完成线下交接，使用后及时在“我的借还”登记归还。</span></div>
  <div v-if="!managing && !canBorrow" class="notice-strip"><el-icon><InfoFilled /></el-icon><span>管理员可查看器材库存。维护器材资料请前往“器材管理”，借用由普通用户登记。</span></div>
  <section class="panel">
    <div class="panel-header"><div><h2 class="panel-title">{{ managing ? '器材清单' : canBorrow ? '可借器材' : '器材目录' }}</h2><p class="panel-subtitle">共 {{ filtered.length }} 个器材品种</p></div><el-button v-if="managing" type="primary" :icon="Plus" :disabled="loading || saving || busyId !== null" @click="edit()">添加器材</el-button><el-button v-else :icon="Refresh" :loading="loading" @click="load()">刷新</el-button></div>
    <form class="filter-bar" @submit.prevent="load(true)">
      <el-input v-model="filter.keyword" class="keyword-input" clearable :prefix-icon="Search" placeholder="搜索器材名称或描述" maxlength="100" />
      <el-select v-model="filter.categoryId" class="category-select" clearable placeholder="全部分类"><el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" /></el-select>
      <el-select v-model="filter.availability" class="status-select" clearable placeholder="全部状态" @change="page = 1"><el-option label="可借用" value="available" /><el-option label="暂无库存" value="empty" /><el-option v-if="managing" label="已停用" value="disabled" /></el-select>
      <el-button type="primary" native-type="submit" :disabled="loading">查询</el-button><el-button :disabled="loading" @click="reset">重置</el-button>
    </form>
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="load-error" />
    <el-table v-loading="loading" :data="rows" :empty-text="loadError ? '数据加载失败，请点击刷新或查询重试' : '暂无符合条件的器材'">
      <el-table-column label="器材名称" min-width="210"><template #default="{ row }"><div class="cell-name">{{ row.name }}</div><div class="cell-secondary">{{ row.description || '暂无描述' }}</div></template></el-table-column>
      <el-table-column prop="categoryName" label="分类" min-width="110" show-overflow-tooltip />
      <el-table-column label="可借 / 总数" min-width="115"><template #default="{ row }"><span class="quantity" :class="{ empty: row.availableQuantity === 0 }">{{ row.availableQuantity }}<span class="quantity-total"> / {{ row.totalQuantity }}</span></span></template></el-table-column>
      <el-table-column label="状态" min-width="105"><template #default="{ row }"><el-tag :type="!row.enabled ? 'info' : row.availableQuantity > 0 ? 'success' : 'warning'" effect="light">{{ !row.enabled ? '已停用' : row.availableQuantity > 0 ? '可借用' : '暂无库存' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" :width="managing ? 160 : 140" fixed="right"><template #default="{ row }"><div class="action-buttons" v-if="managing"><el-button link type="primary" :disabled="busyId !== null" @click="edit(row)">编辑</el-button><el-button link type="danger" :loading="busyId === row.id" :disabled="busyId !== null && busyId !== row.id" @click="remove(row)">删除</el-button></div><el-button v-else-if="canBorrow" type="primary" plain size="small" :icon="Basketball" :loading="busyId === row.id" :disabled="!row.enabled || row.availableQuantity < 1 || busyId !== null && busyId !== row.id" @click="borrow(row)">{{ row.availableQuantity > 0 ? '借用 1 件' : '暂无库存' }}</el-button><span v-else class="cell-secondary">仅普通用户可借用</span></template></el-table-column>
    </el-table>
    <div class="table-footer"><span>共 {{ filtered.length }} 条器材资料</span><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="filtered.length" :page-sizes="[10, 20, 50]" layout="prev, pager, next" /></div>
  </section>
  <el-dialog v-model="dialogOpen" :title="editingId ? '编辑器材' : '添加器材'" width="560px" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @opened="formRef?.clearValidate()">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="save">
      <el-form-item label="器材名称" prop="name"><el-input v-model="form.name" maxlength="100" show-word-limit placeholder="例如：标准篮球" /></el-form-item>
      <el-form-item label="器材分类" prop="categoryId"><el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%"><el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" /></el-select><p v-if="!categories.length" class="form-hint">请先在<el-button link type="primary" @click="dialogOpen = false; router.push('/manage/categories')">分类管理</el-button>中添加分类。</p></el-form-item>
      <el-form-item label="器材描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="规格、用途或使用注意事项（选填）" /></el-form-item>
      <div class="form-grid"><el-form-item label="总数量" prop="totalQuantity"><el-input-number v-model="form.totalQuantity" :min="borrowedQuantity" :max="1000000" :precision="0" style="width:100%" /><p v-if="editingId" class="form-hint">当前借出 {{ borrowedQuantity }} 件，总数不能小于借出数。</p></el-form-item><el-form-item label="使用状态"><el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" inline-prompt /><p class="form-hint">停用后不可新增借用，已有记录仍可归还。</p></el-form-item></div>
    </el-form><template #footer><el-button :disabled="saving" @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存器材</el-button></template>
  </el-dialog>
</template>

