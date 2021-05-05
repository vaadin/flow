## Basic usage

### `prepare-frontend` goal
Goal that verifies requirements and collects needed information for development.

* Verifies that node and npm (or pnpm) tools are installed.
* Frontend resources that are available inside `.jar` dependencies will
be copied into the `node_modules` folder.
* If not available `package.json` and `webpack.config.json` files will be generated.
file.
* `package.json` and `webpack.generated.json` will be updated.

##### `prepare-frontend` configuration options
* **webpackTemplate** `String` - Copy the `webapp.config.js` from the specified URL if missing. Default is
     the template provided by this plugin. Set it to empty string to disable
     the feature.

* **webpackGeneratedTemplate** `String` - Copy the `webapp.generated.js` from the specified URL. Default is the
     template provided by this plugin. Set it to empty string to disable the
     feature.

### `build-frontend` goal
Goal that builds the frontend bundle.

The following actions are performed when creating a package:
* Update `package.json` file with the `@NpmPackage`
 annotations defined in the classpath
* Copy resource files used by flow from `.jar` files to the `node_modules`
 folder
* Install dependencies by running `npm install`
* Update the `generated-flow-imports.js` file imports with the
 `JsModule`, `Theme` and `JavaScript` annotations defined in
 the classpath
* Update `webpack.generated.js` file.

##### `build-frontend` configuration options
* **generateBundle** `boolean` - Whether to generate a bundle from the project frontend sources or not.
 Default value is: true
* **runNpmInstall** `boolean` - Whether to run <code>npm install</code> after updating dependencies.
 Default value is: true
* **generateEmbeddableWebComponents** `boolean` - Whether to generate embeddable web components from WebComponentExporter inheritors.
 Default value is: true
* **frontendResourcesDirectory** `File` - Defines the project frontend directory from where resources should be copied from for use with webpack.
 Default value is: ${project.basedir}/" + Constants.LOCAL_FRONTEND_RESOURCES_PATH)
* **optimizeBundle** `boolean` - Whether to use byte code scanner strategy to discover frontend components.
 Default value is: true

### General configuration options
* **npmFolder** `File` - The folder where `package.json` file is located. Default is project root dir.
 Default value is: ${project.basedir}
* **generatedFolder** `File` - The folder where flow will put generated files that will be used by webpack.
 Default value is: "${project.build.directory}/" + FRONTEND
* **frontendDirectory** `File` - A directory with project's frontend source files.
 Default value is: "${project.basedir}/" + FRONTEND
* **productionMode** `boolean` - Whether or not we are running in productionMode.
 Default value is: ${vaadin.productionMode}
* **useDeprecatedV14Bootstrapping** `String` - Whether or not we are running in legacy V14 bootstrap mode. True if defined or if it's set to true.
 Default value is: ${vaadin.useDeprecatedV14Bootstrapping}
* **eagerServerLoad** `boolean` - Whether or not insert the initial Uidl object in the bootstrap index.html.
 Default value is: "${vaadin." + Constants.SERVLET_PARAMETER_INITIAL_UIDL + "}"
* **webpackOutputDirectory** `File` - The folder where webpack should output index.js and other generated files.
 Default value is: "${project.build.outputDirectory}/" + VAADIN_SERVLET_RESOURCES
* **applicationProperties** `File` - Application properties file in Spring project.
 Default value is: ${project.basedir}/src/main/resources/application.properties
* **openApiJsonFile** `File` - Default generated path of the OpenAPI json.
 Default value is: ${project.build.directory}/generated-resources/openapi.json
* **javaSourceFolder** `File` - .Java source folders for scanning.
 Default value is: ${project.basedir}/src/main/java
* **generatedTsFolder** `File` - The folder where flow will put TS API files for client projects.
 Default value is: "${project.basedir}/" + FRONTEND + "/generated"
* **pnpmEnable** `boolean` - Instructs to use pnpm for installing npm frontend resources.
 Default value is: true
* **requireHomeNodeExec** `boolean` - Whether vaadin home node executable usage is forced. 
 If it's set to {@code true} then vaadin home 'node' is checked and installed if it's absent. 
 Then it will be used instead of globally 'node' or locally installed installed 'node'.
 Default value is: false
