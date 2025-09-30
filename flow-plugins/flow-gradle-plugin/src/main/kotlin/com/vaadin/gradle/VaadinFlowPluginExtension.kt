/**
 *    Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.gradle

import java.io.File
import java.io.Serializable
import javax.inject.Inject
import com.vaadin.flow.internal.StringUtil
import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.InitParameters
import com.vaadin.flow.server.frontend.FrontendTools
import com.vaadin.flow.server.frontend.FrontendToolsSettings
import com.vaadin.flow.server.frontend.FrontendUtils
import com.vaadin.flow.server.frontend.installer.NodeInstaller
import com.vaadin.flow.server.frontend.installer.Platform
import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier

public abstract class VaadinFlowPluginExtension @Inject constructor(private val project: Project) {
    /**
     * Whether we are running in productionMode or not. Defaults to false.
     * Responds to the `-Pvaadin.productionMode` property.
     */
    public abstract val productionMode: Property<Boolean>

    /**
     * The folder where the frontend build tool should output index.js and other generated
     * files. Defaults to `null` which will use the auto-detected value of
     * resoucesDir of the main SourceSet, usually `build/resources/main/META-INF/VAADIN/webapp/`.
     */
    @Deprecated(
        "use frontendOutputDirectory instead",
        replaceWith = ReplaceWith("frontendOutputDirectory")
    )
    public abstract val webpackOutputDirectory: Property<File>

    /**
     * The folder where the frontend build tool should output index.js and other generated
     * files. Defaults to `null` which will use the auto-detected value of
     * resoucesDir of the main SourceSet, usually `build/resources/main/META-INF/VAADIN/webapp/`.
     */
    public abstract val frontendOutputDirectory: Property<File>

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    public abstract val npmFolder: Property<File>

    /**
     * A directory with project's frontend source files.
     *
     * Defaults to `frontend`
     */
    public abstract val frontendDirectory: Property<File>

    /**
     * Whether to generate a bundle from the project frontend sources or not. Defaults to true.
     */
    public abstract val generateBundle: Property<Boolean>

    /**
     * Whether to run `npm install` after updating dependencies. Defaults to true.
     */
    public abstract val runNpmInstall: Property<Boolean>

    /**
     * Whether to generate embeddable web components from WebComponentExporter
     * inheritors. Defaults to true.
     */
    public abstract val generateEmbeddableWebComponents: Property<Boolean>

    /**
     * Defines the project frontend directory from where resources should be
     * copied from for use with webpack. Defaults to [Constants.LOCAL_FRONTEND_RESOURCES_PATH]
     */
    public abstract val frontendResourcesDirectory: Property<File>

    /**
     * Whether to use byte code scanner strategy to discover frontend
     * components. Defaults to true.
     */
    public abstract val optimizeBundle: Property<Boolean>

    /**
     * Instructs to use pnpm for installing npm frontend resources. Default is [Constants.ENABLE_PNPM_DEFAULT]
     *
     * pnpm, a.k.a. performant npm, is a better front-end dependency management option.
     * With pnpm, packages are cached locally by default and linked (instead of
     * downloaded) for every project. This results in reduced disk space usage
     * and faster recurring builds when compared to npm.
     */
    public abstract val pnpmEnable: Property<Boolean>

    /**
     * Instructs to use bun for installing npm frontend resources. Default is false.
     *
     * bun, is a better front-end dependency management option.
     * With bun, packages are cached locally by default and linked (instead of
     * downloaded) for every project. This results in reduced disk space usage
     * and faster recurring builds when compared to npm.
     */
    public abstract val bunEnable: Property<Boolean>

    /**
     * Whether the globally installed pnpm tool is used. By default, the
     * pinned supported version of pnpm is used, see
     * [FrontendTools.DEFAULT_PNPM_VERSION].
     */
    public abstract val useGlobalPnpm: Property<Boolean>

    /**
     * Whether vaadin home node executable usage is forced. If it's set to
     * `true` then vaadin home 'node' is checked and installed if it's
     * absent. Then it will be used instead of globally 'node' or locally
     * installed installed 'node'.
     *
     * Defaults to false.
     */
    public abstract val requireHomeNodeExec: Property<Boolean>

    /**
     * Whether or not insert the initial Uidl object in the bootstrap index.html. Defaults to false.
     * Responds to the `-Pvaadin.eagerServerLoad` property.
     */
    public abstract val eagerServerLoad: Property<Boolean>

    /**
     * Application properties file in Spring project.
     * Defaults to `src/main/resources/application.properties`
     */
    public abstract val applicationProperties: Property<File>

    /**
     * Default generated path of the OpenAPI json.
     *
     * Defaults to `generated-resources/openapi.json`.
     */
    public abstract val openApiJsonFile: Property<File>

    /**
     * Java source folders for connect scanning.
     */
    public abstract val javaSourceFolder: Property<File>

    /**
     * Java resource folder.
     */
    public abstract val javaResourceFolder: Property<File>

    /**
     * The folder where flow will put TS API files for client projects.
     */
    public abstract val generatedTsFolder: Property<File>

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example `"v16.0.0"`. Defaults to [FrontendTools.DEFAULT_NODE_VERSION].
     */
    public abstract val nodeVersion: Property<String>

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to [NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT].
     *
     * Example: `"https://nodejs.org/dist/"`.
     */
    public abstract val nodeDownloadRoot: Property<String>

    /**
     * Allow automatic update of node installed to alternate location. Default `false`
     */
    public abstract val nodeAutoUpdate: Property<Boolean>

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
    public abstract val resourceOutputDirectory: Property<File>

    /**
     * Defines the output folder used by the project.
     *
     * Default value is the `project.buildDir` and should not need to be changed.
     */
    public abstract val projectBuildDir: Property<String>

    /**
     * Defines the npm packages to run postinstall for.
     */
    public abstract val postinstallPackages: ListProperty<String>

    public val classpathFilter: ClasspathFilter = ClasspathFilter()

    /**
     * The name of the SourceSet to scan for Vaadin components - i.e. the classes that are annoated with
     * Vaadin annotations.
     *
     * Defaults to `"main"`
     */
    public abstract val sourceSetName: Property<String>

    /**
     * The Gradle scope the Vaadin dependencies have been added to. Defaults to 'runtimeClasspath' if
     * no sourceSetName has been specified, or '<code>sourceSetName</code>RuntimeClasspath' if a non-main sourceset
     * has been set.
     */
    public abstract val dependencyScope: Property<String>

    /**
     * The Gradle task that the `vaadinPrepareFrontend` task must run before. The target task should run before
     * or be the task that copies the files from the resources directories of the specified SourceSet to the relevant
     * output directory for that SourceSet. Defaults to 'processResources' if no sourceSetName has been specified, or
     * 'process<code>SourceSetName</code>Resources' if a non-main sourceset has been specified.
     */
    public abstract val processResourcesTaskName: Property<String>

    /**
     * Parameter to control if frontend development server should be used in
     * development mode or not.
     *
     * Defaults to false.
     */
    public abstract val frontendHotdeploy: Property<Boolean>

    /**
     * Setting this to true will run {@code npm ci} instead of {@code npm install} when using npm.
     *
     * If using pnpm, the install will be run with {@code --frozen-lockfile} parameter.
     *
     * This makes sure that the versions in package lock file will not be overwritten and production builds are reproducible.
     */
    public abstract val ciBuild: Property<Boolean>

    /**
     * Enable skip of dev bundle rebuild if a dev bundle exists. Defaults to false.
     * @return `true` to skip dev bundle rebuild
     */
    public abstract val skipDevBundleBuild: Property<Boolean>

    /**
     * Setting this to `true` will force a build of the production build
     * even if there is a default production bundle that could be used.
     *
     * Created production bundle optimization is defined by
     * [optimizeBundle] parameter.
     *
     * Defaults to `false`.
     */
    public abstract val forceProductionBuild: Property<Boolean>

    /**
     * Prevents tracking state of the `vaadinPrepareFrontend` task, so that it
     * will re-run every time it is called.
     *
     * Setting this to `true` allows to always execute `vaadinPrepareFrontend`.
     *
     * Defaults to `false`, meaning that the task execution is skipped when its
     * outcomes are up-to-date, improving the overall build time.
     */
    public abstract val alwaysExecutePrepareFrontend: Property<Boolean>

    public abstract val reactEnable: Property<Boolean>

    public abstract val cleanFrontendFiles: Property<Boolean>

    public abstract val applicationIdentifier: Property<String>

    /**
     * The list of extra file extensions that are considered project files.
     * Hashes are calculated for these files as part of detecting if a new
     * bundle should be generated.
     */
    public abstract val frontendExtraFileExtensions: ListProperty<String>

    /**
     * Whether to exclude Vaadin web component npm packages in packages.json
     */
    public abstract val npmExcludeWebComponents: Property<Boolean>

    /**
     * Whether to ignore node/npm tool version checks or not. Defaults to
     * {@code false}.
     */
    public abstract val frontendIgnoreVersionChecks: Property<Boolean>

    /**
     * Allows building a version of the application with a commercial banner
     * when commercial components are used without a license key.
     */
    public abstract val commercialWithBanner: Property<Boolean>

    public fun filterClasspath(
        @DelegatesTo(
            value = ClasspathFilter::class,
            strategy = Closure.DELEGATE_FIRST
        ) block: Closure<*>
    ) {
        block.delegate = classpathFilter
        block.resolveStrategy = Closure.DELEGATE_FIRST
        block.call()
    }

    public fun filterClasspath(block: Action<ClasspathFilter>) {
        block.execute(classpathFilter)
    }

    public val effective: PluginEffectiveConfiguration
        get() = PluginEffectiveConfiguration.get(project)

    public companion object {
        public fun get(project: Project): VaadinFlowPluginExtension =
            project.extensions.getByType(VaadinFlowPluginExtension::class.java)
    }
}

