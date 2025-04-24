/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.net.URI
import java.nio.file.Path
import java.util.function.Consumer
import com.vaadin.flow.plugin.base.BuildFrontendUtil
import com.vaadin.flow.plugin.base.PluginAdapterBuild
import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.frontend.scanner.ClassFinder
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.War
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier

private val servletApiJarRegex =
    Regex(".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$")

internal class GradlePluginAdapter private constructor(
    project: Project,
    internal val config: PluginEffectiveConfiguration,
    private val isBeforeProcessResources: Boolean,
    private val logger: Logger,
) : PluginAdapterBuild {


    private val projectDir = config.projectDir
    private val projectName = config.projectName
    private val buildResourcesDir: File =
        project.getBuildResourcesDir(config.sourceSetName.get())
    private val jarProject: Boolean =
        project.tasks.withType(War::class.java).isEmpty()
    private val jarFiles: FileCollection
    private val resolvedArtifacts: Provider<Set<ModuleIdentifier>>
    private val classFinderClasspath: FileCollection

    constructor(
        task: Task,
        config: PluginEffectiveConfiguration,
        isBeforeProcessResources: Boolean
    ) : this(task.project, config, isBeforeProcessResources, task.logger)

    init {
        val dependencyConfiguration =
            project.configurations.getByName(config.dependencyScope.get())
        resolvedArtifacts =
            dependencyConfiguration.incoming.artifacts.resolvedArtifacts.map { result ->
                result.filter { it.id is ModuleComponentArtifactIdentifier }
                    .map { (it.id.componentIdentifier as ModuleComponentIdentifier).moduleIdentifier }
                    .toSet()
            } ?: project.provider { emptySet() }
        classFinderClasspath =
            createClassFinderClasspath(project, dependencyConfiguration)
        jarFiles = dependencyConfiguration.incoming.files.filter {
            it.name.endsWith(".jar", true)
        } ?: project.files()
    }

    // ClassFinder instance is created the first time it is accessed with the
    // relative getter
    private lateinit var _classFinder: ClassFinder

    private fun createClassFinder(): ClassFinder {
        val apis = classFinderClasspath.filter { it.exists() }.toSet()

        val classFinder =
            BuildFrontendUtil.getClassFinder(apis.map { it.absolutePath })
        // sanity check that the project has flow-server.jar as a dependency
        try {
            classFinder.loadClass<Any>("com.vaadin.flow.server.webcomponent.WebComponentModulesWriter")
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Failed to find classes from flow-server.jar. The project '${projectName}' needs to have a dependency on flow-server.jar")
        }
        logger.info("Passing this classpath to NodeTasks.Builder: ${apis.toPrettyFormat()}")
        return classFinder
    }

    override fun applicationProperties(): File =
        config.applicationProperties.get()

    override fun eagerServerLoad(): Boolean = config.eagerServerLoad.get()

    override fun frontendDirectory(): File = config.frontendDirectory.get()

    override fun generatedTsFolder(): File = config.generatedTsFolder.get()

    override fun getClassFinder(): ClassFinder {
        if (!::_classFinder.isInitialized) {
            _classFinder = createClassFinder()
        }
        return _classFinder
    }

    private fun createClassFinderClasspath(
        project: Project,
        dependencyConfiguration: Configuration?
    ): FileCollection {
        val dependencyConfigurationJars: FileCollection =
            if (dependencyConfiguration != null) {
                val artifactFilter = config.classpathFilter.toPredicate()
                val artifacts = dependencyConfiguration.incoming.artifactView {
                    it.componentFilter { componentId ->
                        // a componentId different ModuleComponentIdentifier
                        // could be a local library, should not be filtered out
                        val accepted = componentId !is ModuleComponentIdentifier || artifactFilter.test(
                            componentId.moduleIdentifier
                        )
                        accepted
                    }
                }.files
                artifacts
            } else project.files()

        val sourceSetName = config.sourceSetName.get()

        // we need to also analyze the project's classes
        val classesDirs: FileCollection =
            project.getSourceSet(sourceSetName).output.classesDirs;

        val resourcesDir: File? =
            project.getSourceSet(sourceSetName).output.resourcesDir

        // for Spring Boot project there is no "providedCompile" scope: the WAR plugin brings that in.
        val providedDeps: Configuration? =
            project.configurations.findByName("providedCompile")
        val servletJar: FileCollection = providedDeps
            ?.incoming?.files?.filter {
                it.absolutePath.matches(servletApiJarRegex)
            } ?: project.files()

        return project.files(classesDirs, resourcesDir, servletJar)
            .filter { it != null }
            .plus(dependencyConfigurationJars.filter {
                check(it.exists()) { "$it doesn't exist" }
                it.exists()
            })
    }

    override fun getJarFiles(): MutableSet<File> = jarFiles.toMutableSet()

    override fun isJarProject(): Boolean = jarProject

    override fun isDebugEnabled(): Boolean = true

    override fun javaSourceFolder(): File = config.javaSourceFolder.get()

    override fun javaResourceFolder(): File = config.javaResourceFolder.get()

    override fun logDebug(debugMessage: CharSequence) {
        logger.debug(debugMessage.toString())
    }

    override fun logDebug(debugMessage: CharSequence, throwable: Throwable?) {
        logger.debug(debugMessage.toString(), throwable)
    }

    override fun logInfo(infoMessage: CharSequence) {
        logger.info(infoMessage.toString())
    }

    override fun logWarn(warningMessage: CharSequence) {
        logger.warn(warningMessage.toString())
    }

    override fun logWarn(warningMessage: CharSequence, throwable: Throwable?) {
        logger.warn(warningMessage.toString(), throwable)
    }

    override fun logError(errorMessage: CharSequence) {
        logger.error(errorMessage.toString())
    }

    override fun logError(warning: CharSequence, e: Throwable?) {
        logger.error(warning.toString(), e)
    }

    override fun nodeDownloadRoot(): URI =
        URI.create(config.nodeDownloadRoot.get())

    override fun nodeAutoUpdate(): Boolean = config.nodeAutoUpdate.get()

    override fun nodeVersion(): String = config.nodeVersion.get()

    override fun npmFolder(): File = config.npmFolder.get()

    override fun openApiJsonFile(): File = config.openApiJsonFile.get()

    override fun pnpmEnable(): Boolean = config.pnpmEnable.get()

    override fun bunEnable(): Boolean = config.bunEnable.get()

    override fun useGlobalPnpm(): Boolean = config.useGlobalPnpm.get()

    override fun projectBaseDirectory(): Path = projectDir.toPath()

    override fun requireHomeNodeExec(): Boolean =
        config.requireHomeNodeExec.get()

    override fun servletResourceOutputDirectory(): File {
        // when running a task which runs before processResources, we need to
        // generate stuff to build/vaadin-generated.
        //
        // However, after processResources is done, anything generated into
        // build/vaadin-generated would simply be ignored. In such case we therefore
        // need to generate stuff directly to build/resources/main.
        if (isBeforeProcessResources) {
            return File(
                config.resourceOutputDirectory.get(),
                Constants.VAADIN_SERVLET_RESOURCES
            )
        }
        return File(buildResourcesDir, Constants.VAADIN_SERVLET_RESOURCES)
    }

    override fun webpackOutputDirectory(): File =
        config.webpackOutputDirectory.get()

    override fun frontendResourcesDirectory(): File =
        config.frontendResourcesDirectory.get()

    override fun generateBundle(): Boolean = config.generateBundle.get()

    override fun generateEmbeddableWebComponents(): Boolean =
        config.generateEmbeddableWebComponents.get()

    override fun optimizeBundle(): Boolean = config.optimizeBundle.get()

    override fun runNpmInstall(): Boolean = config.runNpmInstall.get()

    override fun buildFolder(): String {
        val projectBuildDir = config.projectBuildDir.get()
        if (projectBuildDir.startsWith(projectDir.toString())) {
            return File(projectBuildDir).relativeTo(projectDir).toString()
        }
        return projectBuildDir
    }

    override fun postinstallPackages(): List<String> =
        config.postinstallPackages.get()

    override fun isFrontendHotdeploy(): Boolean = config.frontendHotdeploy.get()

    override fun ciBuild(): Boolean = config.ciBuild.get()

    override fun skipDevBundleBuild(): Boolean = config.skipDevBundleBuild.get()

    override fun forceProductionBuild(): Boolean =
        config.forceProductionBuild.get()

    override fun compressBundle(): Boolean {
        // The compress bundle was decided to not be configurable as there is no
        // point in not compressing it except in the case where we create a pre-compiled frontend bundle jar.
        // For that there is another maven plugin that is used just for this case.
        return true
    }

    override fun isPrepareFrontendCacheDisabled(): Boolean =
        config.alwaysExecutePrepareFrontend.get()

    override fun isReactEnabled(): Boolean = config.reactEnable.get()

    override fun applicationIdentifier(): String =
        config.applicationIdentifier.get()

    override fun isNpmExcludeWebComponents(): Boolean =
        config.npmExcludeWebComponents.get()

    override fun checkRuntimeDependency(
        groupId: String,
        artifactId: String,
        missingDependencyMessageConsumer: Consumer<String>?
    ): Boolean {
        val dependencyAbsent = resolvedArtifacts.get().none {
            groupId == it.group && artifactId == it.name
        }
        if (dependencyAbsent && missingDependencyMessageConsumer != null) {
            missingDependencyMessageConsumer.accept(
                """
                The dependency ${groupId}:${artifactId} has not been found in the project configuration.
                Please add the following dependency to your project configuration:
                
                dependencies {
                    runtimeOnly("${groupId}:${artifactId}")
                }                
            """.trimIndent()
            )
        }
        return dependencyAbsent
    }

    override fun frontendExtraFileExtensions(): List<String> =
        config.frontendExtraFileExtensions.get()

    override fun isFrontendIgnoreVersionChecks(): Boolean = config.frontendIgnoreVersionChecks.get()

}
