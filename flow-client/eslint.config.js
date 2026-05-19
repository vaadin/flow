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
          // Test files aren't covered by tsconfig.json; bump the default
          // project file cap so growing the test suite doesn't trip the
          // typescript-eslint guard rail.
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
