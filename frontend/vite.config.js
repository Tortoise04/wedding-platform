import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 前端开发服务器配置:5173 端口,/api 开头的请求代理到后端 18888(避免跨域)。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    watch: {
      // 编辑工具的原子写会在源码目录留下 .tmpdir / .tmp 临时文件，
      // Windows 下 Vite 的文件监听偶发 EBUSY 崩溃，这里忽略掉。
      // 浏览器自动化(design_audit/截图)会在项目内留下 .tmp-audit 配置目录、构建会留下 .tmpbuild，
      // 这些目录里的 Chrome 缓存文件在 Windows 上被占用时同样会触发 EBUSY，一并忽略。
      ignored: ['**/*.tmpdir/**', '**/*.tmp', '**/.*.tmpdir/**', '**/.tmp-audit/**', '**/.tmpbuild/**', '**/.tmp-build/**', '**/.tmpwork/**', '**/dist/**']
    },
    proxy: {
      '/api': {
        target: 'http://localhost:18888',
        changeOrigin: true
      },
      // 上传的图片存在后端磁盘,不代理的话 5173 下 /uploads 会 404
      '/uploads': {
        target: 'http://localhost:18888',
        changeOrigin: true
      }
    }
  }
})
