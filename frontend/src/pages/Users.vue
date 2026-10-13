<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh, InfoFilled, UserFilled } from '@element-plus/icons-vue'
import { api } from '../api'
import { auth } from '../auth'
import { showError, isCancel, trimmedRequired, textLength, usernameRules, passwordRule } from '../utils'

const items = ref([])
const loading = ref(false)
const saving = ref(false)
const busyId = ref(null)
const loadError = ref('')
const page = ref(1)
const pageSize = 10
const filter = reactive({ keyword: '', role: '', enabled: '' })
const applied = reactive({ keyword: '', role: '', enabled: '' })
const dialogOpen = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ username: '', displayName: '', password: '', role: 'USER', enabled: true })
const isSelf = computed(() => editingId.value === auth.user?.id)
const protectedAdmin = computed(() => {
  const row = items.value.find(item => item.id === editingId.value)
  return row?.enabled && row.role === 'ADMIN' && items.value.filter(item => item.role === 'ADMIN' && item.enabled).length <= 1
})
const rules = computed(() => ({
  username: usernameRules,
  displayName: [trimmedRequired('姓名'), textLength(1, 50, '姓名')],
  password: [passwordRule(!!editingId.value)],
  role: [{ required: true, message: '请选择用户角色', trigger: 'change' }],
}))
const filtered = computed(() => {
  const keyword = applied.keyword.toLocaleLowerCase()
  return items.value.filter(item => (!keyword || item.username.toLocaleLowerCase().includes(keyword) || item.displayName.toLocaleLowerCase().includes(keyword))
    && (!applied.role || item.role === applied.role) && (typeof applied.enabled !== 'boolean' || item.enabled === applied.enabled))
})
const rows = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))
let sequence = 0
async function load() {
  const current = ++sequence
  loading.value = true
  loadError.value = ''
  try {
    const users = await api.users()
    if (current !== sequence) return
    items.value = users
    if ((page.value - 1) * pageSize >= filtered.value.length) page.value = 1
  } catch (error) { if (current === sequence) { loadError.value = error.message; showError(error) } }
  finally { if (current === sequence) loading.value = false }
}
function search() { Object.assign(applied, { ...filter, keyword: filter.keyword.trim() }); page.value = 1 }
function reset() { Object.assign(filter, { keyword: '', role: '', enabled: '' }); search() }
function edit(item) {
  editingId.value = item?.id || null
  Object.assign(form, item ? { username: item.username, displayName: item.displayName, password: '', role: item.role, enabled: item.enabled } : { username: '', displayName: '', password: '', role: 'USER', enabled: true })
  dialogOpen.value = true
}
async function save() {
  if (saving.value || !await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    const data = { displayName: form.displayName.trim(), role: form.role, enabled: form.enabled }
    if (form.password.trim()) data.password = form.password.trim()
    if (!editingId.value) data.username = form.username.trim()
    if (isSelf.value) {
      auth.user = await api.profile({ displayName: data.displayName, ...(data.password ? { password: data.password } : {}) })
    } else {
      await api.saveUser(editingId.value, data)
    }
    ElMessage.success(editingId.value ? '用户资料已更新' : '用户账号已添加')
    dialogOpen.value = false
    await load()
  } catch (error) { showError(error) }
  finally { saving.value = false }
}
function deleteRestriction(item) {
  if (item.id === auth.user?.id) return '不能删除当前登录账号'
  if (item.enabled && item.role === 'ADMIN' && items.value.filter(user => user.enabled && user.role === 'ADMIN').length <= 1) return '至少保留一个启用的管理员账号'
  return ''
}
async function remove(item) {
  if (busyId.value !== null || deleteRestriction(item)) return
  busyId.value = item.id
  try {
    await ElMessageBox.confirm('确认删除用户「' + item.username + '」？已有借用历史的用户不能删除，请编辑后停用账号。', '删除用户', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' })
    await api.deleteUser(item.id)
    ElMessage.success('用户已删除')
    await load()
  } catch (error) { if (!isCancel(error)) showError(error) }
  finally { busyId.value = null }
}
onMounted(load)
</script>

<template>
  <div class="notice-strip"><el-icon><InfoFilled /></el-icon><span>账号由管理员统一分配。停用后无法登录，已有借用历史的账号请停用以保留记录；至少保留一名启用的管理员。</span></div>
  <section class="panel">
    <div class="panel-header"><div><h2 class="panel-title">系统账号</h2><p class="panel-subtitle">共 {{ items.length }} 位用户</p></div><div class="action-buttons"><el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button><el-button type="primary" :icon="Plus" :disabled="loading || saving || busyId !== null" @click="edit()">添加用户</el-button></div></div>
    <form class="filter-bar" @submit.prevent="search">
      <el-input v-model="filter.keyword" class="keyword-input" clearable :prefix-icon="Search" placeholder="搜索用户名或姓名" maxlength="50" />
      <el-select v-model="filter.role" class="status-select" clearable placeholder="全部角色"><el-option label="管理员" value="ADMIN" /><el-option label="普通用户" value="USER" /></el-select>
      <el-select v-model="filter.enabled" class="status-select" clearable placeholder="全部状态"><el-option label="已启用" :value="true" /><el-option label="已停用" :value="false" /></el-select>
      <el-button type="primary" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button>
    </form>
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="load-error" />
    <el-table v-loading="loading" :data="rows" :empty-text="loadError ? '数据加载失败，请刷新重试' : '暂无符合条件的用户'">
      <el-table-column label="用户" min-width="180"><template #default="{ row }"><div class="user-cell"><span class="avatar"><el-icon><UserFilled /></el-icon></span><div><div class="cell-name">{{ row.displayName }} <el-tag v-if="row.id === auth.user.id" size="small" type="info">当前账号</el-tag></div><div class="cell-secondary">{{ row.username }}</div></div></div></template></el-table-column>
      <el-table-column label="角色" min-width="125"><template #default="{ row }"><el-tag :type="row.role === 'ADMIN' ? 'primary' : 'info'">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag></template></el-table-column>
      <el-table-column label="账号状态" min-width="110"><template #default="{ row }"><span class="account-status" :class="{ disabled: !row.enabled }"><span class="status-dot"></span>{{ row.enabled ? '已启用' : '已停用' }}</span></template></el-table-column>
      <el-table-column label="用户编号" min-width="105"><template #default="{ row }"><span class="cell-secondary">#{{ row.id }}</span></template></el-table-column>
      <el-table-column label="操作" width="160" fixed="right"><template #default="{ row }"><div class="action-buttons"><el-button link type="primary" :disabled="busyId !== null" @click="edit(row)">编辑</el-button><el-tooltip :disabled="!deleteRestriction(row)" :content="deleteRestriction(row)" placement="top"><span><el-button link type="danger" :loading="busyId === row.id" :disabled="!!deleteRestriction(row) || busyId !== null && busyId !== row.id" @click="remove(row)">删除</el-button></span></el-tooltip></div></template></el-table-column>
    </el-table>
    <div class="table-footer"><span>共 {{ filtered.length }} 位用户</span><el-pagination v-model:current-page="page" :page-size="pageSize" :total="filtered.length" layout="prev, pager, next" /></div>
  </section>
  <el-dialog v-model="dialogOpen" :title="editingId ? '编辑用户' : '添加用户'" width="520px" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @opened="formRef?.clearValidate()">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="save">
      <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :disabled="!!editingId" maxlength="30" placeholder="3–30 个英文字母、数字或下划线" autocomplete="off" /><p class="form-hint">用户名创建后不可修改。</p></el-form-item>
      <el-form-item label="姓名" prop="displayName"><el-input v-model="form.displayName" maxlength="50" show-word-limit placeholder="用于界面和借还记录显示" /></el-form-item>
      <el-form-item :label="editingId ? '重设密码' : '初始密码'" prop="password"><el-input v-model="form.password" type="password" show-password maxlength="64" autocomplete="new-password" :placeholder="editingId ? '留空保留原密码' : '请输入 8–64 个字符的初始密码'" /><p class="form-hint">8–64 个字符，UTF-8 长度最多 72 字节。中文通常占 3 字节。</p></el-form-item>
      <div class="form-grid">
        <el-form-item label="角色" prop="role"><el-select v-model="form.role" style="width:100%" :disabled="isSelf || protectedAdmin"><el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" /></el-select></el-form-item>
        <el-form-item label="账号状态"><el-switch v-model="form.enabled" :disabled="isSelf || protectedAdmin" active-text="启用" inactive-text="停用" inline-prompt /></el-form-item>
      </div>
      <p v-if="isSelf" class="form-hint">当前登录账号保持管理员角色与启用状态，姓名和密码可在此更新。</p>
      <p v-else-if="protectedAdmin" class="form-hint">这是唯一启用的管理员，角色与启用状态需要保留。</p>
    </el-form>
    <template #footer><el-button :disabled="saving" @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存用户</el-button></template>
  </el-dialog>
</template>

