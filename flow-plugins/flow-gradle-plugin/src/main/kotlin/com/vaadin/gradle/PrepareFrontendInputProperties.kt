/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.gradle

import java.io.File
import com.vaadin.experimental.FeatureFlags
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Declaratively defines the inputs of the [VaadinPrepareFrontendTask]:
 * configurable parameters, Node.js and npm/pnpm versions. Being used for
 * caching the results of vaadinPrepareFrontend task to not run it again if
 * inputs are the same.
 */
internal class PrepareFrontendInputProperties(
    adapter: GradlePluginAdapter,
    private val toolsService: FrontendToolService
) {

    private val config = adapter.config

    @Input
    fun getProductionMode(): Provider<Boolean> = config.productionMode

    @Input
    @Optional
    fun getFrontendOutputDirectory(): Provider<String> =
        config.frontendOutputDirectory
            .filterExists()
            .absolutePath

    @Input
    fun getNpmFolder(): Provider<String> = config.npmFolder.absolutePath

    @Input
    fun getFrontendDirectory(): Provider<String> =
        config.frontendDirectory.absolutePath

    @Input
    fun getGenerateBundle(): Provider<Boolean> = config.generateBundle

    @Input
    fun getRunNpmInstall(): Provider<Boolean> = config.runNpmInstall

    @Input
    fun getGenerateEmbeddableWebComponent(): Provider<Boolean> =
        config.generateEmbeddableWebComponents

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    fun getFrontendResourcesDirectory(): Provider<File> =
        config.frontendResourcesDirectory.filterExists()

    @Input
    fun getOptimizeBundle(): Provider<Boolean> = config.optimizeBundle

    @Input
    fun getPnpmEnable(): Provider<Boolean> = config.pnpmEnable

    @Input
    fun getUseGlobalPnpm(): Provider<Boolean> = config.useGlobalPnpm

    @Input
    fun getRequireHomeNodeExec(): Provider<Boolean> =
        config.requireHomeNodeExec

    @Input
    fun getEagerServerLoad(): Provider<Boolean> = config.eagerServerLoad

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    fun getApplicationProperties(): Provider<File> =
        config.applicationProperties.filterExists()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    fun getOpenApiJsonFile(): Provider<File> =
        config.openApiJsonFile.filterExists()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    fun getFeatureFlagsFile(): Provider<File> = config.javaResourceFolder
        .map { it.resolve(FeatureFlags.PROPERTIES_FILENAME) }
        .filterExists()

    @Input
    fun getJavaSourceFolder(): Provider<String> =
        config.javaSourceFolder.absolutePath

    @Input
    fun getJavaResourceFolder(): Provider<String> =
        config.javaResourceFolder.absolutePath

    @Input
    fun getGeneratedTsFolder(): Provider<String> =
        config.generatedTsFolder.absolutePath

    @Input
    fun getNodeVersion(): Provider<String> = config.nodeVersion

    @Input
    fun getNodeDownloadRoot(): Provider<String> = config.nodeDownloadRoot

    @Input
    fun getNodeAutoUpdate(): Provider<Boolean> = config.nodeAutoUpdate

    @Input
    fun getProjectBuildDir(): Provider<String> = config.projectBuildDir

    @Input
    fun getPostInstallPackages(): ListProperty<String> =
        config.postinstallPackages

    @Input
    fun getFrontendHotdeploy(): Provider<Boolean> = config.frontendHotdeploy

    @Input
    fun getCiBuild(): Provider<Boolean> = config.ciBuild

    @Input
    fun getSkipDevBundleBuild(): Provider<Boolean> =
        config.skipDevBundleBuild

    @Input
    fun getForceProductionBuild(): Provider<Boolean> =
        config.forceProductionBuild

    @Input
    fun getReactEnable(): Provider<Boolean> = config.reactEnable

    @Input
    fun getFrontendExtraFileExtensions(): ListProperty<String> =
        config.frontendExtraFileExtensions

    @Input
    fun getApplicationIdentifier(): Provider<String> =
        config.applicationIdentifier

    @Input
    fun getNpmExcludeWebComponents(): Provider<Boolean> =
        config.npmExcludeWebComponents

    @Input
    fun getFrontendIgnoreVersionChecks(): Provider<Boolean> =
        config.frontendIgnoreVersionChecks

    @Input
    @Optional
    fun getNodeExecutablePath(): Provider<String> =
        toolsService.toolsProperty { it.nodeBinary }
            .filterExists()

    @Input
    @Optional
    fun getNpmExecutablePath(): Provider<String> =
        toolsService.toolsProperty { tools ->
            val npmExecutable = tools.npmExecutable ?: listOf()
            npmExecutable.joinToString(" ")
        }


    @Input
    @Optional
    fun getPnpmExecutablePath(): Provider<String> =
        config.pnpmEnable.filterBy {
            it
        }.flatMap {
            toolsService.toolsProperty { tools ->
                val pnpmExecutable = tools.pnpmExecutable ?: listOf()
                pnpmExecutable.joinToString(" ")
            }
        }.orElse("")

}
