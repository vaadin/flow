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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.internal.enterprise.test.FileProperty
import java.io.File

public interface VaadinFlowPluginExtension {
    /**
     * Whether or not we are running in productionMode. Defaults to false.
     * Responds to the `-Pvaadin.productionMode` property.
     */
    public val productionMode: Property<Boolean>

    /**
     * The folder where webpack should output index.js and other generated
     * files. Defaults to `null` which will use the auto-detected value of
     * resoucesDir of the main SourceSet, usually `build/resources/main/META-INF/VAADIN/webapp/`.
     */
    public val webpackOutputDirectory: Property<File>

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    public val npmFolder: Property<File>

    /**
     * A directory with project's frontend source files.
     *
     * Defaults to `frontend`
     */
    public val frontendDirectory: Property<File>

    /**
     * Whether to generate a bundle from the project frontend sources or not. Defaults to true.
     */
    public val generateBundle: Property<Boolean>

    /**
     * Whether to run `npm install` after updating dependencies. Defaults to true.
     */
    public val runNpmInstall: Property<Boolean>

    /**
     * Whether to generate embeddable web components from WebComponentExporter
     * inheritors. Defaults to true.
     */
    public val generateEmbeddableWebComponents: Property<Boolean>

    /**
     * Defines the project frontend directory from where resources should be
     * copied from for use with webpack. Defaults to [Constants.LOCAL_FRONTEND_RESOURCES_PATH]
     */
    public val frontendResourcesDirectory: Property<File>

    /**
     * Whether to use byte code scanner strategy to discover frontend
     * components. Defaults to true.
     */
    public val optimizeBundle: Property<Boolean>

    /**
     * Instructs to use pnpm for installing npm frontend resources. Default is [Constants.ENABLE_PNPM_DEFAULT]
     *
     * pnpm, a.k.a. performant npm, is a better front-end dependency management option.
     * With pnpm, packages are cached locally by default and linked (instead of
     * downloaded) for every project. This results in reduced disk space usage
     * and faster recurring builds when compared to npm.
     */
    public val pnpmEnable: Property<Boolean>

    /**
     * Instructs to use bun for installing npm frontend resources. Default is false.
     *
     * bun, is a better front-end dependency management option.
     * With bun, packages are cached locally by default and linked (instead of
     * downloaded) for every project. This results in reduced disk space usage
     * and faster recurring builds when compared to npm.
     */
    public val bunEnable: Property<Boolean>

    /**
     * Whether the globally installed pnpm tool is used. By default, the
     * pinned supported version of pnpm is used, see
     * [FrontendTools.DEFAULT_PNPM_VERSION].
     */
    public val useGlobalPnpm: Property<Boolean>

    /**
     * Whether vaadin home node executable usage is forced. If it's set to
     * `true` then vaadin home 'node' is checked and installed if it's
     * absent. Then it will be used instead of globally 'node' or locally
     * installed installed 'node'.
     *
     * Defaults to false.
     */
    public val requireHomeNodeExec: Property<Boolean>

    /**
     * Whether or not insert the initial Uidl object in the bootstrap index.html. Defaults to false.
     * Responds to the `-Pvaadin.eagerServerLoad` property.
     */
    public val eagerServerLoad: Property<Boolean>

    /**
     * Application properties file in Spring project.
     * Defaults to `src/main/resources/application.properties`
     */
    public val applicationProperties: Property<File>

    /**
     * Default generated path of the OpenAPI json.
     *
     * Defaults to `generated-resources/openapi.json`.
     */
    public val openApiJsonFile: Property<File>

    /**
     * Java source folders for connect scanning.
     */
    public val javaSourceFolder: Property<File>

    /**
     * Java resource folder.
     */
    public val javaResourceFolder: Property<File>

    /**
     * The folder where flow will put TS API files for client projects.
     */
    public val generatedTsFolder: Property<File>

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example `"v16.0.0"`. Defaults to [FrontendTools.DEFAULT_NODE_VERSION].
     */
    public val nodeVersion: Property<String>

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to [NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT].
     *
     * Example: `"https://nodejs.org/dist/"`.
     */
    public val nodeDownloadRoot: Property<String>

    /**
     * Allow automatic update of node installed to alternate location. Default `false`
     */
    public val nodeAutoUpdate: Property<Boolean>

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
    public val resourceOutputDirectory: Property<File>

