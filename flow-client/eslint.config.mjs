import prettier from 'eslint-config-vaadin/prettier';
import typescript from 'eslint-config-vaadin/typescript';
import globals from 'globals';

export default [
  {
    ignores: ['target/**/*', '**/*.js', '*.mjs']
  },
  ...typescript,
  ...prettier,
  {
    languageOptions: {
      parserOptions: {
        projectService: {
          allowDefaultProject: ['src/test/frontend/*'],
          // The migration adds a *Tests.ts per converted module; raise the
          // default-project cap (default 8) so linting keeps working as the
          // test suite grows.
          maximumDefaultProjectFileMatchCount_THIS_WILL_SLOW_DOWN_LINTING: 100
        }
      }
    },
    rules: {
      '@typescript-eslint/ban-ts-comment': 'off',
      '@typescript-eslint/class-methods-use-this': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      '@typescript-eslint/method-signature-style': 'off',
      '@typescript-eslint/no-invalid-void-type': 'off',
      '@typescript-eslint/no-shadow': 'off',
      '@typescript-eslint/no-unused-expressions': 'off',
      '@typescript-eslint/no-unused-vars': 'off',
      '@typescript-eslint/no-use-before-define': 'off',
    }
  },
  {
    // Engine code must log through internal/Console, which suppresses browser
    // logging in production mode (unless the vaadin.browserLog localStorage flag
    // is set). Calling `console` directly bypasses that, so it is an error here;
    // the few deliberately ungated sites carry an inline disable comment.
    files: ['src/main/frontend/internal/**/*.ts'],
    ignores: ['src/main/frontend/internal/Console.ts'],
    rules: {
      'no-console': 'error'
    }
  },
  {
    files: ['src/**/frontend/**/*'],
    languageOptions: {
      globals: {
        ...globals.browser
      }
    }
  },
  {
    files: ['src/test/frontend/*.ts'],
    languageOptions: {
      globals: {
        ...globals.mocha
      }
    }
  }
];
