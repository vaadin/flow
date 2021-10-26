import { defineConfig } from 'vite';
import path from 'path';

import { processThemeResources } from '@vaadin/application-theme-plugin/theme-handle.js';
import settings from './target/vaadin-dev-server-settings.json';

const frontendFolder = path.resolve(__dirname, settings.frontendFolder);
const themeFolder = path.resolve(frontendFolder, settings.themeFolder);
const buildFolder = path.resolve(__dirname, settings.frontendBundleOutput);

const projectStaticAssetsFolders = [
  path.resolve(__dirname, 'src', 'main', 'resources', 'META-INF', 'resources'),
  path.resolve(__dirname, 'src', 'main', 'resources', 'static'),
  frontendFolder
];

// Folders in the project which can contain application themes
const themeProjectFolders = projectStaticAssetsFolders.map((folder) =>
    path.resolve(folder, settings.themeFolder)
);

const themeOptions = {
  devMode: false,
  // The following matches folder 'target/flow-frontend/themes/'
  // (not 'frontend/themes') for theme in JAR that is copied there
  themeResourceFolder: path.resolve(__dirname, settings.themeResourceFolder),
  themeProjectFolders: themeProjectFolders,
  projectStaticAssetsOutputFolder: path.resolve(__dirname, settings.staticOutput),
  frontendGeneratedFolder: path.resolve(frontendFolder, settings.generatedFolder)
};

// Block debug and trace logs.
console.trace = () => {};
console.debug =() => {};

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => ({
  root: 'frontend',
  base: mode === 'production' ? '' : '/VAADIN/',
  resolve: {
    alias: {
      themes: themeFolder,
      Frontend: frontendFolder
    },
  },
  build: {
    outDir: buildFolder,
    assetsDir: 'VAADIN/build',
    rollupOptions: {
      input: {
        main: path.resolve(frontendFolder, 'index.html'),
        generated: path.resolve(frontendFolder, 'generated/vaadin.ts')
      },
      output: {
        // Produce only one chunk that gets imported into index.html
        manualChunks: () => 'everything.js'
      },
    },
  },
  plugins: [
    {
      name: 'custom-theme',
      config() {
        processThemeResources(themeOptions, console);
      }
    }
  ]
}));
