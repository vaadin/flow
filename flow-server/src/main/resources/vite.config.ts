import { defineConfig } from 'vite';

// https://vitejs.dev/config/
export default defineConfig({
  root: 'frontend',
  build: {
    rollupOptions: {
      external: /^lit-element/,
    },
  },
});
