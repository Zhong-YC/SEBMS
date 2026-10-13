import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1', port: 5173, strictPort: true,
    proxy: { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: false } },
  },
  build: {
    chunkSizeWarningLimit: 1100,
    rollupOptions: { output: { manualChunks: { vendor: ['vue', 'vue-router', 'axios'], 'element-plus': ['element-plus'] } } },
  },
})
