/**
 *    Copyright 2000-2026 Vaadin Ltd.
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
import com.vaadin.experimental.FeatureFlags
import com.vaadin.flow.server.Constants
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Declaratively defines the inputs of the [VaadinBuildFrontendTask]:
 * configurable parameters, frontend sources,
 * and package descriptor files. Used for caching the results of
 * vaadinBuildFrontend task to skip re-execution when inputs are unchanged.
 */
internal class BuildFrontendInputProperties(
    adapter: GradlePluginAdapter,
    private val toolsService: FrontendToolService
) {

    private val config = adapter.config

    @Input
    fun getProductionMode(): Provider<Boolean> = config.productionMode

    // The following directory locations are deliberately not task inputs.
    // Hashing their absolute paths into the cache key would make the key
    // path-dependent and break build-cache relocation: a shared cache would
    // miss whenever the project is checked out at a different absolute path.
    // Their relevant content is tracked by relocatable inputs instead
    // (frontendSourceFiles, the package.json/lock files, the @Classpath
    // project classes, and the feature-flags/application.properties files),
    // and vaadinBuildFrontend strips these paths from the production token it
    // caches, so the location never reaches a retained output.
    @Internal
    fun getFrontendOutputDirectory(): Provider<String> =
        config.frontendOutputDirectory
            .absolutePath

    @Internal
    fun getResourcesOutputDirectory(): Provider<String> =
        config.resourcesOutputDirectory
            .absolutePath

    @Internal
    fun getNpmFolder(): Provider<String> = config.npmFolder.absolutePath

    @Internal
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
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getFrontendResourcesDirectory(): Provider<File> =
        config.frontendResourcesDirectory.filterExists()

    @Input
    fun getOptimizeBundle(): Provider<Boolean> = config.optimizeBundle

    @Input
    fun getEagerServerLoad(): Provider<Boolean> = config.eagerServerLoad

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    fun getApplicationProperties(): Provider<File> =
        config.applicationProperties.filterExists()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    fun getOpenApiJsonFile(): Provider<File> =
        config.openApiJsonFile.filterExists()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    fun getFeatureFlagsFile(): Provider<File> = config.javaResourceFolder
        .map { it.resolve(FeatureFlags.PROPERTIES_FILENAME) }
        .filterExists()

    @Internal
    fun getJavaSourceFolder(): Provider<String> =
        config.javaSourceFolder.absolutePath

    @Internal
    fun getJavaResourceFolder(): Provider<String> =
        config.javaResourceFolder.absolutePath

    @Input
    fun getPostInstallPackages(): ListProperty<String> =
        config.postinstallPackages

    @Input
    fun getForceProductionBuild(): Provider<Boolean> =
        config.forceProductionBuild

    @Input
    fun getReactEnable(): Provider<Boolean> = config.reactEnable

    @Input
    fun getFrontendExtraFileExtensions(): ListProperty<String> =
        config.frontendExtraFileExtensions

    @Input
    fun getNpmExcludeWebComponents(): Provider<Boolean> =
        config.npmExcludeWebComponents

    @Input
    fun getCleanFrontendFiles(): Provider<Boolean> =
        config.cleanFrontendFiles

    @Input
    fun getCommercialWithBanner(): Provider<Boolean> =
        config.commercialWithBanner

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    fun getPackageJsonFile(): Provider<File> =
        config.npmFolder.map { File(it, Constants.PACKAGE_JSON) }.filterExists()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    fun getPackageLockJsonFile(): Provider<File> =
        config.npmFolder.map { File(it, Constants.PACKAGE_LOCK_JSON) }.filterExists()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    fun getPackageLockYamlFile(): Provider<File> =
        config.npmFolder.map { File(it, Constants.PACKAGE_LOCK_YAML) }.filterExists()

}
