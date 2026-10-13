<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh, InfoFilled, Collection } from '@element-plus/icons-vue'
import { api } from '../api'
import { showError, isCancel, trimmedRequired, textLength } from '../utils'

const items = ref([])
const equipment = ref([])
const loading = ref(false)
const saving = ref(false)
const busyId = ref(null)
const loadError = ref('')
const keyword = ref('')
const appliedKeyword = ref('')
const page = ref(1)
const pageSize = 10
const dialogOpen = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '' })
const rules = { name: [trimmedRequired('分类名称'), textLength(1, 50, '分类名称')] }
const categoryCounts = computed(() => {
  const counts = new Map()
  for (const item of equipment.value) counts.set(item.categoryId, (counts.get(item.categoryId) || 0) + 1)
  return counts
})
const filtered = computed(() => items.value.filter(item => item.name.toLocaleLowerCase().includes(appliedKeyword.value.toLocaleLowerCase())))
const rows = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))
let sequence = 0
async function load() {
  const current = ++sequence
  loading.value = true
  loadError.value = ''
  try {
    const [categories, equipmentList] = await Promise.all([api.categories(), api.equipment()])
    if (current !== sequence) return
    items.value = categories
    equipment.value = equipmentList
    if ((page.value - 1) * pageSize >= filtered.value.length) page.value = 1
  } catch (error) { if (current === sequence) { loadError.value = error.message; showError(error) } }
  finally { if (current === sequence) loading.value = false }
}
function search() { appliedKeyword.value = keyword.value.trim(); page.value = 1 }
function reset() { keyword.value = ''; search() }
function edit(item) {
  editingId.value = item?.id || null
  form.name = item?.name || ''
  dialogOpen.value = true
}
async function save() {
  if (saving.value || !await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    await api.saveCategory(editingId.value, { name: form.name.trim() })
    ElMessage.success(editingId.value ? '分类名称已更新' : '分类已添加')
    dialogOpen.value = false
    await load()
  } catch (error) { showError(error) }
  finally { saving.value = false }
}
async function remove(item) {
  if (busyId.value !== null) return
  busyId.value = item.id
  try {
    await ElMessageBox.confirm('确认删除分类「' + item.name + '」？已关联器材的分类不能删除。', '删除分类', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' })
    await api.deleteCategory(item.id)
    ElMessage.success('分类已删除')
    await load()
  } catch (error) { if (!isCancel(error)) showError(error) }
  finally { busyId.value = null }
}
onMounted(load)
</script>

<template>
  <div class="notice-strip"><el-icon><InfoFilled /></el-icon><span>分类用于组织和筛选器材。已关联器材的分类，需要先调整器材所属分类才能删除。</span></div>
  <section class="panel">
    <div class="panel-header"><div><h2 class="panel-title">器材分类</h2><p class="panel-subtitle">共 {{ items.length }} 个分类</p></div><div class="action-buttons"><el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button><el-button type="primary" :icon="Plus" :disabled="loading || saving || busyId !== null" @click="edit()">添加分类</el-button></div></div>
    <form class="filter-bar" @submit.prevent="search"><el-input v-model="keyword" class="keyword-input" :prefix-icon="Search" clearable maxlength="50" placeholder="搜索分类名称" /><el-button type="primary" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button></form>
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="load-error" />
    <el-table v-loading="loading" :data="rows" :empty-text="loadError ? '数据加载失败，请刷新重试' : '暂无符合条件的分类'">
      <el-table-column label="分类名称" min-width="220"><template #default="{ row }"><div class="category-name"><span class="category-badge"><el-icon><Collection /></el-icon></span><span class="cell-name">{{ row.name }}</span></div></template></el-table-column>
      <el-table-column label="关联器材品种" min-width="150"><template #default="{ row }"><span class="quantity">{{ categoryCounts.get(row.id) || 0 }}</span><span class="cell-secondary"> 种</span></template></el-table-column>
      <el-table-column label="分类编号" min-width="110"><template #default="{ row }"><span class="cell-secondary">#{{ row.id }}</span></template></el-table-column>
      <el-table-column label="操作" width="160" fixed="right"><template #default="{ row }"><div class="action-buttons"><el-button link type="primary" :disabled="busyId !== null" @click="edit(row)">编辑</el-button><el-tooltip :disabled="!categoryCounts.get(row.id)" content="已关联器材，请先调整器材所属分类" placement="top"><span><el-button link type="danger" :loading="busyId === row.id" :disabled="!!categoryCounts.get(row.id) || busyId !== null && busyId !== row.id" @click="remove(row)">删除</el-button></span></el-tooltip></div></template></el-table-column>
    </el-table>
    <div class="table-footer"><span>共 {{ filtered.length }} 个分类</span><el-pagination v-model:current-page="page" :page-size="pageSize" :total="filtered.length" layout="prev, pager, next" /></div>
  </section>
  <el-dialog v-model="dialogOpen" :title="editingId ? '编辑分类' : '添加分类'" width="450px" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @opened="formRef?.clearValidate()">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="save"><el-form-item label="分类名称" prop="name"><el-input v-model="form.name" maxlength="50" show-word-limit placeholder="例如：球类器材" /></el-form-item><p class="form-hint">使用简短、明确的名称，便于查找器材。</p></el-form>
    <template #footer><el-button :disabled="saving" @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存分类</el-button></template>
  </el-dialog>
</template>
