import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The dev server proxies /api to the Spring Boot backend so the browser never needs CORS.
// The backend listens on 8090 (server.port in application.properties); KALA_API_URL overrides it.
export default defineConfig({
  plugins: [react()],
  server: { proxy: { '/api': process.env.KALA_API_URL ?? 'http://localhost:8090' } },
})
