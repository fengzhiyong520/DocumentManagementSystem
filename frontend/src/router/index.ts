import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/UserList.vue')
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/user/RoleList.vue')
      },
      {
        path: 'permission',
        name: 'Permission',
        component: () => import('@/views/user/PermissionList.vue')
      },
      {
        path: 'menu',
        name: 'Menu',
        component: () => import('@/views/user/MenuList.vue')
      },
      {
        path: 'document',
        name: 'Document',
        component: () => import('@/views/document/DocumentList.vue')
      },
      {
        path: 'borrow',
        name: 'Borrow',
        component: () => import('@/views/borrow/BorrowList.vue')
      },
      {
        path: 'borrow-history',
        name: 'BorrowHistory',
        component: () => import('@/views/borrow/BorrowHistoryList.vue')
      },
      {
        path: 'hall',
        name: 'Hall',
        component: () => import('@/views/hall/Hall.vue')
      },
      {
        path: 'process-definition',
        name: 'ProcessDefinition',
        component: () => import('@/views/process/ProcessDefinitionList.vue')
      },
      {
        path: 'friend',
        name: 'Friend',
        component: () => import('@/views/user/FriendList.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router

