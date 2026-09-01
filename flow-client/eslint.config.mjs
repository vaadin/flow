import prettier from 'eslint-config-vaadin/prettier';
import typescript from 'eslint-config-vaadin/typescript';
import globals from 'globals';
import tsdoc from 'eslint-plugin-tsdoc';

export default [
  {
    ignores: ['target/**/*', '**/*.js', '*.mjs']
  },
  ...typescript,
  ...prettier,
  {
    // Validate the TSDoc/JSDoc syntax of doc comments in the migrated modules so
    // ported Javadoc tags stay well-formed (e.g. @param/@returns/@typeParam and
    // {@link} references) as the series grows. Scoped to the ported engine
    // modules under internal/; the pre-existing files above this directory still
    // carry Java-style @code Javadoc and are out of scope for this migration.
    files: ['src/main/frontend/internal/**/*.ts'],
    plugins: { tsdoc },
    rules: {
      'tsdoc/syntax': 'error'
    }
  },
  {
    languageOptions: {
      parserOptions: {
        projectService: {
          // The ported suites belong to src/test/frontend/tsconfig.json, which
          // the project service finds by walking up from each file. Only the
          // two root-level suites need the default project: they predate the
          // port and are not type-clean, so that project leaves them out.
          allowDefaultProject: ['src/test/frontend/*']
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
    ignores: ['src/main/frontend/internal/client/Console.ts'],
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
    files: ['src/test/frontend/**/*.ts'],
    languageOptions: {
      globals: {
        ...globals.mocha
      }
    },
    rules: {
      // Tests exercise the full public surface of each ported module, including
      // members that are @deprecated to mirror their Java originals (e.g. the
      // BrowserInfo browser-family probes). Calling them from tests is expected.
      '@typescript-eslint/no-deprecated': 'off'
    }
  }
];
