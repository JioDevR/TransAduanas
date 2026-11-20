import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@primevue/themes': fileURLToPath(new URL('./node_modules/@primevue/themes', import.meta.url))
    }
  },
  // Vite organiza los paquetes automáticamente para evitar ciclos entre archivos generados.
  server: {
    port: 5180,
    proxy: {
      '/api': {
        target: 'http://localhost:8085',
        changeOrigin: true
      }
    }
  },
  test: {
    globals: true,
    environment: 'node'
  }
})
