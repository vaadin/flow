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
    './snowpack-plugin-css-string.js' // replacement for lit-css-loader in webpack
  ],
  devOptions: {
    hmr: false,
    open: 'none',
    output: 'stream'
  },
  buildOptions: {
    // Prepend with VAADIN/ so that Vaadin Dev Mode Handler correctly detects requests
    // to node modules as 'dev mode' requests and passes them to the snowpack dev server
    // See com.vaadin.flow.server.DevModeHandler.isDevModeRequest()
    metaUrlPath: 'VAADIN/_snowpack',
  },
  alias: {
    // the Frontend alias feature
    'Frontend': './frontend',

    // quick fix for the missing Vaadin theme plugin
    'themes': './frontend/themes',

    // Fix a build error:
    // @vaadin/flow-frontend: Unscannable package import found.
    // Snowpack scans source files for package imports at startup, and on every change.
    // But, sometimes an import gets added during the build process, invisible to our file scanner.
    // We'll prepare this package for you now, but should add "@vaadin/flow-frontend" to "knownEntrypoints"
    // in your config file so that this gets prepared with the rest of your imports during startup.
    '@vaadin/flow-frontend': './target/flow-frontend',
    '@vaadin/form': './target/flow-frontend/form'
  },
  // Serve the .js files from node_modules and flow-frontend as they are,
  // even if there are .ts sources along with .js compiled output.
  exclude: [
    '**/node_modules/**/*',
    '**/target/flow-frontend/**/*.ts', // need to explicitly specify .ts here to workaround a snowpack quirk
  ],

  // Fix a build warning:
  // @vaadin/flow-frontend: Locally linked package detected outside of project root.
  // If you are working in a workspace/monorepo, set your snowpack.config.js "workspaceRoot" to your workspace
  // directory to take advantage of fast HMR updates for linked packages. Otherwise, this package will be
  // cached until its package.json "version" changes. To silence this warning, set "workspaceRoot: false".
  workspaceRoot: false,
}