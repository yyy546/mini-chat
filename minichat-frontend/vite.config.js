import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  define: {
    'global': 'window'
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8090', // 后端地址
        changeOrigin: true, // 解决跨域
        // 关键：删除rewrite规则，保留/api前缀转发到后端
        ws: true // 开启WebSocket代理（必须）
      }
    }
  },
})