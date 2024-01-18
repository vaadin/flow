/**
 *    Copyright 2000-2024 Vaadin Ltd
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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.net.URI
import com.vaadin.experimental.FeatureFlags
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider

/**
 * Declaratively defines the inputs of the [VaadinPrepareFrontendTask]:
 * configurable parameters, Node.js and npm/pnpm versions. Being used for
 * caching the results of vaadinPrepareFrontend task to not run it again if
 * inputs are the same.
 */
internal class PrepareFrontendInputProperties(private val config: PluginEffectiveConfiguration) {
    private val tools: Provider<FrontendTools> = initialiseFrontendToolsSettings()

    @Input
    public fun getProductionMode(): Provider<Boolean> = config.productionMode

    @Input
    @Optional
    public fun getWebpackOutputDirectory(): Provider<String> = config.webpackOutputDirectory
        .filterExists()
        .absolutePath

    @Input
    public fun getNpmFolder(): Provider<String> = config.npmFolder.absolutePath

    @Input
    public fun getFrontendDirectory(): Provider<String> = config.frontendDirectory.absolutePath

    @Input
    public fun getGenerateBundle(): Provider<Boolean> = config.generateBundle

    @Input
    public fun getRunNpmInstall(): Provider<Boolean> = config.runNpmInstall

    @Input
    public fun getGenerateEmbeddableWebComponent(): Provider<Boolean> = config.generateEmbeddableWebComponents

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public fun getFrontendResourcesDirectory(): Provider<File> = config.frontendResourcesDirectory.filterExists()

    @Input
    public fun getOptimizeBundle(): Provider<Boolean> = config.optimizeBundle

    @Input
    public fun getPnpmEnable(): Provider<Boolean> = config.pnpmEnable

    @Input
    public fun getUseGlobalPnpm(): Provider<Boolean> = config.useGlobalPnpm

    @Input
    public fun getRequireHomeNodeExec(): Provider<Boolean> = config.requireHomeNodeExec

    @Input
    public fun getEagerServerLoad(): Provider<Boolean> = config.eagerServerLoad

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public fun getApplicationProperties(): Provider<File> = config.applicationProperties.filterExists()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public fun getOpenApiJsonFile(): Provider<File> = config.openApiJsonFile.filterExists()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public fun getFeatureFlagsFile(): Provider<File> = config.javaResourceFolder
        .map { it.resolve(FeatureFlags.PROPERTIES_FILENAME) }
        .filterExists()

    @Input
    public fun getJavaSourceFolder(): Provider<String> = config.javaSourceFolder.absolutePath

    @Input
    public fun getJavaResourceFolder(): Provider<String> = config.javaResourceFolder.absolutePath

    @Input
    public fun getGeneratedTsFolder(): Provider<String> = config.generatedTsFolder.absolutePath

    @Input
    public fun getNodeVersion(): Provider<String> = config.nodeVersion

    @Input
    public fun getNodeDownloadRoot(): Provider<String> = config.nodeDownloadRoot

    @Input
    public fun getNodeAutoUpdate(): Provider<Boolean> = config.nodeAutoUpdate

    @Input
    public fun getProjectBuildDir(): Provider<String> = config.projectBuildDir

    @Input
    public fun getPostInstallPackages(): ListProperty<String> = config.postinstallPackages

    @Input
    public fun getFrontendHotdeploy(): Provider<Boolean> = config.frontendHotdeploy

    @Input
    public fun getCiBuild(): Provider<Boolean> = config.ciBuild

    @Input
    public fun getSkipDevBundleBuild(): Provider<Boolean> = config.skipDevBundleBuild

    @Input
    public fun getForceProductionBuild(): Provider<Boolean> = config.forceProductionBuild

    @Input
    public fun geReactRouterEnabled(): Provider<Boolean> = config.reactRouterEnabled

    @Input
    @Optional
    public fun getNodeExecutablePath(): Provider<String> = tools
        .mapOrNull { it.nodeBinary }
        .filterExists()

    @Input
    @Optional
    public fun getNpmExecutablePath(): Provider<String> = tools.map { tools ->
        val npmExecutable = tools.npmExecutable ?: listOf()
        npmExecutable.joinToString(" ")
    }

    @Input
    @Optional
    public fun getPnpmExecutablePath(): Provider<String> = config.pnpmEnable.map { pnpmEnable: Boolean ->
        if (!pnpmEnable) {
            return@map ""
        }
        val pnpmExecutable = tools.get().pnpmExecutable ?: listOf()
        pnpmExecutable.joinToString(" ")
    }

    private fun initialiseFrontendToolsSettings(): Provider<FrontendTools> = config.npmFolder.map { npmFolder: File ->
        val settings = FrontendToolsSettings(npmFolder.absolutePath) {
            FrontendUtils.getVaadinHomeDirectory()
                .absolutePath
        }
        settings.nodeDownloadRoot = URI(config.nodeDownloadRoot.get())
        settings.isForceAlternativeNode = config.requireHomeNodeExec.get()
        settings.isUseGlobalPnpm = config.useGlobalPnpm.get()
        settings.isAutoUpdate = config.nodeAutoUpdate.get()
        FrontendTools(settings)
    }
}
