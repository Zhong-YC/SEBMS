import { createRouter, createWebHistory } from 'vue-router'
import { auth, bootstrapAuth } from './auth'
import Login from './pages/Login.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login, meta: { public: true, title: '登录' } },
    { path: '/', redirect: '/overview' },
    { path: '/overview', component: () => import('./pages/Overview.vue'), meta: { admin: true, title: '工作台', subtitle: '器材与借还情况，一目了然' } },
    { path: '/equipment', component: () => import('./pages/Equipment.vue'), meta: { title: '器材借用', subtitle: '找到所需器材，完成线下交接后登记借用' } },
    { path: '/records', component: () => import('./pages/Records.vue'), meta: { title: '我的借还', subtitle: '查看借用历史，及时登记器材归还' } },
    { path: '/manage/equipment', component: () => import('./pages/Equipment.vue'), meta: { admin: true, title: '器材管理', subtitle: '维护器材资料、数量和可借状态' } },
    { path: '/manage/records', component: () => import('./pages/Records.vue'), meta: { admin: true, title: '全部借还记录', subtitle: '按器材、借用人和日期查询借还记录' } },
    { path: '/manage/categories', component: () => import('./pages/Categories.vue'), meta: { admin: true, title: '分类管理', subtitle: '为器材建立清晰、易查找的分类' } },
    { path: '/manage/users', component: () => import('./pages/Users.vue'), meta: { admin: true, title: '用户管理', subtitle: '维护系统账号、角色和使用状态' } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
router.beforeEach(async to => {
  try { await bootstrapAuth() } catch { /* 登录页可再次发起请求；页面操作展示连接错误。 */ }
  if (!to.meta.public && !auth.user) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.path === '/login' && auth.user) return auth.user.role === 'ADMIN' ? '/overview' : '/equipment'
  if (to.meta.admin && auth.user?.role !== 'ADMIN') return '/equipment'
  document.title = (to.meta.title || '首页') + ' · 体育器材借用管理系统'
})

