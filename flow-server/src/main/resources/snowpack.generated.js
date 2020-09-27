// This is a generated file. It will be overwritten by Flow.

const fs = require('fs');
const path = require('path');

const cwd = process.cwd();
const isTS = fs.existsSync(path.join(cwd, 'tsconfig.json'));

// This is just a hack to monitor files in ./frontend for live reload.
// This should be done by a suitable hook into Snowpack or parsing console log
// in DevModeHandler (this is problematic due to the dashboard).
const chokidar = require('chokidar');
const watcher = chokidar.watch('./frontend', { persistent: true });
watcher
  .on('change', path =>
    process.stdout.write(`Frontend file has been changed: ${path}\n`))

module.exports = {
  mount: {
    frontend: '/VAADIN',
    target: '/VAADIN'
  },
  plugins: [ './snowpack-plugin-css-string.js' ],
  devOptions: {
    hmr: false,
    open: 'none'
  },
  buildOptions: {
    webModulesUrl: '/VAADIN/webmodules'
  },
  installOptions: {
    installTypes: isTS,
  },
  alias: {
    "Frontend": "/VAADIN",
    "@vaadin/flow-frontend": "/VAADIN/flow-frontend"
  }
};
