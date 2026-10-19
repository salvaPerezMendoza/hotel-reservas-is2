import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

// En desarrollo, las llamadas a /api se reenvían al backend de Spring Boot.
export default defineConfig({
  plugins: [react()],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  server: {
    port: 5173,
    host: true,
    proxy: {
      // Se quita el Origin: para el backend es una llamada del mismo sitio, igual que detrás de nginx.
      '/api': {
        target: process.env.VITE_BACKEND ?? 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => proxy.on('proxyReq', (req) => req.removeHeader('origin')),
      },
    },
  },
})
