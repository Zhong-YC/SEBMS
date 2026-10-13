<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Basketball, DataBoard, Box, Tickets, Collection, User, ArrowDown, Fold, Expand, UserFilled, SwitchButton } from '@element-plus/icons-vue'
import { auth } from './auth'
import { api } from './api'
import { showError, trimmedRequired, textLength, passwordRule } from './utils'

const route = useRoute()
const router = useRouter()
const mobileOpen = ref(false)
const loggingOut = ref(false)
const profileOpen = ref(false)
const profileSaving = ref(false)
const profileRef = ref()
const profile = reactive({ displayName: '', password: '' })
const profileRules = {
  displayName: [trimmedRequired('姓名'), textLength(1, 50, '姓名')],
  password: [passwordRule(true)],
}
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const nav = computed(() => isAdmin.value ? [{ path: '/overview', label: '工作台', icon: DataBoard }] : [
  { path: '/equipment', label: '器材借用', icon: Basketball },
  { path: '/records', label: '我的借还', icon: Tickets },
])
const management = [
  { path: '/manage/equipment', label: '器材管理', icon: Box },
  { path: '/manage/records', label: '全部借还记录', icon: Tickets },
  { path: '/manage/categories', label: '分类管理', icon: Collection },
  { path: '/manage/users', label: '用户管理', icon: User },
]
const today = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
function go(path) { mobileOpen.value = false; router.push(path) }
function sessionExpired() {
  if (!auth.user) return
  auth.user = null
  profileOpen.value = false
  ElMessage.warning('登录已失效，请重新登录')
  router.replace({ path: '/login', query: { redirect: route.fullPath } })
}
onMounted(() => window.addEventListener('session-expired', sessionExpired))
onUnmounted(() => window.removeEventListener('session-expired', sessionExpired))
async function logout() {
  if (loggingOut.value) return
  loggingOut.value = true
  try { await api.logout(); auth.user = null; await router.replace('/login') }
  catch (error) { showError(error) }
  finally { loggingOut.value = false }
}
function accountAction(command) {
  if (command === 'logout') return logout()
  profile.displayName = auth.user.displayName
  profile.password = ''
  profileOpen.value = true
}
async function saveProfile() {
  if (profileSaving.value || !await profileRef.value.validate().catch(() => false)) return
  profileSaving.value = true
  try {
    const data = { displayName: profile.displayName.trim() }
    if (profile.password.trim()) data.password = profile.password.trim()
    auth.user = await api.profile(data)
    profileOpen.value = false
    ElMessage.success('个人资料已更新')
  } catch (error) { showError(error) }
  finally { profileSaving.value = false }
}
</script>

<template>
  <div v-if="!auth.ready" class="startup"><el-icon class="is-loading"><Basketball /></el-icon><span>正在加载系统…</span></div>
  <router-view v-else-if="route.meta.public" />
  <div v-else-if="auth.user" class="app-layout">
    <div v-if="mobileOpen" class="nav-backdrop" @click="mobileOpen = false"></div>
    <aside class="sidebar" :class="{ 'is-open': mobileOpen }">
      <a class="brand" href="/" @click.prevent="go(isAdmin ? '/overview' : '/equipment')">
        <span class="brand-symbol"><el-icon><Basketball /></el-icon></span>
        <span><strong>体育器材</strong><small>借用管理系统</small></span>
      </a>
      <div class="nav-section-label">日常使用</div>
      <nav aria-label="日常使用">
        <button v-for="item in nav" :key="item.path" class="nav-item" :class="{ active: route.path === item.path }" @click="go(item.path)"><el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span></button>
      </nav>
      <template v-if="isAdmin">
        <div class="nav-section-label management-label">系统管理</div>
        <nav aria-label="系统管理">
          <button v-for="item in management" :key="item.path" class="nav-item" :class="{ active: route.path === item.path }" @click="go(item.path)"><el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span></button>
        </nav>
      </template>
      <div class="sidebar-note"><span class="status-dot"></span>规范登记 · 有借有还<small>每次借用登记 1 件器材</small></div>
    </aside>
    <div class="main-layout">
      <header class="topbar">
        <div class="topbar-left">
          <el-button class="mobile-toggle" text circle aria-label="展开导航" @click="mobileOpen = !mobileOpen"><el-icon><component :is="mobileOpen ? Fold : Expand" /></el-icon></el-button>
          <span class="breadcrumb">体育器材管理 <span>/</span> <strong>{{ route.meta.title }}</strong></span>
        </div>
        <el-dropdown trigger="click" @command="accountAction">
          <button class="account-button">
            <span class="avatar">{{ auth.user.displayName?.slice(0, 1) || '用' }}</span>
            <span class="account-name">{{ auth.user.displayName }}<small>{{ isAdmin ? '管理员' : '普通用户' }}</small></span>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown><el-dropdown-menu><el-dropdown-item command="profile" :icon="UserFilled">个人资料</el-dropdown-item><el-dropdown-item command="logout" :icon="SwitchButton" divided :disabled="loggingOut">{{ loggingOut ? '正在退出…' : '退出登录' }}</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </header>
      <main class="page-content">
        <div class="page-heading"><div><h1>{{ route.meta.title }}</h1><p>{{ route.meta.subtitle }}</p></div><span class="today-label">{{ today }}</span></div>
        <router-view :key="route.path" />
      </main>
      <footer class="app-footer">体育器材借用管理系统 <span>·</span> 让每一件器材都用得其所</footer>
    </div>
    <el-dialog v-model="profileOpen" title="个人资料" width="480px" :close-on-click-modal="false" :close-on-press-escape="!profileSaving" :show-close="!profileSaving" @opened="profileRef?.clearValidate()">
      <el-form ref="profileRef" :model="profile" :rules="profileRules" label-position="top" @submit.prevent="saveProfile">
        <el-form-item label="用户名"><el-input :model-value="auth.user.username" disabled /></el-form-item>
        <el-form-item label="角色"><el-input :model-value="isAdmin ? '管理员' : '普通用户'" disabled /></el-form-item>
        <el-form-item label="姓名" prop="displayName"><el-input v-model="profile.displayName" maxlength="50" show-word-limit /></el-form-item>
        <el-form-item label="新密码" prop="password"><el-input v-model="profile.password" type="password" show-password maxlength="64" placeholder="留空保留原密码，设置时需 8–64 个字符" /></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="profileSaving" @click="profileOpen = false">取消</el-button><el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button></template>
    </el-dialog>
  </div>
</template>


