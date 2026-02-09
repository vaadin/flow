import { fileURLToPath } from 'url';
import { defineConfig } from 'vite';
import typescript from '@rollup/plugin-typescript';

export default defineConfig({
  build: {
    sourcemap: true,
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
        /^construct-style-sheets-polyfill.*/, // not added by Flow since v25.
        /^lit.*/,
        /^@vaadin.*/,
      ]
    }
  },
  // Preserve import.meta.hot in the built file so it can be replaced in the application instead
  define: { 'import.meta.hot': 'import.meta.hot' }
});
