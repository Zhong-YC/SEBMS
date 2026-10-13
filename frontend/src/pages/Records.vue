<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, CircleCheck, InfoFilled } from '@element-plus/icons-vue'
import { api } from '../api'
import { auth } from '../auth'
import { showError, isCancel, formatTime } from '../utils'

const route = useRoute()
const allRecords = computed(() => route.path === '/manage/records')
const items = ref([])
const loading = ref(false)
const busyId = ref(null)
const loadError = ref('')
const page = ref(1)
const pageSize = 10
const filter = reactive({ keyword: '', status: '', dates: [] })
const personal = computed(() => allRecords.value ? items.value : items.value.filter(item => item.userId === auth.user?.id))
const rows = computed(() => personal.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const borrowingCount = computed(() => personal.value.filter(item => item.status === 'BORROWING').length)
let sequence = 0
async function load(reset = false) {
  const current = ++sequence
  if (reset) page.value = 1
  loading.value = true; loadError.value = ''
  try {
    const records = await api.borrows({ keyword: filter.keyword.trim() || undefined, status: filter.status || undefined, startDate: filter.dates?.[0] || undefined, endDate: filter.dates?.[1] || undefined })
    if (current !== sequence) return
    items.value = records
    if ((page.value - 1) * pageSize >= personal.value.length) page.value = 1
  } catch (error) { if (current === sequence) { loadError.value = error.message; showError(error) } }
  finally { if (current === sequence) loading.value = false }
}
function reset() { Object.assign(filter, { keyword: '', status: '', dates: [] }); load(true) }
async function returnItem(item) {
  if (busyId.value !== null) return
  busyId.value = item.id
  try {
    await ElMessageBox.confirm('确认已归还「' + item.equipmentName + '」1 件？请完成线下交接后再登记归还。', '登记归还', { confirmButtonText: '确认归还', cancelButtonText: '取消', type: 'info' })
    await api.returnBorrow(item.id)
    ElMessage.success('归还成功，库存已更新')
    await load()
  } catch (error) { if (!isCancel(error)) { showError(error); if ([400, 409].includes(error.status)) await load() } }
  finally { busyId.value = null }
}
onMounted(() => load())
</script>

<template>
  <div v-if="!allRecords" class="notice-strip"><el-icon><InfoFilled /></el-icon><span>完成线下归还后，请点击对应记录的“登记归还”。每条记录对应 <strong>1 件</strong> 器材。</span></div>
  <section class="panel">
    <div class="panel-header"><div><h2 class="panel-title">{{ allRecords ? '借还记录' : '我的借还记录' }}</h2><p class="panel-subtitle">当前查询共 {{ personal.length }} 条记录，其中 {{ borrowingCount }} 条借用中</p></div><el-button :icon="Refresh" :loading="loading" @click="load()">刷新</el-button></div>
    <form class="filter-bar" @submit.prevent="load(true)">
      <el-input v-model="filter.keyword" class="keyword-input" clearable :prefix-icon="Search" :placeholder="allRecords ? '搜索器材名称或用户名' : '搜索器材名称'" maxlength="100" />
      <el-select v-model="filter.status" class="status-select" clearable placeholder="全部状态"><el-option label="借用中" value="BORROWING" /><el-option label="已归还" value="RETURNED" /></el-select>
      <el-date-picker v-model="filter.dates" type="daterange" range-separator="至" start-placeholder="借用开始日期" end-placeholder="借用结束日期" value-format="YYYY-MM-DD" format="YYYY-MM-DD" style="width:280px;flex-grow:0" />
      <el-button type="primary" native-type="submit" :disabled="loading">查询</el-button><el-button :disabled="loading" @click="reset">重置</el-button>
    </form>
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="load-error" />
    <el-table v-loading="loading" :data="rows" :empty-text="loadError ? '数据加载失败，请点击刷新或查询重试' : '暂无符合条件的借还记录'">
      <el-table-column label="器材名称" min-width="160"><template #default="{ row }"><div class="cell-name">{{ row.equipmentName }}</div><div class="cell-secondary">记录编号 #{{ row.id }} · 1 件</div></template></el-table-column>
      <el-table-column v-if="allRecords" label="借用人" min-width="125"><template #default="{ row }"><div>{{ row.displayName }}</div><div class="cell-secondary">{{ row.username }}</div></template></el-table-column>
      <el-table-column label="借用时间" min-width="175"><template #default="{ row }">{{ formatTime(row.borrowTime) }}</template></el-table-column>
      <el-table-column label="归还时间" min-width="175"><template #default="{ row }">{{ formatTime(row.returnTime) }}</template></el-table-column>
      <el-table-column label="状态" width="105"><template #default="{ row }"><el-tag :type="row.status === 'BORROWING' ? 'warning' : 'success'">{{ row.status === 'BORROWING' ? '借用中' : '已归还' }}</el-tag></template></el-table-column>
      <el-table-column v-if="!allRecords" label="操作" width="130" fixed="right"><template #default="{ row }"><el-button v-if="row.status === 'BORROWING'" type="primary" plain size="small" :loading="busyId === row.id" :disabled="busyId !== null && busyId !== row.id" @click="returnItem(row)">登记归还</el-button><span v-else class="cell-secondary"><el-icon><CircleCheck /></el-icon> 已完成</span></template></el-table-column>
    </el-table>
    <div class="table-footer"><span>共 {{ personal.length }} 条记录</span><el-pagination v-model:current-page="page" :page-size="pageSize" :total="personal.length" layout="prev, pager, next" /></div>
  </section>
</template>
