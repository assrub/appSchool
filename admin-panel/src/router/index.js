import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import AdminLayout from '../layouts/AdminLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guest: true },
  },
  {
    path: '/',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
      },
      {
        path: 'subjects',
        name: 'subjects',
        component: () => import('../views/SubjectsView.vue'),
      },
      {
        path: 'subjects/:subjectId/topics',
        name: 'topics',
        component: () => import('../views/TopicsView.vue'),
        props: true,
      },
      {
        path: 'topics/:topicId/units',
        name: 'units',
        component: () => import('../views/UnitsView.vue'),
        props: true,
      },
      {
        path: 'units/:unitId/blocks',
        name: 'blocks',
        component: () => import('../views/BlocksView.vue'),
        props: true,
      },
      {
        path: 'blocks/:blockId/items',
        name: 'items',
        component: () => import('../views/ItemsView.vue'),
        props: true,
      },
      {
        path: 'theory/:type/:id',
        name: 'theoryEdit',
        component: () => import('../views/TheoryEditor.vue'),
        props: true,
      },
      {
        path: 'progress',
        name: 'progress',
        component: () => import('../views/ProgressView.vue'),
      },
      {
        path: 'script',
        name: 'script',
        component: () => import('../views/ScriptView.vue'),
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('../views/UsersView.vue'),
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) return next('/login')
  if (to.meta.guest && auth.isAuthenticated) return next('/')
  next()
})

export default router
