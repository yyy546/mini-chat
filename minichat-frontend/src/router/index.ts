import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Main from '../views/Main.vue'
import Friend from '../views/Friend.vue'
import GroupApply from '../views/GroupApply.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Main',
    component: Main,
    meta: { requiresAuth: true }
  },
  {
    path: '/group-apply',
    name: 'GroupApply',
    component: GroupApply,
    meta: { requiresAuth: true }
  },
  {
    path: '/friend',
    name: 'Friend',
    component: Friend,
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/')
  } else {
    next()
  }
})

export default router
