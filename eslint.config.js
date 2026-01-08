import { fileURLToPath } from 'node:url';
import tsEslint from 'typescript-eslint';
import tsRequireTypeChecking from 'eslint-config-vaadin/typescript-requiring-type-checking';
import tsImports from 'eslint-config-vaadin/imports-typescript';
import testing from 'eslint-config-vaadin/testing';
import prettier from 'eslint-config-vaadin/prettier';

const root = new URL('./', import.meta.url);

const config = tsEslint.config(
  tsRequireTypeChecking,
  tsImports,
  testing,
  prettier,
  {
    rules: {
      'import-x/no-unassigned-import': 'off',
      'import-x/no-duplicates': 'off',
      '@typescript-eslint/no-use-before-define': 'off',
      '@typescript-eslint/unbound-method': 'off',
      '@typescript-eslint/no-shadow': 'off',
      'import-x/prefer-default-export': 'off',
    },
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: fileURLToPath(root),
      },
    },
  },
);

export default config;
