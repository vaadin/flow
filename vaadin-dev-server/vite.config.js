import { fileURLToPath } from 'url';
import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    // Write output to resources to include it in Maven package
    outDir: 'src/main/resources/META-INF/frontend/vaadin-dev-tools',
    // Clear output directory
    emptyOutDir: true,
    rollupOptions: {
      input: {
        devTools: fileURLToPath(new URL('./frontend/vaadin-dev-tools.ts', import.meta.url))
      },
      output: {
        // Ensure consistent file name for dev tools bundle
        entryFileNames: 'vaadin-dev-tools.js'
      },
      // Do not resolve imports for Lit and Vaadin components, these modules
      // will be provided by the application that hosts the dev tools.
      external: [
        /^lit.*/,
        /^@vaadin.*/,
      ]
    }
  }
});
