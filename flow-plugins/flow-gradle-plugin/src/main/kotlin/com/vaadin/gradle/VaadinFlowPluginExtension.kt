/**
 *    Copyright 2000-2023 Vaadin Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.gradle

import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.InitParameters
import com.vaadin.flow.server.frontend.FrontendTools
import com.vaadin.flow.server.frontend.installer.NodeInstaller
import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

public open class VaadinFlowPluginExtension(project: Project) {
    /**
     * Whether or not we are running in productionMode. Defaults to false.
     * Responds to the `-Pvaadin.productionMode` property.
     */
    public var productionMode: Boolean = false

    /**
     * The folder where webpack should output index.js and other generated
     * files. Defaults to `null` which will use the auto-detected value of
     * resoucesDir of the main SourceSet, usually `build/resources/main/META-INF/VAADIN/webapp/`.
     */
    public var webpackOutputDirectory: File? = null

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    public var npmFolder: File = project.projectDir

    /**
     * A directory with project's frontend source files.
     */
    public var frontendDirectory: File = File(project.projectDir, "frontend")

    /**
     * Whether to generate a bundle from the project frontend sources or not.
     */
    public var generateBundle: Boolean = true

    /**
     * Whether to run `npm install` after updating dependencies.
     */
    public var runNpmInstall: Boolean = true

    /**
     * Whether to generate embeddable web components from WebComponentExporter
     * inheritors.
     */
    public var generateEmbeddableWebComponents: Boolean = true

    /**
     * Defines the project frontend directory from where resources should be
     * copied from for use with webpack.
     */
    public var frontendResourcesDirectory: File = File(project.projectDir, Constants.LOCAL_FRONTEND_RESOURCES_PATH)

    /**
     * Whether to use byte code scanner strategy to discover frontend
     * components.
     */
    public var optimizeBundle: Boolean = true

    /**
     * Instructs to use pnpm for installing npm frontend resources. Default is [Constants.ENABLE_PNPM_DEFAULT]
     *
     * pnpm, a.k.a. performant npm, is a better front-end dependency management option.
     * With pnpm, packages are cached locally by default and linked (instead of
     * downloaded) for every project. This results in reduced disk space usage
     * and faster recurring builds when compared to npm.
     */
    public var pnpmEnable: Boolean = Constants.ENABLE_PNPM_DEFAULT

    /**
     * Whether the globally installed pnpm tool is used. By default, the
     * pinned supported version of pnpm is used, see [FrontendTools
     * .DEFAULT_PNPM_VERSION].
     */
    public var useGlobalPnpm: Boolean = Constants.GLOBAL_PNPM_DEFAULT

    /**
     * Whether vaadin home node executable usage is forced. If it's set to
     * `true` then vaadin home 'node' is checked and installed if it's
     * absent. Then it will be used instead of globally 'node' or locally
     * installed installed 'node'.
     */
    public var requireHomeNodeExec: Boolean = false

    /**
     * Whether or not insert the initial Uidl object in the bootstrap index.html. Defaults to false.
     * Responds to the `-Pvaadin.eagerServerLoad` property.
     */
    public var eagerServerLoad: Boolean = false

    /**
     * Application properties file in Spring project.
     */
    public var applicationProperties: File = File(project.projectDir, "src/main/resources/application.properties")

    /**
     * Default generated path of the OpenAPI json.
     */
    public var openApiJsonFile: File = File(project.buildDir, "generated-resources/openapi.json")

    /**
     * Java source folders for connect scanning.
     */
    public var javaSourceFolder: File = File(project.projectDir, "src/main/java")

    /**
     * Java resource folder.
     */
    public var javaResourceFolder: File = File(project.projectDir, "src/main/resources")

    /**
     * The folder where flow will put TS API files for client projects.
     */
    public var generatedTsFolder: File = File(project.projectDir, "frontend/generated")

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example `"v16.0.0"`. Defaults to [FrontendTools.DEFAULT_NODE_VERSION].
     */
    public var nodeVersion: String = FrontendTools.DEFAULT_NODE_VERSION

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to [NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT].
     *
     * Example: `"https://nodejs.org/dist/"`.
     */
    public var nodeDownloadRoot: String =
        com.vaadin.flow.server.frontend.installer.Platform.guess().getNodeDownloadRoot()

    /**
     * Allow automatic update of node installed to alternate location. Default `false`
     */
    public var nodeAutoUpdate: Boolean = false

    /**
     * Defines the output directory for generated non-served resources, such as
     * the token file. Defaults to `build/vaadin-generated` folder.
     *
     * The plugin will automatically register
     * this as an additional resource folder, which should then be picked up by the IDE.
     * That will allow the app to run for example in Intellij with Tomcat.
     * Generating files into build/resources/main wouldn't work since Intellij+Tomcat
     * ignores that folder.
     *
     * The `flow-build-info.json` file is generated here.
     */
    public var resourceOutputDirectory: File = File(project.buildDir, "vaadin-generated")

    /**
     * Defines the output folder used by the project.
     *
     * Default value is the `project.buildDir` and should not need to be changed.
     */
    public var projectBuildDir: String = project.buildDir.toString()

    /**
     * Defines the npm packages to run postinstall for.
     */
    public var postinstallPackages: List<String> = listOf()

    public var classpathFilter: ClasspathFilter = ClasspathFilter()

    /**
     * The name of the SourceSet to scan for Vaadin components - i.e. the classes that are annoated with
     * Vaadin annotations.
     */
    public var sourceSetName : String = "main"

    /**
     * The Gradle scope the Vaadin dependencies have been added to. Defaults to 'runtimeClasspath' if
     * no sourceSetName has been specified, or '<code>sourceSetName</code>RuntimeClasspath' if a non-main sourceset
     * has been set.
     */
    public var dependencyScope : String? = null

    /**
     * The Gradle task that the `vaadinPrepareFrontend` task must run before. The target task should run before
     * or be the task that copies the files from the resources directories of the specified SourceSet to the relevant
     * output directory for that SourceSet. Defaults to 'processResources' if no sourceSetName has been specified, or
     * 'process<code>SourceSetName</code>Resources' if a non-main sourceset has been specified.
     */
    public var processResourcesTaskName : String? = null

    /**
     * Parameter to control if frontend development server should be used in
     * development mode or not.
     */
    public var frontendHotdeploy: Boolean = false

    /**
     * Setting this to true will run {@code npm ci} instead of {@code npm install} when using npm.
     *
     * If using pnpm, the install will be run with {@code --frozen-lockfile} parameter.
     *
     * This makes sure that the versions in package lock file will not be overwritten and production builds are reproducible.
     */
    public var ciBuild: Boolean = false

    /**
     * Enable skip of dev bundle rebuild if a dev bundle exists.
     *
     * @return `true` to skip dev bundle rebuild
     */
    public var skipDevBundleBuild: Boolean = false


    /**
     * Setting this to `true` will force a build of the production build
     * even if there is a default production bundle that could be used.
     *
     * Created production bundle optimization is defined by
     * [.optimizeBundle] parameter.
     */
    public var forceProductionBuild: Boolean = false

    /**
     * Prevents tracking state of the `vaadinPrepareFrontend` task, so that it
     * will re-run every time it is called.
     *
     * Setting this to `true` allows to always execute `vaadinPrepareFrontend`.
     *
     * Defaults to `false`, meaning that the task execution is skipped when its
     * outcomes are up-to-date, improving the overall build time.
     */
    public var alwaysExecutePrepareFrontend: Boolean = false

    public fun filterClasspath(@DelegatesTo(value = ClasspathFilter::class, strategy = Closure.DELEGATE_FIRST) block: Closure<*>? = null): ClasspathFilter {
        if (block != null) {
            block.delegate = classpathFilter
            block.resolveStrategy = Closure.DELEGATE_FIRST
            block.call()
        }
        return classpathFilter
    }

    public fun filterClasspath(block: Action<ClasspathFilter>): ClasspathFilter {
        block.execute(classpathFilter)
        return classpathFilter
    }

    public companion object {
        public fun get(project: Project): VaadinFlowPluginExtension =
                project.extensions.getByType(VaadinFlowPluginExtension::class.java)
    }

    internal fun autoconfigure(project: Project) {
        // calculate webpackOutputDirectory if not set by the user
        if (webpackOutputDirectory == null) {
            webpackOutputDirectory = File(project.buildResourcesDir, Constants.VAADIN_WEBAPP_RESOURCES)
        }

        val productionModeProperty: Boolean? = project.getBooleanProperty("vaadin.productionMode")
        if (productionModeProperty != null) {
            productionMode = productionModeProperty
        }

        val eagerServerLoadProperty: Boolean? = project.getBooleanProperty("vaadin.eagerServerLoad")
        if (eagerServerLoadProperty != null) {
            eagerServerLoad = eagerServerLoadProperty
        }

        val pnpmEnableProperty: Boolean? = project.getBooleanProperty(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM)
        if (pnpmEnableProperty != null) {
            pnpmEnable = pnpmEnableProperty
        }

        val ciBuildProperty: Boolean? = project.getBooleanProperty(InitParameters.CI_BUILD)
        if (ciBuildProperty != null) {
            ciBuild = ciBuildProperty
        }

        val forceProductionBuildProperty: Boolean? = project.getBooleanProperty(InitParameters.FORCE_PRODUCTION_BUILD)
        if (forceProductionBuildProperty != null) {
            forceProductionBuild = forceProductionBuildProperty
        }

        val useGlobalPnpmProperty: Boolean? = project.getBooleanProperty(InitParameters.SERVLET_PARAMETER_GLOBAL_PNPM)
        if (useGlobalPnpmProperty != null) {
            useGlobalPnpm = useGlobalPnpmProperty
        }

        // calculate processResourcesTaskName if not set by user
        if (processResourcesTaskName == null) {
            processResourcesTaskName = if (sourceSetName == "main") {
                "processResources"
            } else {
                "process${sourceSetName.replaceFirstChar(Char::titlecase)}Resources"
            }
        }

        // calculate dependencyScope if not set by user
        if (dependencyScope == null) {
            dependencyScope = if (sourceSetName == "main") {
                "runtimeClasspath"
            } else  {
                sourceSetName + "RuntimeClasspath"
            }
        }
    }

    override fun toString(): String = "VaadinFlowPluginExtension(" +
            "productionMode=$productionMode, " +
            "webpackOutputDirectory=$webpackOutputDirectory, " +
            "npmFolder=$npmFolder, " +
            "frontendDirectory=$frontendDirectory, " +
            "generateBundle=$generateBundle, " +
            "runNpmInstall=$runNpmInstall, " +
            "generateEmbeddableWebComponents=$generateEmbeddableWebComponents, " +
            "frontendResourcesDirectory=$frontendResourcesDirectory, " +
            "optimizeBundle=$optimizeBundle, " +
            "pnpmEnable=$pnpmEnable, " +
            "ciBuild=$ciBuild, " +
            "forceProductionBuild=$forceProductionBuild, " +
            "useGlobalPnpm=$useGlobalPnpm, " +
            "requireHomeNodeExec=$requireHomeNodeExec, " +
            "eagerServerLoad=$eagerServerLoad, " +
            "applicationProperties=$applicationProperties, " +
            "openApiJsonFile=$openApiJsonFile, " +
            "javaSourceFolder=$javaSourceFolder, " +
            "javaResourceFolder=$javaResourceFolder, " +
            "generatedTsFolder=$generatedTsFolder, " +
            "nodeVersion=$nodeVersion, " +
            "nodeDownloadRoot=$nodeDownloadRoot, " +
            "nodeAutoUpdate=$nodeAutoUpdate, " +
            "resourceOutputDirectory=$resourceOutputDirectory, " +
            "postinstallPackages=$postinstallPackages, " +
            "sourceSetName=$sourceSetName, " +
            "dependencyScope=$dependencyScope, " +
            "processResourcesTaskName=$processResourcesTaskName" +
            ")"
}

internal val Project.buildResourcesDir: File get() {
    val sourceSets: SourceSetContainer = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
    return sourceSets.getByName(extensions.getByType(VaadinFlowPluginExtension::class.java).sourceSetName).output.resourcesDir!!
}
