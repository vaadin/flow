// This is a generated file. It will be overwritten by Flow.

const fs = require('fs');
const path = require('path');

const cwd = process.cwd();
const isTS = fs.existsSync(path.join(cwd, 'tsconfig.json'));

module.exports = {
  mount: {
    frontend: '/VAADIN',
    target: '/VAADIN'
  },
  plugins: [ './snowpack-plugin-css-string.js' ],
  devOptions: {
    hmr: false,
    open: 'none',
    output: 'stream'
  },
  buildOptions: {
    webModulesUrl: '/VAADIN/webmodules'
  },
  installOptions: {
    installTypes: isTS,
  },
  alias: {
    'Frontend': '/VAADIN',
    '@vaadin/flow-frontend': '/VAADIN/flow-frontend'
  },
  exclude: [
    '**/node_modules/**/*',
    '**/__tests__/*',
    '**/*.@(spec|test).@(js|mjs)',
    '**/flow-frontend/@(Connect|Flow|VaadinDevmodeGizmo).ts' ] // use precompiled
};
