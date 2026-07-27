import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/vendors': 'http://localhost:8080',
      '/purchase-orders': 'http://localhost:8080',
      '/quotations': 'http://localhost:8080',
      '/contracts': 'http://localhost:8080',
      '/ai': 'http://localhost:8080',
      '/mcp': 'http://localhost:8080',
      '/inventory': 'http://localhost:8080'
    }
  }
});
