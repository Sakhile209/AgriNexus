import { defineConfig } from 'vite'

const apiTarget = process.env.AGRINEXUS_API_TARGET || 'http://localhost:8081'

export default defineConfig({
  preview: {
    port: 5174,
    strictPort: true,
    proxy: {
      // Preserve the browser-facing Host so Spring recognizes same-origin
      // requests, including when accessed through a LAN address.
      // Vite's string shorthand enables changeOrigin and triggers CORS rejection.
      '/api': { target: apiTarget, changeOrigin: false },
      '/actuator': { target: apiTarget, changeOrigin: false },
    },
  },
})
