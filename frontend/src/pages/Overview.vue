<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Basketball, Box, Collection, User, Refresh, Right, Tickets, CircleCheck, TrendCharts } from '@element-plus/icons-vue'
import { api } from '../api'
import { auth } from '../auth'
import { showError, formatTime } from '../utils'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const stats = ref(null)
const equipment = ref([])
const records = ref([])
const metrics = computed(() => [
  { label: '器材总件数', value: stats.value?.totalQuantity, unit: '件', icon: Box, foot: '包含启用与停用的器材' },
  { label: '器材品种数', value: stats.value?.equipmentTypes, unit: '种', icon: Collection, foot: '已建档的器材资料' },
  { label: '库存余量', value: stats.value?.availableQuantity, unit: '件', icon: CircleCheck, foot: '含停用器材，实际借用需启用' },
  { label: '借出数量', value: stats.value?.borrowedQuantity, unit: '件', icon: Basketball, foot: '尚未登记归还的器材' },
  { label: '系统用户', value: stats.value?.userCount, unit: '人', icon: User, foot: '已建立的系统账号' },
])
const recentRecords = computed(() => [...records.value].sort((a, b) => (b.borrowTime || '').localeCompare(a.borrowTime || '') || b.id - a.id).slice(0, 6))
const inventory = computed(() => {
  const groups = new Map()
  for (const item of equipment.value) {
    const group = groups.get(item.categoryId) || { id: item.categoryId, name: item.categoryName, total: 0, available: 0 }
    group.total += item.totalQuantity
    group.available += item.availableQuantity
    groups.set(item.categoryId, group)
  }
  return [...groups.values()].sort((a, b) => b.total - a.total).slice(0, 5)
})
const enabledTypes = computed(() => equipment.value.filter(item => item.enabled).length)
const outOfStock = computed(() => equipment.value.filter(item => item.enabled && item.availableQuantity === 0).length)
const availability = computed(() => stats.value?.totalQuantity ? Math.round(stats.value.availableQuantity / stats.value.totalQuantity * 100) : 0)
let sequence = 0
async function load() {
  const current = ++sequence
  loading.value = true
  loadError.value = ''
  try {
    const [summary, items, history] = await Promise.all([api.stats(), api.equipment(), api.borrows()])
    if (current !== sequence) return
    stats.value = summary
    equipment.value = items
    records.value = history
  } catch (error) {
    if (current === sequence) { loadError.value = error.message; showError(error) }
  } finally { if (current === sequence) loading.value = false }
}
onMounted(load)
</script>

<template>
  <div class="welcome-strip">
    <div><h2>{{ auth.user.displayName }}，欢迎回来</h2><p>从库存到借还记录，及时掌握器材使用情况。</p></div>
    <el-icon aria-hidden="true"><Basketball /></el-icon>
  </div>
  <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="load-error" />
  <div v-loading="loading" class="metrics-grid">
    <div v-for="metric in metrics" :key="metric.label" class="metric-card">
      <div class="metric-top"><span>{{ metric.label }}</span><span class="metric-icon"><el-icon><component :is="metric.icon" /></el-icon></span></div>
      <div class="metric-number">{{ metric.value ?? '—' }}<small>{{ metric.unit }}</small></div>
      <div class="metric-foot">{{ metric.foot }}</div>
    </div>
  </div>
  <div class="dashboard-grid">
    <section class="panel">
      <div class="panel-header"><div><h2 class="panel-title">最近借还</h2><p class="panel-subtitle">按借用时间展示最近 6 条记录</p></div><el-button link type="primary" :icon="Right" @click="router.push('/manage/records')">查看全部</el-button></div>
      <el-table v-loading="loading" :data="recentRecords" :empty-text="loadError ? '数据加载失败，请刷新重试' : '暂无借还记录'">
        <el-table-column label="器材 / 借用人" min-width="160"><template #default="{ row }"><div class="cell-name">{{ row.equipmentName }}</div><div class="cell-secondary">{{ row.displayName }} · {{ row.username }}</div></template></el-table-column>
        <el-table-column label="借用时间" min-width="172"><template #default="{ row }">{{ formatTime(row.borrowTime) }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'BORROWING' ? 'warning' : 'success'">{{ row.status === 'BORROWING' ? '借用中' : '已归还' }}</el-tag></template></el-table-column>
      </el-table>
      <div class="table-footer"><span>共 {{ records.length }} 条借还记录</span><el-button text :icon="Refresh" :loading="loading" @click="load">刷新数据</el-button></div>
    </section>
    <section class="panel">
      <div class="panel-header"><div><h2 class="panel-title">分类库存</h2><p class="panel-subtitle">按总数量展示前 5 个分类</p></div><el-icon class="panel-icon"><TrendCharts /></el-icon></div>
      <div v-loading="loading" class="inventory-list">
        <div v-for="item in inventory" :key="item.id" class="inventory-item">
          <div class="inventory-item-top"><span>{{ item.name }}</span><small>库存 {{ item.available }} / {{ item.total }} 件</small></div>
          <el-progress :percentage="item.total ? Math.round(item.available / item.total * 100) : 0" :stroke-width="7" :show-text="false" color="#64a382" />
        </div>
        <el-empty v-if="!inventory.length" :image-size="70" :description="loadError ? '数据加载失败' : '暂无器材资料'" />
      </div>
      <div class="overview-summary">
        <div class="summary-item"><span>库存占比</span><strong>{{ stats ? availability + '%' : '—' }}</strong></div>
        <div class="summary-item"><span>启用品种</span><strong>{{ stats ? enabledTypes : '—' }}</strong></div>
        <div class="summary-item"><span>暂无库存</span><strong>{{ stats ? outOfStock : '—' }}</strong></div>
        <div class="summary-item"><span>停用品种</span><strong>{{ stats ? equipment.length - enabledTypes : '—' }}</strong></div>
      </div>
    </section>
  </div>
  <div class="quick-links">
    <button @click="router.push('/manage/equipment')"><el-icon><Box /></el-icon><span>维护器材资料</span><el-icon><Right /></el-icon></button>
    <button @click="router.push('/manage/users')"><el-icon><User /></el-icon><span>管理系统账号</span><el-icon><Right /></el-icon></button>
    <button @click="router.push('/manage/records')"><el-icon><Tickets /></el-icon><span>查询全部借还</span><el-icon><Right /></el-icon></button>
  </div>
</template>