public class PluginEffectiveConfiguration(
    project: Project,
    extension: VaadinFlowPluginExtension
) : Serializable {

    internal val projectDir = project.projectDir
    internal val projectName = project.name

    public val productionMode: Provider<Boolean> = extension.productionMode
        .convention(false)
        .overrideWithSystemPropertyFlag(project, "vaadin.productionMode")

    public val sourceSetName: Property<String> = extension.sourceSetName
        .convention("main")

    public val dependencyScope: Property<String> = extension.dependencyScope
        .convention(sourceSetName.map {
            if (it == "main") {
                "runtimeClasspath"
            } else {
                "${it}RuntimeClasspath"
            }
        })


    internal val hillaAvailable: Provider<Boolean> =
        project.configurations.getByName(dependencyScope.get())
            .incoming.artifacts.resolvedArtifacts
            .map { result ->
                result.filter {
                    it.id is ModuleComponentArtifactIdentifier && it.id.componentIdentifier is ModuleComponentIdentifier
                }.map {
                    (it.id.componentIdentifier as ModuleComponentIdentifier).moduleIdentifier
                }.any {
                    it.group == "com.vaadin" && it.name == "hilla-endpoint"
                }
            }


    public val frontendOutputDirectory: Provider<File> =
        extension.frontendOutputDirectory.convention(
            extension.webpackOutputDirectory
                .convention(
                    sourceSetName.map {
                        File(
                            project.getBuildResourcesDir(it),
                            Constants.VAADIN_WEBAPP_RESOURCES
                        )
                    }
                )
        )

    public val npmFolder: Provider<File> = extension.npmFolder
        .convention(project.projectDir)

    public val frontendDirectory: Provider<File> = extension.frontendDirectory
        .convention(
            File(
                project.projectDir,
                FrontendUtils.DEFAULT_FRONTEND_DIR
            )
        )

    // Replacement for BuildFrontendUtil.getFrontendDirectory(adapter)
    // to avoid circular dependencies between PluginEffectiveConfiguration
    // and GradlePluginAdapter
    public val effectiveFrontendDirectory: Provider<File> =
        npmFolder.zip(frontendDirectory) { npmFolder, frontendDirectory ->
            FrontendUtils.getFrontendFolder(
                npmFolder,
                frontendDirectory
            )
        }

    public val generateBundle: Provider<Boolean> = extension.generateBundle
        .convention(true)

    public val runNpmInstall: Provider<Boolean> = extension.runNpmInstall
        .convention(true)

    public val generateEmbeddableWebComponents: Provider<Boolean> =
        extension.generateEmbeddableWebComponents
            .convention(true)

    public val frontendResourcesDirectory: Property<File> =
        extension.frontendResourcesDirectory
            .convention(
                File(
                    project.projectDir,
                    Constants.LOCAL_FRONTEND_RESOURCES_PATH
                )
            )

    public val optimizeBundle: Property<Boolean> = extension.optimizeBundle
        .convention(true)

    public val pnpmEnable: Provider<Boolean> = extension.pnpmEnable
        .convention(Constants.ENABLE_PNPM_DEFAULT)
        .overrideWithSystemPropertyFlag(
            project,
            InitParameters.SERVLET_PARAMETER_ENABLE_PNPM
        )

    public val bunEnable: Provider<Boolean> = extension.bunEnable
        .convention(Constants.ENABLE_BUN_DEFAULT)
        .overrideWithSystemPropertyFlag(
            project,
            InitParameters.SERVLET_PARAMETER_ENABLE_BUN
        )

    public val useGlobalPnpm: Provider<Boolean> = extension.useGlobalPnpm
        .convention(Constants.GLOBAL_PNPM_DEFAULT)
        .overrideWithSystemPropertyFlag(
            project,
            InitParameters.SERVLET_PARAMETER_GLOBAL_PNPM
        )

    public val requireHomeNodeExec: Property<Boolean> =
        extension.requireHomeNodeExec
            .convention(false)

    public val eagerServerLoad: Provider<Boolean> = extension.eagerServerLoad
        .convention(false)
        .overrideWithSystemPropertyFlag(project, "vaadin.eagerServerLoad")

    public val applicationProperties: Property<File> =
        extension.applicationProperties
            .convention(
                File(
                    project.projectDir,
                    "src/main/resources/application.properties"
                )
            )

    public val openApiJsonFile: Property<File> = extension.openApiJsonFile
        .convention(
            project.layout.buildDirectory.file("generated-resources/openapi.json")
                .asFile()
        )

    public val javaSourceFolder: Property<File> = extension.javaSourceFolder
        .convention(File(project.projectDir, "src/main/java"))

    public val javaResourceFolder: Property<File> = extension.javaResourceFolder
        .convention(File(project.projectDir, "src/main/resources"))

    public val generatedTsFolder: Property<File> = extension.generatedTsFolder
        .convention(frontendDirectory.map { File(it, FrontendUtils.GENERATED) })

    public val nodeVersion: Property<String> = extension.nodeVersion
        .convention(FrontendTools.DEFAULT_NODE_VERSION)

    public val nodeDownloadRoot: Property<String> = extension.nodeDownloadRoot
        .convention(Platform.guess().nodeDownloadRoot)

    public val nodeAutoUpdate: Property<Boolean> = extension.nodeAutoUpdate
        .convention(false)

    public val resourceOutputDirectory: Property<File> =
        extension.resourceOutputDirectory
            .convention(
                project.layout.buildDirectory.dir("vaadin-generated").asFile()
            )

    public val projectBuildDir: Property<String> = extension.projectBuildDir
        .convention(project.layout.buildDirectory.map { it.asFile.toString() })

    public val postinstallPackages: ListProperty<String> =
        extension.postinstallPackages
            .convention(listOf())

    public val classpathFilter: ClasspathFilter = extension.classpathFilter

    public val processResourcesTaskName: Property<String> =
        extension.processResourcesTaskName
            .convention(sourceSetName.map {
                if (it == "main") {
                    "processResources"
                } else {
                    "process${it.replaceFirstChar(Char::titlecase)}Resources"
                }
            })

    public val frontendHotdeploy: Provider<Boolean> =
        extension.frontendHotdeploy
            .convention(
                effectiveFrontendDirectory.zip(
                    hillaAvailable
                ) { frontendDirectory, hasHilla ->
                    hasHilla &&
                            FrontendUtils.isHillaViewsUsed(frontendDirectory)
                }
            )
            .overrideWithSystemPropertyFlag(
                project,
                InitParameters.FRONTEND_HOTDEPLOY
            )

    public val ciBuild: Provider<Boolean> = extension.ciBuild
        .convention(false)
        .overrideWithSystemPropertyFlag(project, InitParameters.CI_BUILD)

    public val skipDevBundleBuild: Property<Boolean> =
        extension.skipDevBundleBuild
            .convention(false)

    public val forceProductionBuild: Provider<Boolean> =
        extension.forceProductionBuild
            .convention(false)
            .overrideWithSystemPropertyFlag(
                project,
                InitParameters.FORCE_PRODUCTION_BUILD
            )

    public val alwaysExecutePrepareFrontend: Property<Boolean> =
        extension.alwaysExecutePrepareFrontend
            .convention(false)

    public val reactEnable: Provider<Boolean> = extension.reactEnable
        .convention(effectiveFrontendDirectory.map {
            FrontendUtils.isReactRouterRequired(it)
        })
        .overrideWithSystemPropertyFlag(project, InitParameters.REACT_ENABLE)

    public val cleanFrontendFiles: Property<Boolean> =
        extension.cleanFrontendFiles
            .convention(true)

    public val applicationIdentifier: Provider<String> =
        extension.applicationIdentifier
            .convention(
                "app-" + StringUtil.getHash(
                    project.name,
                    java.nio.charset.StandardCharsets.UTF_8
                )
            )
            .overrideWithSystemProperty(
                project,
                "vaadin.${InitParameters.APPLICATION_IDENTIFIER}"
            )

    // TODO: Possibly get value from system param InitParameters.FRONTEND_EXTRA_EXTENSIONS
    public val frontendExtraFileExtensions: ListProperty<String> =
        extension.frontendExtraFileExtensions
            .convention(listOf())

    public val frontendIgnoreVersionChecks: Provider<Boolean> = extension
        .frontendIgnoreVersionChecks.convention(false)
        .overrideWithSystemPropertyFlag(
            project,
            FrontendUtils.PARAM_IGNORE_VERSION_CHECKS
        )

    public val npmExcludeWebComponents: Provider<Boolean> = extension
        .npmExcludeWebComponents.convention(false)

    public val commercialWithBanner: Provider<Boolean> =
        extension.commercialWithBanner.convention(false)
            .overrideWithSystemPropertyFlag(
                project,
                "vaadin.${InitParameters.COMMERCIAL_WITH_BANNER}"
            )

    public val toolsSettings: Provider<FrontendToolsSettings> = npmFolder.map {
        FrontendToolsSettings(it.absolutePath) {
            FrontendUtils.getVaadinHomeDirectory()
                .absolutePath
        }
    }

    /**
     * Finds the value of a boolean property. It searches in gradle and system properties.
     *
     * If the property is defined in both gradle and system properties, then the system property is taken.
     *
     * @param propertyName the property name
     * @return a new provider of the value, which either takes the original value if the system/gradle property is not present,
     * `true` if it's defined or if it's set to "true" and `false` otherwise.
     */
    private fun Provider<Boolean>.overrideWithSystemPropertyFlag(
        project: Project,
        propertyName: String
    ): Provider<Boolean> =
        project.getBooleanProperty(propertyName).orElse(this)

    /**
     * Finds the value of a string property. It searches in gradle and system properties.
     *
     * If the property is defined in both gradle and system properties, then the system property is taken.
     *
     * @param propertyName the property name
     * @return a new provider of the value, which either takes the original value if the system/gradle property is not present.
     */
    private fun Provider<String>.overrideWithSystemProperty(
        project: Project,
        propertyName: String
    ): Provider<String> =
        project.getStringProperty(propertyName).orElse(this)

    override fun toString(): String = "PluginEffectiveConfiguration(" +
            "productionMode=${productionMode.get()}, " +
            "applicationIdentifier=${applicationIdentifier.get()}, " +
            "frontendOutputDirectory=${frontendOutputDirectory.get()}, " +
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
            "frontendHotdeploy=${frontendHotdeploy.get()}," +
            "reactEnable=${reactEnable.get()}," +
            "cleanFrontendFiles=${cleanFrontendFiles.get()}," +
            "frontendExtraFileExtensions=${frontendExtraFileExtensions.get()}," +
            "npmExcludeWebComponents=${npmExcludeWebComponents.get()}" +
            "commercialWithBanner=${commercialWithBanner.get()}" +
            ")"

    public companion object {
        public fun get(project: Project): PluginEffectiveConfiguration =
            PluginEffectiveConfiguration(
                project,
                VaadinFlowPluginExtension.get(project)
            )

    }
}
