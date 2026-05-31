import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import SubjectsView from '../views/SubjectsView.vue'
import TopicsView from '../views/TopicsView.vue'
import UnitsView from '../views/UnitsView.vue'
import BlocksView from '../views/BlocksView.vue'
import ItemsView from '../views/ItemsView.vue'

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
      { path: '', name: 'dashboard', component: DashboardView },
      { path: 'subjects', name: 'subjects', component: SubjectsView },
      { path: 'subjects/:subjectId/topics', name: 'topics', component: TopicsView, props: true },
      { path: 'topics/:topicId/units', name: 'units', component: UnitsView, props: true },
      { path: 'units/:unitId/blocks', name: 'blocks', component: BlocksView, props: true },
      { path: 'blocks/:blockId/items', name: 'items', component: ItemsView, props: true },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    next('/login')
  } else if (to.meta.guest && auth.isAuthenticated) {
    next('/')
  } else {
    next()
  }
})

export default router
