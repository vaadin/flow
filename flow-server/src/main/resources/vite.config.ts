import { defineConfig } from 'vite';
import path from 'path';

const frontendFolder = path.resolve(__dirname, 'frontend');
const themeFolder = path.resolve(frontendFolder, 'themes');

// https://vitejs.dev/config/
export default defineConfig({
  root: 'frontend',
  base: '/VAADIN/',
  resolve: {
    alias: {
      themes: themeFolder,
      Frontend: frontendFolder
    },
  },
  build: {
    rollupOptions: {
      external: /^lit-element/,
    },
  },
});
