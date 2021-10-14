import { defineConfig } from 'vite';
import path from 'path';

import { processThemeResources, extractThemeName } from '@vaadin/application-theme-plugin/theme-handle.js';
import flowSettings from './target/flow-settings.json'; // build directory should be set from build system configuration

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
      Frontend: frontendFolder,
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
      },
      load() {
        const themeName = extractThemeName(themeOptions.frontendGeneratedFolder);
        if(themeName) {
          this.addWatchFile(path.resolve(themeFolder, themeName));
        }
      },
      handleHotUpdate(context) {
        const themePath = path.resolve(themeFolder);
        const contextPath = path.resolve(context.file);
        if(contextPath.startsWith(themePath)) {
          processThemeResources(themeOptions, console);
        }
      },
    }
  ]
});
