import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { noAuth: true }
    },
    // 住客端
    {
      path: '/guest',
      redirect: '/guest/home',
      meta: { roles: ['ROLE_MEMBER', 'ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/guest/home',
      name: 'GuestHome',
      component: () => import('@/views/guest/Home.vue'),
      meta: { roles: ['ROLE_MEMBER', 'ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/guest/order',
      name: 'GuestOrder',
      component: () => import('@/views/guest/Order.vue'),
      meta: { roles: ['ROLE_MEMBER', 'ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/guest/payment/:orderId',
      name: 'GuestPayment',
      component: () => import('@/views/guest/Payment.vue'),
      meta: { roles: ['ROLE_MEMBER', 'ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/guest/checkin/:orderId',
      name: 'GuestCheckIn',
      component: () => import('@/views/guest/CheckIn.vue'),
      meta: { roles: ['ROLE_MEMBER', 'ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/guest/card/:checkinId',
      name: 'GuestRoomCard',
      component: () => import('@/views/guest/RoomCard.vue'),
      meta: { roles: ['ROLE_MEMBER', 'ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/guest/orders',
      name: 'GuestMyOrders',
      component: () => import('@/views/guest/MyOrders.vue'),
      meta: { roles: ['ROLE_MEMBER', 'ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    // 管理端
    {
      path: '/admin',
      redirect: '/admin/dashboard',
      meta: { roles: ['ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/admin/dashboard',
      name: 'AdminDashboard',
      component: () => import('@/views/admin/Dashboard.vue'),
      meta: { roles: ['ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/admin/orders',
      name: 'AdminOrders',
      component: () => import('@/views/admin/OrderManage.vue'),
      meta: { roles: ['ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/admin/rooms',
      name: 'AdminRooms',
      component: () => import('@/views/admin/RoomManage.vue'),
      meta: { roles: ['ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/admin/members',
      name: 'AdminMembers',
      component: () => import('@/views/admin/MemberManage.vue'),
      meta: { roles: ['ROLE_ADMIN', 'ROLE_STAFF'] }
    },
    {
      path: '/admin/checkins',
      name: 'AdminCheckins',
      component: () => import('@/views/admin/CheckInManage.vue'),
      meta: { roles: ['ROLE_ADMIN', 'ROLE_STAFF'] }
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()

  // 恢复登录状态
  if (!auth.isLoggedIn) {
    auth.loadFromStorage()
  }

  // 白名单页面
  if (to.meta.noAuth) {
    if (auth.isLoggedIn && to.path === '/login') {
      return next(auth.isAdmin || auth.isStaff ? '/admin/dashboard' : '/guest/home')
    }
    return next()
  }

  // 未登录
  if (!auth.isLoggedIn) {
    return next('/login')
  }

  // 角色检查
  const roles = to.meta.roles as string[] | undefined
  if (roles && !roles.includes(auth.userInfo!.role)) {
    return next(auth.isAdmin || auth.isStaff ? '/admin/dashboard' : '/guest/home')
  }

  next()
})

export default router