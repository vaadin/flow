# Vaadin Gradle Plugin

This is the official Vaadin Gradle Plugin for Vaadin 19 and newer.
The implementation is based on `flow-plugin-base` which is also used by the Vaadin Maven plugin.

Prerequisites:
* Java 17 or higher
* node.js and npm. Vaadin will now automatically install node.js and npm, but you can also install those locally:
  * Windows/Mac: [node.js Download site](https://nodejs.org/en/download/)
  * Linux: Use package manager e.g. `sudo apt install npm`

As opposed to the older version of Gradle plugin, the new plugin doesn't create
projects any more. You can obtain a project example from:

* [Basic WAR project](https://github.com/vaadin/base-starter-gradle)
* [Spring Boot project](https://github.com/vaadin/base-starter-spring-gradle)

## Installation

You need to add the following lines to your `build.gradle` file
(please check the latest version at
[plugins.gradle.org](https://plugins.gradle.org/plugin/com.vaadin)):

```
plugins {
    id 'com.vaadin'
}
```

### Compatibility chart

Please follow this chart to learn which Plugin version to use with particular Vaadin version.
Vaadin recommends using the latest Vaadin LTS (Long-Term Support) version.

| Vaadin Gradle Plugin version | Supports                                       |
|------------------------------|------------------------------------------------|
| -                            | Vaadin 13 and lower are unsupported            |
| 0.6.0 and lower              | Vaadin 14.1.x LTS and lower                    |
| 0.7.0                        | Vaadin 14.2.x LTS                              |
| 0.14.3.7                     | All other Vaadin 14 LTS versions (recommended) |
| -                            | Vaadin 15 and 16 are unsupported               |
| 0.17.0.1                     | Vaadin 17 and higher                           |
| 0.20.0.0.alpha3              | experimental support for Vaadin 19 and 20      |
| 20.0.0                       | Vaadin 20 and higher                           |
| 24.0.0                       | Vaadin 24 and higher                           |

## Tasks

Most common commands for all projects:

* `./gradlew clean build` - builds the project and prepares the project for development. Automatically
  calls the `vaadinPrepareFrontend` task, but doesn't call the `vaadinBuildFrontend` task by default.
* `./gradlew clean vaadinPrepareFrontend` - quickly prepares the project for development.
* `./gradlew clean build -Pvaadin.productionMode` - will compile Vaadin in production mode,
   then packages everything into the war/jar archive. Automatically calls the
   `vaadinPrepareFrontend` and `vaadinBuildFrontend` tasks.

*Note* (after you built the project in production mode): In order to prepare the project
setup back to development mode, you must run `./gradlew vaadinPrepareFrontend`
with the `productionMode` effectively set to false (e.g. by ommitting the `-Pvaadin.productionMode` flag).

All tasks provided by the plugin:

* `vaadinClean` will clean the project completely and removes JavaScript packaging-related
  files such as `node_modules`, `package*.json`, `webpack.generated.js`, `pnpm-lock.yaml` and `pnpmfile.js`.
  You can use this task to clean up your project in case Vaadin throws mysterious exceptions,
  especially after you upgraded Vaadin to a newer version.
* `vaadinPrepareFrontend` will prepare your project for development. Calling this task
  will allow you to run the project e.g. in Tomcat with Intellij Ultimate.
  The task checks that node and npm tools are installed, copies frontend resources available inside
  `.jar` dependencies to `node_modules`, and creates or updates `package.json`/`pnpmfile.js` and `webpack.config.json` files.
* `vaadinBuildFrontend` will use webpack to compile all JavaScript and CSS files into one huge bundle in production mode,
  and will place that by default into the `build/vaadin-generated` folder. The folder is
  then later picked up by `jar` and `war` tasks which then package the folder contents properly
  onto the classpath. Note that this task is not automatically hooked into `war`/`jar`/`assemble`/`build` and
  need to be invoked explicitly. Note: this task is only triggered automatically if `productionMode` is set to true.

## Automatic Download of node.js and npm/pnpm

Since Vaadin Gradle Plugin 0.7.0, you no longer need to have node.js nor
npm installed in your system in order to use Vaadin.
Vaadin will download the node.js and npm (and pnpm if `pnpmEnable` is true) and will place it
into the `$HOME/.vaadin` folder.

This functionality is triggered automatically,
you do not need to call a Gradle task nor configure your CI environment in any special way.

## Multi-project builds

It is important to apply this plugin only to projects building the final war/jar artifact. You can
achieve that by having the `com.vaadin` plugin in the `plugins{}` block not applied by default, then
applying the plugin only in the war project:

```groovy
plugins {
  id 'java'
  id "com.vaadin" version "0.8.0" apply false
}

project("lib") {
  apply plugin: 'java'
}
project("web") {
  apply plugin: 'war'
  apply plugin: "com.vaadin"
  dependencies {
    compile project(':lib')
  }
}
```

## FAQ

Q: Why the `flow-server-production-mode.jar` file is missing in the WAR artifact built for production?

A: That jar file is actually not really needed. Its sole purpose is to set the `productionMode` `web.xml` context-param, however
   we're telling this information to Vaadin in a different way. Please see next question for more details.

Q: How is Vaadin configured for production mode?

A: Vaadin is configured via the `META-INF/VAADIN/config/flow-build-info.json` file.
   When the Plugin is set for production mode, either via `-Pvaadin.productionMode` or via `vaadin { productionMode = true }`,
   it will set the `productionMode` JSON property to true in the `flow-build-info.json` file.
   That will then tell Vaadin to run in production mode.

Q: How can I verify that the WAR file has been built correctly for production mode?

A: Please read the [Development vs production mode](https://mvysny.github.io/Vaadin-the-missing-guide/)
   guide to find the list of files which need to be present in a production-mode artifact.

## IDE Support

Intellij support for projects using Gradle and Vaadin Gradle Plugin is excellent.

There's a known issue with Eclipse and VSCode. Eclipse+BuildShip may need a workaround
in order for Gradle projects to work, please see [https://vaadin.com/forum/thread/18241436](https://vaadin.com/forum/thread/18241436) for more info.
This applies to Visual Studio Code (VSCode) as well since it also uses Eclipse bits and BuildShip
underneath - see [https://github.com/mvysny/vaadin14-embedded-jetty-gradle/issues/4](https://github.com/mvysny/vaadin14-embedded-jetty-gradle/issues/4)
for more details.

In order to run your project in production mode from your IDE, simply compile the
project in production mode, e.g. by using `-Pvaadin.productionMode`. The plugin will
then produce the `META-INF/VAADIN/config/flow-build-info.json` file with the `"productionMode": true` setting,
which will then cause Vaadin to start in production mode at runtime. To revert this setting
back to false, simply recompile your project without the `-Pvaadin.productionMode` switch.

# Developing The Plugin

Please read the Gradle Tutorial on [Developing Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)
to understand how Gradle plugins are developed.

The main entry to the plugin is the `VaadinPlugin` class. When applied to the project, it will register
all necessary tasks and extensions into the project.

Use Intellij (Community is enough) to open the project.

## Running The IT/Functional Tests

There is a comprehensive test suite which tests the plugin in various generated projects.
To run all tests from the suite:

```bash
./gradlew check
```

That will run the `functionalTest` task which will run all tests from the `src/functionalTest` folder.

### Running Individual Functional Tests from Intellij

Just right-click the test class and select "Run". If running the test fails, try one of the following:

1. Use Intellij, Community edition is enough
2. Go to "File / Settings / Build, Execution, Deployment / Build Tools / Gradle" and make sure that
   "Run tests using" is set to "Gradle".

## Developing the plugin and testing it on-the-fly at the same time

### Composite builds

You can take advantage of [composite builds](https://docs.gradle.org/current/userguide/composite_builds.html),
which will allow you to join together the plugin itself along with an example project using that plugin,
into one composite project. The easiest way is to use the [Base Starter Gradle](https://github.com/vaadin/base-starter-gradle)
example project.

1. Clone the Base Starter Gradle project and open it in Intellij
2. Create a `settings.gradle` file containing the following line: `includeBuild("/path/to/your/plugin/project/vaadin-gradle-plugin")`
   (use full path on your system to the Gradle Plugin project)
3. Reimport the Base Starter project: Gradle / Reimport. A new project named `vaadin-gradle-plugin`
   should appear in your workspace.
4. Open the terminal with Alt+F12.
5. If you now type `./gradlew vaadinPrepareFrontend` into the command line, Gradle will compile any changes done to
   the Gradle plugin and will run the updated plugin code. You can verify that by adding `println()` statements
   into the `VaadinPrepareFrontendTask` class.

If Gradle would complain that it can't download `beta` or `rc` artifacts (e.g. `flow-server-4.0.0.rc1`), just
add this to your app's `build.gradle` file:

```groovy
buildscript {
    repositories {
        maven { setUrl("https://maven.vaadin.com/vaadin-prereleases") }
    }
}
```

### Local Maven repository

Alternatively, you can build and publish the Flow Gradle plugin into the local Maven repository which allows using it in your Gradle project afterward:

1. Clone the Base Starter Gradle project.
2. Add `mavenLocal()` to `buildscript.repositories` as the first place to look up.
3. Add `dependencies { classpath 'com.vaadin:flow-gradle-plugin:24.9-SNAPSHOT' }` to `buildscript.repositories`.
4. Run `./gradlew clean build publishToMavenLocal` in the `flow-plugins/flow-gradle-plugin` repo folder.
5. Run the previous command with `-x functionalTest` to skip functional tests.
6. If you now run `./gradlew vaadinPrepareFrontend` in the Starter project folder, Gradle will use the local version of the Flow plugin. You can verify that by adding `println()` statements into the `VaadinPrepareFrontendTask` class.


## License

This plugin is distributed under the Apache License 2.0 license. For more information about the license see the LICENSE file
in the root directory of the repository. A signed CLA is required when contributing to the project.

See [CONTRIBUTING](CONTRIBUTING.md) for instructions for getting the plugin sources, and for compiling and using the plugin locally.
