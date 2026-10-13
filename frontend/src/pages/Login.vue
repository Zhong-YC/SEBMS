<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Basketball, User, Lock, ArrowRight, CircleCheck } from '@element-plus/icons-vue'
import { signIn, auth } from '../auth'
import { usernameRules, passwordRule } from '../utils'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = { username: usernameRules, password: [passwordRule()] }
async function login() {
  if (loading.value || !await formRef.value.validate().catch(() => false)) return
  loading.value = true
  try {
    await signIn({ username: form.username.trim(), password: form.password.trim() })
    const target = route.query.redirect
    await router.replace(typeof target === 'string' && target.startsWith('/') && !target.startsWith('//') && target !== '/login'
      ? target : auth.user.role === 'ADMIN' ? '/overview' : '/equipment')
    ElMessage.success('欢迎回来，' + auth.user.displayName)
  } catch (error) { ElMessage.error(error.message || '登录失败，请重试') }
  finally { loading.value = false }
}
</script>

<template>
  <main class="login-page">
    <section class="login-story">
      <div class="login-brand"><span class="brand-symbol"><el-icon><Basketball /></el-icon></span><strong>体育器材借用管理系统</strong></div>
      <div class="story-content"><div class="eyebrow">SPORTS EQUIPMENT</div><h1>运动，从这里开始。</h1><p>让器材流转有序，<br />让每一场运动轻松出发。</p><div class="story-lines"><span><el-icon><CircleCheck /></el-icon>器材库存清晰可查</span><span><el-icon><CircleCheck /></el-icon>借用归还及时登记</span><span><el-icon><CircleCheck /></el-icon>历史记录随时追溯</span></div></div>
      <div class="court-art" aria-hidden="true"><div class="court-circle"></div><div class="court-line"></div><div class="court-ball"><Basketball /></div></div>
      <span class="story-footer">规范使用 · 爱护器材 · 按时归还</span>
    </section>
    <section class="login-panel">
      <div class="login-form-wrap">
        <span class="login-kicker">欢迎使用</span><h2>登录你的账号</h2><p class="login-intro">使用管理员分配的账号访问系统。</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="login">
          <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :prefix-icon="User" placeholder="请输入用户名" autocomplete="username" maxlength="30" autofocus /></el-form-item>
          <el-form-item label="密码" prop="password"><el-input v-model="form.password" :prefix-icon="Lock" type="password" show-password placeholder="请输入密码" autocomplete="current-password" maxlength="64" /></el-form-item>
          <el-button class="login-submit" type="primary" size="large" native-type="submit" :loading="loading">登录系统<el-icon v-if="!loading"><ArrowRight /></el-icon></el-button>
        </el-form>
        <div class="demo-accounts"><div>演示账号 <span>仅限本地演示环境</span></div><p><strong>管理员</strong><code>admin</code><code>Admin123!</code></p><p><strong>普通用户</strong><code>student</code><code>Student123!</code></p></div>
        <p class="login-help">如需开通账号或重置密码，请联系管理员。</p>
      </div>
      <div class="login-bottom">体育器材借用管理系统</div>
    </section>
  </main>
</template>

