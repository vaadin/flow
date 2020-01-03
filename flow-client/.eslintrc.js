module.exports = {
    "env": {
        "browser": true,
        "es6": true
    },
    "extends": "vaadin",
    "globals": {
        "Atomics": "readonly",
        "SharedArrayBuffer": "readonly"
    },
    "parserOptions": {
        "ecmaVersion": 2017,
        "sourceType": "module"
    },
    "rules": {
    },
    "overrides": [{
        "files": ["FlowClient.js"],

        "rules": {
            "brace-style": "off",
            "camelcase": "off",
            "comma-spacing": "off",
            "curly": "off",
            "indent": "off",
            "key-spacing": "off",
            "keyword-spacing": "off",
            "max-len": "off",
            "no-caller": "off",
            "no-constant-condition": "off",
            "no-control-regex": "off",
            "no-debugger": "off",
            "no-empty": "off",
            "no-ex-assign": "off",
            "no-extra-semi": "off",
            "no-func-assign": "off",
            "no-invalid-this": "off",
            "no-redeclare": "off",
            "no-self-assign": "off",
            "no-throw-literal": "off",
            "no-unused-vars": "off",
            "quotes": "off",
            "semi": "off",
            "semi-spacing": "off",
            "space-before-blocks": "off",
            "space-before-function-paren": "off",
            "space-infix-ops": "off",
            "no-trailing-spaces": "off",
            "func-call-spacing": "off"
        }
    }]
};
