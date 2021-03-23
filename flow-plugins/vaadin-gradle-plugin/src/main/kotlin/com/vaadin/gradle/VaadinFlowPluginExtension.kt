/**
 *    Copyright 2000-2020 Vaadin Ltd
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
import com.vaadin.flow.server.frontend.FrontendTools
import com.vaadin.flow.server.frontend.FrontendUtils
import com.vaadin.flow.server.frontend.installer.NodeInstaller
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
     * Copy the `webapp.config.js` from the specified URL if missing. Default is
     * the template provided by this plugin. Set it to empty string to disable
     * the feature.
     */
    public var webpackTemplate: String = FrontendUtils.WEBPACK_CONFIG
    /**
     * Copy the `webapp.generated.js` from the specified URL. Default is the
     * template provided by this plugin. Set it to empty string to disable the
     * feature.
     */
    public var webpackGeneratedTemplate: String = FrontendUtils.WEBPACK_GENERATED
    /**
     * The folder where flow will put generated files that will be used by
     * webpack.
     *
     * @todo mavi we should move this to `build/frontend/` but in order to do that we need Flow 2.2 or higher. Leaving as-is for now.
     */
    public var generatedFolder: File = File(project.projectDir, "target/frontend")
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
     * Instructs to use pnpm for installing npm frontend resources. Default is [Constants.ENABLE_PNPM_DEFAULT_STRING]
     * Responds to the `-Pvaadin.useDeprecatedV14Bootstrapping` property.
     *
     * pnpm, a.k.a. performant npm, is a better front-end dependency management option.
     * With pnpm, packages are cached locally by default and linked (instead of
     * downloaded) for every project. This results in reduced disk space usage
     * and faster recurring builds when compared to npm.
     */
    public var pnpmEnable: Boolean = Constants.ENABLE_PNPM_DEFAULT_STRING.toBoolean()

    /**
     * Whether vaadin home node executable usage is forced. If it's set to
     * `true` then vaadin home 'node' is checked and installed if it's
     * absent. Then it will be used instead of globally 'node' or locally
     * installed installed 'node'.
     */
    public var requireHomeNodeExec: Boolean = false

    /**
     * Whether or not we are running in legacy V14 bootstrap mode. Defaults to false.
     * Responds to the `-Pvaadin.useDeprecatedV14Bootstrapping` property.
     */
    public var useDeprecatedV14Bootstrapping: Boolean = false

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
     * The folder where flow will put TS API files for client projects.
     */
    public var generatedTsFolder: File = File(project.projectDir, "frontend/generated")

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example `"v12.16.0"`. Defaults to [FrontendTools.DEFAULT_NODE_VERSION].
     */
    public var nodeVersion: String = FrontendTools.DEFAULT_NODE_VERSION

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to [NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT].
     *
     * Example: `"https://nodejs.org/dist/"`.
     */
    public var nodeDownloadRoot: String = NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT

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

        val useDeprecatedV14BootstrappingProperty: Boolean? = project.getBooleanProperty("vaadin.useDeprecatedV14Bootstrapping")
        if (useDeprecatedV14BootstrappingProperty != null) {
            useDeprecatedV14Bootstrapping = useDeprecatedV14BootstrappingProperty
        }

        val pnpmEnableProperty: Boolean? = project.getBooleanProperty(Constants.SERVLET_PARAMETER_ENABLE_PNPM)
        if (pnpmEnableProperty != null) {
            pnpmEnable = pnpmEnableProperty
        }
    }

    override fun toString(): String = "VaadinFlowPluginExtension(" +
            "productionMode=$productionMode, " +
            "webpackOutputDirectory=$webpackOutputDirectory, " +
            "npmFolder=$npmFolder, " +
            "webpackTemplate='$webpackTemplate', " +
            "webpackGeneratedTemplate='$webpackGeneratedTemplate', " +
            "generatedFolder=$generatedFolder, " +
            "frontendDirectory=$frontendDirectory, " +
            "generateBundle=$generateBundle, " +
            "runNpmInstall=$runNpmInstall, " +
            "generateEmbeddableWebComponents=$generateEmbeddableWebComponents, " +
            "frontendResourcesDirectory=$frontendResourcesDirectory, " +
            "optimizeBundle=$optimizeBundle, " +
            "pnpmEnable=$pnpmEnable, " +
            "requireHomeNodeExec=$requireHomeNodeExec, " +
            "useDeprecatedV14Bootstrapping=$useDeprecatedV14Bootstrapping, " +
            "eagerServerLoad=$eagerServerLoad, " +
            "applicationProperties=$applicationProperties, " +
            "openApiJsonFile=$openApiJsonFile, " +
            "javaSourceFolder=$javaSourceFolder, " +
            "generatedTsFolder=$generatedTsFolder, " +
            "nodeVersion=$nodeVersion, " +
            "nodeDownloadRoot=$nodeDownloadRoot, " +
            "resourceOutputDirectory=$resourceOutputDirectory" +
            ")"
}

internal val Project.buildResourcesDir: File get() {
    val sourceSets: SourceSetContainer = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
    return sourceSets.getByName("main").output.resourcesDir!!
}
