module.exports = {
  "env": {
    "browser": true,
    "es6": true
  },
  "parserOptions": {
    "ecmaVersion": 2017,
    "sourceType": "module"
  },
  "ignorePatterns": ["**/FlowClient.js", "**/copy-to-clipboard.js"],

  "overrides": [
    {
      "files": ["**/*.js"],
      "extends": "eslint:recommended",
      "rules": {
        "no-prototype-builtins": 1,
      },
    },

    {
      "files": ["**/.eslintrc.js"],
      "globals": {
        "module": "readonly",
      },
    },

    {
      "files": ["**/*.ts"],

      "extends": "vaadin",
      "plugins": ["@typescript-eslint"],
      "parser": "@typescript-eslint/parser",

      "rules": {
        "@typescript-eslint/no-empty-function": 1,
        "@typescript-eslint/ban-ts-comment": 1,
        "@typescript-eslint/ban-types": 1,
        "default-case": 1,
        "import/no-cycle": 1,
        "prefer-destructuring": 1,
        "no-multi-assign": 1,
        "no-ex-assign": 1,
        "no-return-assign": 1,
        "no-use-before-define": 1,
        "no-useless-constructor": 0,
        "@typescript-eslint/no-useless-constructor": 1,
        "prefer-template": 1,

        "@typescript-eslint/explicit-module-boundary-types": 0,
        "@typescript-eslint/no-explicit-any": 0,
        "@typescript-eslint/no-inferrable-types": 0,
        "@typescript-eslint/no-non-null-assertion": 0,
        "lines-between-class-members": 0,
        "max-classes-per-file": 0,
        "no-else-return": 0,
        "no-nested-ternary": 0,
        "no-restricted-globals": 0,
        "no-restricted-syntax": 0
      },
    },
  ]
};
