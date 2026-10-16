import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Listen on all interfaces so a phone on the same network can reach the dev server
    // (needed when using a tunnel such as Cloudflare Tunnel - see README).
    host: true,
    // Trust the tunnel hostname (Cloudflare quick tunnels use *.trycloudflare.com)
    // so Vite does not reject proxied requests with "Blocked request. This host is not allowed."
    allowedHosts: ['.trycloudflare.com'],
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws': { target: 'ws://localhost:8080', ws: true },
    },
  },
})
