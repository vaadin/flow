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

import com.vaadin.flow.server.frontend.FrontendTools
import com.vaadin.flow.server.frontend.FrontendToolsSettings
import com.vaadin.flow.server.frontend.FrontendUtils
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.net.URI
import java.util.stream.Collectors
import com.vaadin.experimental.FeatureFlags

/**
 * Declaratively defines the inputs of the [VaadinPrepareFrontendTask]:
 * configurable parameters, Node.js and npm/pnpm versions. Being used for
 * caching the results of vaadinPrepareFrontend task to not run it again if
 * inputs are the same.
 */
public class PrepareFrontendInputProperties public constructor(project: Project) {

    private var extension: VaadinFlowPluginExtension

    private val tools: FrontendTools

    init {
        extension = VaadinFlowPluginExtension.get(project)
        tools = initialiseFrontendToolsSettings()
    }

    @Input
    public fun getProductionMode(): Boolean {
        return extension.productionMode
    }

    @Input
    @Optional
    public fun getWebpackOutputDirectory(): String? {
        val webpackOutputDirectory = extension.webpackOutputDirectory
        if (webpackOutputDirectory != null && !webpackOutputDirectory.exists()) {
            return null
        } else if (webpackOutputDirectory == null) {
            return null
        }
        return webpackOutputDirectory.absolutePath
    }

    @Input
    public fun getNpmFolder(): String {
        return extension.npmFolder.absolutePath
    }

    @Input
    public fun getFrontendDirectory(): String {
        return extension.frontendDirectory.absolutePath
    }

    @Input
    public fun getGenerateBundle(): Boolean {
        return extension.generateBundle
    }

    @Input
    public fun getRunNpmInstall(): Boolean {
        return extension.runNpmInstall
    }

    @Input
    public fun getGenerateEmbeddableWebComponent(): Boolean {
        return extension.generateEmbeddableWebComponents
    }

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public fun getFrontendResourcesDirectory(): File? {
        val frontendResourcesDirectory = extension.frontendResourcesDirectory
        if (!frontendResourcesDirectory.exists()) {
            return null
        }
        return frontendResourcesDirectory
    }

    @Input
    public fun getOptimizeBundle(): Boolean {
        return extension.optimizeBundle
    }

    @Input
    public fun getPnpmEnable(): Boolean {
        return extension.pnpmEnable
    }

    @Input
    public  fun getUseGlobalPnpm(): Boolean {
        return extension.useGlobalPnpm
    }

    @Input
    public fun getRequireHomeNodeExec(): Boolean {
        return extension.requireHomeNodeExec
    }

    @Input
    public fun getEagerServerLoad(): Boolean {
        return extension.eagerServerLoad
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public fun getApplicationProperties(): File? {
        val applicationProperties = extension.applicationProperties
        if (!applicationProperties.exists()) {
            return null
        }
        return applicationProperties
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public fun getOpenApiJsonFile(): File? {
        val openApiJsonFile = extension.openApiJsonFile
        if (!openApiJsonFile.exists()) {
            return null
        }
        return openApiJsonFile
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public fun getFeatureFlagsFile(): File? {
        val featureFlagsFile = extension.javaResourceFolder.resolve(FeatureFlags.PROPERTIES_FILENAME)
        if (!featureFlagsFile.exists()) {
            return null
        }
        return featureFlagsFile
    }

    @Input
    public fun getJavaSourceFolder(): String {
        return extension.javaSourceFolder.absolutePath
    }

    @Input
    public fun getJavaResourceFolder(): String {
        return extension.javaResourceFolder.absolutePath
    }

    @Input
    public fun getGeneratedTsFolder(): String {
        return extension.generatedTsFolder.absolutePath
    }

    @Input
    public fun getNodeVersion(): String {
        return extension.nodeVersion
    }

    @Input
    public fun getNodeDownloadRoot(): String {
        return extension.nodeDownloadRoot
    }

    @Input
    public fun getNodeAutoUpdate(): Boolean {
        return extension.nodeAutoUpdate
    }

    @Input
    public fun getProjectBuildDir(): String {
        return extension.projectBuildDir
    }

    @Input
    public fun getPostInstallPackages(): List<String> {
        return extension.postinstallPackages
    }

    @Input
    public fun getFrontendHotdeploy(): Boolean {
        return extension.frontendHotdeploy
    }

    @Input
    public fun getCiBuild(): Boolean {
        return extension.ciBuild
    }

    @Input
    public fun getSkipDevBundleBuild(): Boolean {
        return extension.skipDevBundleBuild
    }

    @Input
    public fun getForceProductionBuild(): Boolean {
        return extension.forceProductionBuild
    }

    @Input
    @Optional
    public fun getNodeExecutablePath(): String? {
        val nodeBinary = tools.nodeBinary ?: return null
        val nodeBinaryFile = File(nodeBinary)
        if (!nodeBinaryFile.exists()) {
            return null
        }
        return nodeBinaryFile.absolutePath
    }

    @Input
    @Optional
    public fun getNpmExecutablePath(): String? {
        val npmExecutable = tools.npmExecutable ?: return null
        return npmExecutable.stream()
            .collect(Collectors.joining(" "))
    }

    @Input
    @Optional
    public fun getPnpmExecutablePath(): String? {
        val pnpmExecutable = tools.pnpmExecutable ?: return null
        return pnpmExecutable.stream()
            .collect(Collectors.joining(" "))
    }

    private fun initialiseFrontendToolsSettings(): FrontendTools {
        val settings = FrontendToolsSettings(
            extension.npmFolder.absolutePath
        ) {
            FrontendUtils.getVaadinHomeDirectory()
                .absolutePath
        }
        settings.nodeDownloadRoot = URI(extension.nodeDownloadRoot)
        settings.isForceAlternativeNode = extension.requireHomeNodeExec
        settings.isUseGlobalPnpm = extension.useGlobalPnpm
        settings.isAutoUpdate = extension.nodeAutoUpdate
        return FrontendTools(settings)
    }
}