    /**
     * Defines the output folder used by the project.
     *
     * Default value is the `project.buildDir` and should not need to be changed.
     */
    public val projectBuildDir: Property<String>

    /**
     * Defines the npm packages to run postinstall for.
     */
    public val postinstallPackages: ListProperty<String>

    public val classpathFilter: Property<ClasspathFilter>

    /**
     * The name of the SourceSet to scan for Vaadin components - i.e. the classes that are annoated with
     * Vaadin annotations.
     *
     * Defaults to `"main"`
     */
    public val sourceSetName: Property<String>

    /**
     * The Gradle scope the Vaadin dependencies have been added to. Defaults to 'runtimeClasspath' if
     * no sourceSetName has been specified, or '<code>sourceSetName</code>RuntimeClasspath' if a non-main sourceset
     * has been set.
     */
    public val dependencyScope: Property<String>

    /**
     * The Gradle task that the `vaadinPrepareFrontend` task must run before. The target task should run before
     * or be the task that copies the files from the resources directories of the specified SourceSet to the relevant
     * output directory for that SourceSet. Defaults to 'processResources' if no sourceSetName has been specified, or
     * 'process<code>SourceSetName</code>Resources' if a non-main sourceset has been specified.
     */
    public val processResourcesTaskName: Property<String>

    /**
     * Parameter to control if frontend development server should be used in
     * development mode or not.
     *
     * Defaults to false.
     */
    public val frontendHotdeploy: Property<Boolean>

    /**
     * Setting this to true will run {@code npm ci} instead of {@code npm install} when using npm.
     *
     * If using pnpm, the install will be run with {@code --frozen-lockfile} parameter.
     *
     * This makes sure that the versions in package lock file will not be overwritten and production builds are reproducible.
     */
    public val ciBuild: Property<Boolean>

    /**
     * Enable skip of dev bundle rebuild if a dev bundle exists. Defaults to false.
     * @return `true` to skip dev bundle rebuild
     */
    public val skipDevBundleBuild: Property<Boolean>

    /**
     * Setting this to `true` will force a build of the production build
     * even if there is a default production bundle that could be used.
     *
     * Created production bundle optimization is defined by
     * [optimizeBundle] parameter.
     *
     * Defaults to `false`.
     */
    public val forceProductionBuild: Property<Boolean>

    /**
     * Prevents tracking state of the `vaadinPrepareFrontend` task, so that it
     * will re-run every time it is called.
     *
     * Setting this to `true` allows to always execute `vaadinPrepareFrontend`.
     *
     * Defaults to `false`, meaning that the task execution is skipped when its
     * outcomes are up-to-date, improving the overall build time.
     */
    public val alwaysExecutePrepareFrontend: Property<Boolean>

    /**
     * If `true` navigation error views implementing [HasErrorParameter]
     * can be rendered for exceptions during RPC request handling, not only limited
     * to exceptions thrown during navigation life-cycle.
     *
     * @return `true` to enable error view rendering in RPC, `false` by default
     */
    public val isErrorHandlerRedirect: Property<Boolean>

    public fun filterClasspath(@DelegatesTo(value = ClasspathFilter::class, strategy = Closure.DELEGATE_FIRST) block: Closure<*>) {
        block.delegate = classpathFilter.get()
        block.resolveStrategy = Closure.DELEGATE_FIRST
        block.call()
    }

    public fun filterClasspath(block: Action<ClasspathFilter>) {
        block.execute(classpathFilter.get())
    }

    public companion object {
        public fun get(project: Project): VaadinFlowPluginExtension =
                project.extensions.getByType(VaadinFlowPluginExtension::class.java)
    }
}

