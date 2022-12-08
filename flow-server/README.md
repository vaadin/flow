## Flow Webpack plugins

Flow now uses webpack plugins to make the `webpack.generated.js` cleaner and easier to extend
without cluttering the file and making it long and complex.

The files get installed with the task `TaskInstallFrontendBuildPlugins` which reads the `plugins.json`
in from `src/main/resources/plugins` and installs the plugins named here e.g.

```json
{
  "plugins": [
    "stats-plugin"
  ]
}
```

The plugin itself should also be contained in `src/main/resources/plugins` with the
folder name being the same as the plugin name.

For stats-plugin this means it should be located in `src/main/resources/plugins/stats-plugin`.

The plugin folder needs to contain the plugin javascript files plus a package.json with at least the fields
`version`, `main`, `files` filled where:
  * `version` is the semver version for the plugin. 
  (Plugin will not be updated if the same version already exists)
  * `main` depicts the main js file for the plugin.
  * `files` contains all files the plugin needs.
   (only these files will be copied)

The full information would be preferred:

```json
{
  "description": "stats-plugin",
  "keywords": [
    "plugin"
  ],
  "repository": "vaadin/flow",
  "name": "@vaadin/stats-plugin",
  "version": "1.0.0",
  "main": "stats-plugin.js",
  "author": "Vaadin Ltd",
  "license": "Apache-2.0",
  "bugs": {
    "url": "https://github.com/vaadin/flow/issues"
  },
  "files": [
    "stats-plugin.js"
  ]
}
```

For creating a plugin see [Writing a plugin](https://webpack.js.org/contribute/writing-a-plugin/)

## Working with plugin in a project

Testing a plugin is often easier by using it in a project.
As by default all plugins are re-copied from the `flow-server.jar` making changes
would mean building the server module each time.

The plugins are copied to `target/plugins` from there they are "installed" to `node_modules` with (p)npm.
To make editing and testing easier one can add `"update": false,` to the plugin package json that will make it not be overwritten.

## Using a Flow webpack plugin

The flow plugins get installed to `node_modules/@vaadin` which means that using them we should use the for `@vaadin/${plugin-name}`

As the plugins are meant for internal use the are added to `webpack.generated.js` and
used from there.

First we need to import the webpack plugin

```js
const StatsPlugin = require('@vaadin/stats-plugin');
```

then add the plugin with required options to `plugins` in the generated file
```js
  plugins: [
    new StatsPlugin({
      devMode: devMode,
      statsFile: statsFile,
      setResults: function (statsFile) {
        stats = statsFile;
      }
    }),
 ]
```

### Logs from plugins and loaders

To set logging for the plugins one can add to the `webpack.config.js`
file:
```js
  {
    stats: {
      logging: 'log'
    }
  }
```

where the accepted logging levels are:
 - 'none', false - disable logging
 - 'error' - errors only
 - 'warn' - errors and warnings only
 - 'info' - errors, warnings, and info messages
 - 'log', true - errors, warnings, info messages, log messages, groups, clears. Collapsed groups are displayed in a collapsed state.
 - 'verbose' - log everything except debug and trace. Collapsed groups are displayed in expanded state.

For debug level logs one should have the settings as:

```js
  {
    stats: {
      logging: 'log',
      loggingDebug: [
        'theme-loader',
        'ApplicationThemePlugin',
        'FlowIdPlugin'
      ]
    }
  }
```
