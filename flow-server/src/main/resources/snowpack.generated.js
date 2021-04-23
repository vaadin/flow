module.exports = {
  mount: {
    // Snowpack will resolve requests to /VAADIN/some/file
    // as frontend/some/file. Most notably, the request to
    // /VAADIN/index.html and .js would be resolved by
    // frontend/index.html and .js
    frontend: '/VAADIN',

    // Snowpack will resolve requests to /VAADIN/flow-frontend/some/file
    // as target/flow-frontend/some/file. Most notably, the request to
    // /VAADIN/flow-frontend/VaadinDevmodeGizmo.js would be resolved by
    // target/flow-frontend/VaadinDevmodeGizmo.js
    'target/flow-frontend': '/VAADIN/flow-frontend'

    // Files outside of ./frontend and ./target/flow-frontend won't
    // be accessible through the snowpack dev server
  },
  plugins: [
    './snowpack-plugin-css-string.js' // replacement for lit-css-loader of webpack
  ],
  devOptions: {
    hmr: false,
    open: 'none',
    output: 'stream'
  },
  buildOptions: {
    // Prepend VAADIN/ to all web modules URLs served by the snowpack dev
    // server so that Vaadin's DevModeHandler correctly identifies such
    // requests as 'dev mode' and passes them to the snowpack dev server.
    // (example: /VAADIN/_snowpack/pkg/lit-element.js)
    // See com.vaadin.flow.server.DevModeHandler.isDevModeRequest()
    metaUrlPath: 'VAADIN/_snowpack',
  },
  alias: {
    // the Frontend alias feature
    'Frontend': './frontend',

    // quick fix for the missing Vaadin theme plugin
    'themes': './frontend/themes',
  },
  // Serve the .js files from node_modules and flow-frontend as they are,
  // even if there are .ts sources along with .js compiled output.
  exclude: [
    '**/node_modules/**/*',
    '**/target/flow-frontend/**/*',
  ]
}