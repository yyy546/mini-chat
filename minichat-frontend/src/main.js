import { createApp } from 'vue'
import { createPinia } from 'pinia' // 保留原有Pinia创建依赖
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router from './router'
// 引入图标库
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 新增：导入用户仓库（用于初始化用户信息）
import { useUserStore } from './store/user'

// 1. 创建核心实例（保留原有逻辑）
const app = createApp(App)
const pinia = createPinia() // 原有：在main.js内创建Pinia实例

// 2. 安装插件（保留原有所有功能）
app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 3. 全局注册所有图标（保留原有逻辑）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// 4. 恢复用户信息（若存在Token）
const userStore = useUserStore()
userStore.initUserInfo().catch(() => {})

// 5. 挂载应用
app.mount('#app')