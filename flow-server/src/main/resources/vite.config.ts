import { defineConfig } from 'vite';
import path from 'path';

import { processThemeResources } from '@vaadin/application-theme-plugin/theme-handle.js';
import flowSettings from './flow-settings.json';

const frontendFolder = path.resolve(__dirname, flowSettings.frontendFolder);
const themeFolder = path.resolve(frontendFolder, flowSettings.themeFolder);

const projectStaticAssetsFolders = [
  path.resolve(__dirname, 'src', 'main', 'resources', 'META-INF', 'resources'),
  path.resolve(__dirname, 'src', 'main', 'resources', 'static'),
  frontendFolder
];

// Folders in the project which can contain application themes
const themeProjectFolders = projectStaticAssetsFolders.map((folder) =>
    path.resolve(folder, flowSettings.themeFolder)
);

const themeOptions = {
  devMode: false,
  // The following matches folder 'target/flow-frontend/themes/'
  // (not 'frontend/themes') for theme in JAR that is copied there
  themeResourceFolder: path.resolve(__dirname, flowSettings.themeResourceFolder),
  themeProjectFolders: themeProjectFolders,
  projectStaticAssetsOutputFolder: path.resolve(__dirname, flowSettings.staticOutput),
  frontendGeneratedFolder: path.resolve(frontendFolder, flowSettings.generatedFolder)
};

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
  plugins: [
    {
      name: 'custom-theme',
      config() {
        processThemeResources(themeOptions, console);
      }
    }
  ]
});
