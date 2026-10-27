import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken, getRole } from '@/store/auth'

// 路由按三端 + 门户划分,meta 里声明需要的角色:空 = 公开 / 登录 = 任意已登录 / 具体角色数组。
const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },

  // 门户(游客可看)
  {
    path: '/',
    component: () => import('@/layouts/PortalLayout.vue'),
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: 'home', component: () => import('@/views/portal/Home.vue') },
      { path: 'services', name: 'services', component: () => import('@/views/portal/ServiceList.vue') },
      { path: 'services/:id', name: 'serviceDetail', component: () => import('@/views/portal/ServiceDetail.vue') },
      { path: 'merchants', name: 'merchants', component: () => import('@/views/portal/Merchants.vue') },
      { path: 'content', name: 'content', component: () => import('@/views/portal/Content.vue') }
    ]
  },

  // 用户端(登录即可)
  {
    path: '/user',
    component: () => import('@/layouts/UserLayout.vue'),
    meta: { auth: true, roles: ['USER'] },
    children: [
      { path: '', redirect: '/user/orders' },
      { path: 'profile', name: 'userProfile', component: () => import('@/views/user/Profile.vue') },
      { path: 'orders', name: 'userOrders', component: () => import('@/views/user/Orders.vue') },
      { path: 'favorites', name: 'userFavorites', component: () => import('@/views/user/Favorites.vue') },
      { path: 'browse', name: 'userBrowse', component: () => import('@/views/user/Browse.vue') },
      { path: 'consultations', name: 'userConsultations', component: () => import('@/views/user/Consultations.vue') },
      { path: 'disputes', name: 'userDisputes', component: () => import('@/views/user/Disputes.vue') },
      { path: 'disputes/:id', name: 'userDisputeDetail', component: () => import('@/views/user/DisputeDetail.vue') },
      // 商家入驻申请:申请人此时角色仍是 USER,只能挂在用户端下,不能进 /merchant
      { path: 'merchant-apply', name: 'merchantApply', component: () => import('@/views/merchant/Apply.vue') }
    ]
  },

  // 商家端(商家或管理员)
  {
    path: '/merchant',
    component: () => import('@/layouts/MerchantLayout.vue'),
    meta: { roles: ['MERCHANT', 'ADMIN'] },
    children: [
      { path: '', redirect: '/merchant/dashboard' },
      { path: 'dashboard', name: 'merchantDashboard', component: () => import('@/views/merchant/Dashboard.vue') },
      { path: 'services', name: 'merchantServices', component: () => import('@/views/merchant/Services.vue') },
      { path: 'orders', name: 'merchantOrders', component: () => import('@/views/merchant/Orders.vue') },
      { path: 'evaluations', name: 'merchantEvaluations', component: () => import('@/views/merchant/Evaluations.vue') },
      { path: 'consultations', name: 'merchantConsultations', component: () => import('@/views/merchant/Consultations.vue') },
      { path: 'profile', name: 'merchantProfile', component: () => import('@/views/merchant/Profile.vue') },
      { path: 'disputes', name: 'merchantDisputes', component: () => import('@/views/merchant/Disputes.vue') },
      { path: 'disputes/:id', name: 'merchantDisputeDetail', component: () => import('@/views/merchant/DisputeDetail.vue') }
    ]
  },

  // 管理端(仅管理员)
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { roles: ['ADMIN'] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'adminDashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'merchants', name: 'adminMerchants', component: () => import('@/views/admin/Merchants.vue') },
      { path: 'services', name: 'adminServices', component: () => import('@/views/admin/Services.vue') },
      { path: 'evaluations', name: 'adminEvaluations', component: () => import('@/views/admin/Evaluations.vue') },
      { path: 'penalties', name: 'adminPenalties', component: () => import('@/views/admin/Penalties.vue') },
      { path: 'orders', name: 'adminOrders', component: () => import('@/views/admin/Orders.vue') },
      { path: 'disputes', name: 'adminDisputes', component: () => import('@/views/admin/Disputes.vue') },
      { path: 'content', name: 'adminContent', component: () => import('@/views/admin/Content.vue') }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫:未登录拦到登录页,角色不符拦回各自首页。
router.beforeEach((to) => {
  const token = getToken()
  const role = getRole()
  const needAuth = to.matched.some(r => r.meta.auth)
  const needRoles = to.matched.find(r => r.meta.roles)?.meta.roles

  if (needAuth && !token) return '/login'
  if (needRoles && !needRoles.includes(role)) {
    return role === 'ADMIN' ? '/admin' : role === 'MERCHANT' ? '/merchant' : '/home'
  }
  return true
})

export default router
