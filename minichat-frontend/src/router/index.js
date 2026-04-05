import { createRouter, createWebHistory } from 'vue-router'

// 静态导入组件（避免动态导入问题）
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Main from '../views/Main.vue'
import Friend from '../views/Friend.vue'
import GroupApply from '../views/GroupApply.vue'

const routes = [
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

// 简单的路由守卫
router.beforeEach((to, from, next) => {
    // const token = localStorage.getItem('token')
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
