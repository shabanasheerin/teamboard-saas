import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The backend runs on 8081 (so it does not clash with other projects on 8080).
const backend = 'http://localhost:8081';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': backend,
      '/ws': { target: backend, ws: true },
    },
  },
});
