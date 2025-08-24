import { fileURLToPath } from 'node:url';
import tsEslint from 'typescript-eslint';
import baseConfig from '../../eslint.config.js';

const root = new URL('./', import.meta.url);

const config = tsEslint.config(
  baseConfig,
  {
    files: ['src/**/*.ts', 'src/**/*.js'],
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: fileURLToPath(root),
      },
    },
  },
);

export default config;
