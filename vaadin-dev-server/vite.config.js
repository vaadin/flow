import { fileURLToPath } from 'url';
import { defineConfig } from 'vite';
import typescript from '@rollup/plugin-typescript';

const { execSync } = require('child_process');

export default defineConfig({
  build: {
    // Write output to resources to include it in Maven package
    outDir: 'src/main/resources/META-INF/frontend/vaadin-dev-tools',
    // Clear output directory
    emptyOutDir: true,
    rollupOptions: {
      input: {
        devTools: fileURLToPath(new URL('./src/main/frontend/vaadin-dev-tools.ts', import.meta.url))
      },
      output: {
        // Ensure consistent file name for dev tools bundle
        entryFileNames: 'vaadin-dev-tools.js'
      },
      plugins: [typescript({ tsconfig: './tsconfig.json' })],
      // Do not resolve the following imports, these modules
      // will be provided by the application that hosts the dev tools.
      external: [
        /^construct-style-sheets-polyfill.*/,
        /^lit.*/,
        /^@vaadin.*/,
      ]
    }
  }
});