internal class PluginEffectiveConfiguration(
    private val project: Project,
    private val extension: VaadinFlowPluginExtension
) {
    val productionMode: Provider<Boolean> = extension.productionMode
        .convention(false)
        .overrideWithSystemProperty("vaadin.productionMode")

    val sourceSetName = extension.sourceSetName
        .convention("main")

    val webpackOutputDirectory: Provider<File> = extension.webpackOutputDirectory
        .convention(sourceSetName.map { File(project.getBuildResourcesDir(it), Constants.VAADIN_WEBAPP_RESOURCES) })

    val npmFolder: Provider<File> = extension.npmFolder
        .convention(project.projectDir)

    val frontendDirectory: Provider<File> = extension.frontendDirectory
        .convention(File(project.projectDir, "frontend"))

    val generateBundle: Provider<Boolean> = extension.generateBundle
        .convention(true)

    val runNpmInstall: Provider<Boolean> = extension.runNpmInstall
        .convention(true)

    val generateEmbeddableWebComponents: Provider<Boolean> = extension.generateEmbeddableWebComponents
        .convention(true)

    val frontendResourcesDirectory: Property<File> = extension.frontendResourcesDirectory
        .convention(File(project.projectDir, Constants.LOCAL_FRONTEND_RESOURCES_PATH))

    val optimizeBundle: Property<Boolean> = extension.optimizeBundle
        .convention(true)

    val pnpmEnable: Provider<Boolean> = extension.pnpmEnable
        .convention(Constants.ENABLE_PNPM_DEFAULT)
        .overrideWithSystemProperty(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM)

    val bunEnable: Provider<Boolean> = extension.bunEnable
        .convention(Constants.ENABLE_BUN_DEFAULT)
        .overrideWithSystemProperty(InitParameters.SERVLET_PARAMETER_ENABLE_BUN)

    val useGlobalPnpm: Provider<Boolean> = extension.useGlobalPnpm
        .convention(Constants.GLOBAL_PNPM_DEFAULT)
        .overrideWithSystemProperty(InitParameters.SERVLET_PARAMETER_GLOBAL_PNPM)

    val requireHomeNodeExec: Property<Boolean> = extension.requireHomeNodeExec
        .convention(false)

    val eagerServerLoad: Provider<Boolean> = extension.eagerServerLoad
        .convention(false)
        .overrideWithSystemProperty("vaadin.eagerServerLoad")

    val applicationProperties: Property<File> = extension.applicationProperties
        .convention(File(project.projectDir, "src/main/resources/application.properties"))

    val openApiJsonFile: Property<File> = extension.openApiJsonFile
        .convention(File(project.buildDir, "generated-resources/openapi.json"))

    val javaSourceFolder: Property<File> = extension.javaSourceFolder
        .convention(File(project.projectDir, "src/main/java"))

    val javaResourceFolder: Property<File> = extension.javaResourceFolder
        .convention(File(project.projectDir, "src/main/resources"))

    val generatedTsFolder: Property<File> = extension.generatedTsFolder
        .convention(File(project.projectDir, "frontend/generated"))

    val nodeVersion: Property<String> = extension.nodeVersion
        .convention(FrontendTools.DEFAULT_NODE_VERSION)

    val nodeDownloadRoot: Property<String> = extension.nodeDownloadRoot
        .convention(com.vaadin.flow.server.frontend.installer.Platform.guess().getNodeDownloadRoot())

    val nodeAutoUpdate: Property<Boolean> = extension.nodeAutoUpdate
        .convention(false)

    val resourceOutputDirectory: Property<File> = extension.resourceOutputDirectory
        .convention(File(project.buildDir, "vaadin-generated"))

    val projectBuildDir: Property<String> = extension.projectBuildDir
        .convention(project.buildDir.toString())

    val postinstallPackages: ListProperty<String> = extension.postinstallPackages
        .convention(listOf())

    val classpathFilter: Property<ClasspathFilter> = extension.classpathFilter

    val dependencyScope: Property<String> = extension.dependencyScope
        .convention(sourceSetName.map {
            if (it == "main") {
                "runtimeClasspath"
            } else {
                "${it}RuntimeClasspath"
            }
        })

    val processResourcesTaskName: Property<String> = extension.processResourcesTaskName
        .convention(sourceSetName.map {
            if (it == "main") {
                "processResources"
            } else {
                "process${it.replaceFirstChar(Char::titlecase)}Resources"
            }
        })

    val frontendHotdeploy: Property<Boolean> = extension.frontendHotdeploy
        .convention(false)

    val ciBuild: Provider<Boolean> = extension.ciBuild
        .convention(false)
        .overrideWithSystemProperty(InitParameters.CI_BUILD)

    val skipDevBundleBuild: Property<Boolean> = extension.skipDevBundleBuild
        .convention(false)

    val forceProductionBuild: Provider<Boolean> = extension.forceProductionBuild
        .convention(false)
        .overrideWithSystemProperty(InitParameters.FORCE_PRODUCTION_BUILD)

    val alwaysExecutePrepareFrontend: Property<Boolean> = extension.alwaysExecutePrepareFrontend
        .convention(false)

    val isErrorHandlerRedirect: Provider<Boolean> = extension.isErrorHandlerRedirect
        .convention(false)
        .overrideWithSystemProperty(InitParameters.ERROR_HANDLER_REDIRECT_ENABLED)

    /**
     * Finds the value of a boolean property. It searches in gradle and system properties.
     *
     * If the property is defined in both gradle and system properties, then the gradle property is taken.
     *
     * @param propertyName the property name
     * @return a new provider of the value, which either takes the original value if the system/gradle property is not present,
     * `true` if it's defined or if it's set to "true" and `false` otherwise.
     */
    private fun Provider<Boolean>.overrideWithSystemProperty(propertyName: String) : Provider<Boolean> = map { originalValue ->
        if (System.getProperty(propertyName) != null) {
            val value: String = System.getProperty(propertyName)
            val valueBoolean: Boolean = value.isBlank() || value.toBoolean()
            project.logger.info("Set $propertyName to $valueBoolean because of System property $propertyName='$value'")
            return@map valueBoolean
        }
        if (project.hasProperty(propertyName)) {
            val value: String = project.property(propertyName) as String
            val valueBoolean: Boolean = value.isBlank() || value.toBoolean()
            project.logger.info("Set $propertyName to $valueBoolean because of Gradle project property $propertyName='$value'")
            return@map valueBoolean
        }
        return@map originalValue
    }

    override fun toString(): String = "VaadinFlowPluginExtension(" +
            "productionMode=${productionMode.get()}, " +
            "webpackOutputDirectory=${webpackOutputDirectory.get()}, " +
            "npmFolder=${npmFolder.get()}, " +
            "frontendDirectory=${frontendDirectory.get()}, " +
            "generateBundle=${generateBundle.get()}, " +
            "runNpmInstall=${runNpmInstall.get()}, " +
            "generateEmbeddableWebComponents=${generateEmbeddableWebComponents.get()}, " +
            "frontendResourcesDirectory=${frontendResourcesDirectory.get()}, " +
            "optimizeBundle=${optimizeBundle.get()}, " +
            "pnpmEnable=${pnpmEnable.get()}, " +
            "bunEnable=${bunEnable.get()}, " +
            "ciBuild=${ciBuild.get()}, " +
            "forceProductionBuild=${forceProductionBuild.get()}, " +
            "useGlobalPnpm=${useGlobalPnpm.get()}, " +
            "requireHomeNodeExec=${requireHomeNodeExec.get()}, " +
            "eagerServerLoad=${eagerServerLoad.get()}, " +
            "applicationProperties=${applicationProperties.get()}, " +
            "openApiJsonFile=${openApiJsonFile.get()}, " +
            "javaSourceFolder=${javaSourceFolder.get()}, " +
            "javaResourceFolder=${javaResourceFolder.get()}, " +
            "generatedTsFolder=${generatedTsFolder.get()}, " +
            "nodeVersion=${nodeVersion.get()}, " +
            "nodeDownloadRoot=${nodeDownloadRoot.get()}, " +
            "nodeAutoUpdate=${nodeAutoUpdate.get()}, " +
            "resourceOutputDirectory=${resourceOutputDirectory.get()}, " +
            "projectBuildDir=${projectBuildDir.get()}, " +
            "postinstallPackages=${postinstallPackages.get()}, " +
            "sourceSetName=${sourceSetName.get()}, " +
            "dependencyScope=${dependencyScope.get()}, " +
            "processResourcesTaskName=${processResourcesTaskName.get()}, " +
            "skipDevBundleBuild=${skipDevBundleBuild.get()}, " +
            "alwaysExecutePrepareFrontend=${alwaysExecutePrepareFrontend.get()}, " +
            "isErrorHandlerRedirect=${isErrorHandlerRedirect.get()}, " +
            "frontendHotdeploy=${frontendHotdeploy.get()}" +
            ")"
    companion object {
        internal fun get(project: Project): PluginEffectiveConfiguration =
            PluginEffectiveConfiguration(project, VaadinFlowPluginExtension.get(project))
    }
}
