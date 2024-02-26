/*
 * Copyright 2000-2024 Vaadin Ltd
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

import com.vaadin.flow.plugin.base.PluginAdapterBuild
import com.vaadin.flow.plugin.base.BuildFrontendUtil
import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.frontend.scanner.ClassFinder
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.tasks.bundling.War
import java.io.File
import java.net.URI
import java.nio.file.Path

private val servletApiJarRegex = Regex(".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$")

internal class GradlePluginAdapter(
    val project: Project,
    val config: PluginEffectiveConfiguration,
    private val isBeforeProcessResources: Boolean
): PluginAdapterBuild {
    override fun applicationProperties(): File = config.applicationProperties.get()

    override fun eagerServerLoad(): Boolean = config.eagerServerLoad.get()

    override fun frontendDirectory(): File = config.frontendDirectory.get()

    override fun generatedTsFolder(): File = config.generatedTsFolder.get()

    override fun getClassFinder(): ClassFinder {
        val dependencyConfiguration: Configuration? = project.configurations.findByName(config.dependencyScope.get())
        val dependencyConfigurationJars: List<File> = if (dependencyConfiguration != null) {
            var artifacts: List<ResolvedArtifact> =
                dependencyConfiguration.resolvedConfiguration.resolvedArtifacts.toList()
            val artifactFilter = config.classpathFilter.toPredicate()
            artifacts = artifacts.filter { artifactFilter.test(it.moduleVersion.id.module) }
            artifacts.map { it.file }
        } else listOf()

        // we need to also analyze the project's classes
        val classesDirs: List<File> = project.getSourceSet(config.sourceSetName.get()).output.classesDirs
            .toList()
            .filter { it.exists() }

        val resourcesDir: List<File> = listOfNotNull(project.getSourceSet(config.sourceSetName.get()).output.resourcesDir)
                .filter { it.exists() }

        // for Spring Boot project there is no "providedCompile" scope: the WAR plugin brings that in.
        val providedDeps: Configuration? = project.configurations.findByName("providedCompile")
        val servletJar: List<File> = providedDeps
            ?.filter { it.absolutePath.matches(servletApiJarRegex) }
            ?.toList()
            ?: listOf()

        val apis: Set<File> = (dependencyConfigurationJars + classesDirs + resourcesDir + servletJar).toSet()

        // eagerly check that all the files/folders exist, to avoid spamming the console later on
        // see https://github.com/vaadin/vaadin-gradle-plugin/issues/38 for more details
        apis.forEach {
            check(it.exists()) { "$it doesn't exist" }
        }

        val classFinder = BuildFrontendUtil.getClassFinder(apis.map { it.absolutePath })

        // sanity check that the project has flow-server.jar as a dependency
        try {
            classFinder.loadClass<Any>("com.vaadin.flow.server.webcomponent.WebComponentModulesWriter")
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Failed to find classes from flow-server.jar. The project '${project.name}' needs to have a dependency on flow-server.jar")
        }

        project.logger.info("Passing this classpath to NodeTasks.Builder: ${apis.toPrettyFormat()}")

        return classFinder
    }

    override fun getJarFiles(): MutableSet<File> {
        val jarFiles: Set<File> = project.configurations.getByName(config.dependencyScope.get()).jars.toSet()
        return jarFiles.toMutableSet()
    }

    override fun isJarProject(): Boolean = project.tasks.withType(War::class.java).isEmpty()

    override fun isDebugEnabled(): Boolean = true

    override fun javaSourceFolder(): File = config.javaSourceFolder.get()

    override fun javaResourceFolder(): File = config.javaResourceFolder.get()

    override fun logDebug(debugMessage: CharSequence) {
        project.logger.debug(debugMessage.toString())
    }

    override fun logInfo(infoMessage: CharSequence) {
        project.logger.info(infoMessage.toString())
    }

    override fun logWarn(warningMessage: CharSequence) {
        project.logger.warn(warningMessage.toString())
    }

    override fun logWarn(warningMessage: CharSequence, throwable: Throwable?) {
        project.logger.warn(warningMessage.toString(), throwable)
    }

    override fun logError(warning: CharSequence, e: Throwable?) {
        project.logger.error(warning.toString(), e)
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

    override fun projectBaseDirectory(): Path = project.projectDir.toPath()

    override fun requireHomeNodeExec(): Boolean = config.requireHomeNodeExec.get()

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
        return File(project.getBuildResourcesDir(config.sourceSetName.get()), Constants.VAADIN_SERVLET_RESOURCES)
    }

    override fun webpackOutputDirectory(): File =
        config.webpackOutputDirectory.get()

    override fun frontendResourcesDirectory(): File = config.frontendResourcesDirectory.get()

    override fun generateBundle(): Boolean = config.generateBundle.get()

    override fun generateEmbeddableWebComponents(): Boolean = config.generateEmbeddableWebComponents.get()

    override fun optimizeBundle(): Boolean = config.optimizeBundle.get()

    override fun runNpmInstall(): Boolean = config.runNpmInstall.get()

    override fun buildFolder(): String {
        val projectBuildDir = config.projectBuildDir.get()
        if (projectBuildDir.startsWith(project.projectDir.toString())) {
            return File(projectBuildDir).relativeTo(project.projectDir).toString()
        }
        return projectBuildDir
    }
    override fun postinstallPackages(): List<String> = config.postinstallPackages.get()

    override fun isFrontendHotdeploy(): Boolean = config.frontendHotdeploy.get()

    override fun ciBuild(): Boolean = config.ciBuild.get()

    override fun skipDevBundleBuild(): Boolean = config.skipDevBundleBuild.get()

    override fun forceProductionBuild(): Boolean = config.forceProductionBuild.get()

    override fun compressBundle(): Boolean {
        // The compress bundle was decided to not be configurable as there is no
        // point in not compressing it except in the case where we create a pre-compiled frontend bundle jar.
        // For that there is another maven plugin that is used just for this case.
        return true
    }

    override fun isPrepareFrontendCacheDisabled(): Boolean = config.alwaysExecutePrepareFrontend.get()

    override fun isReactEnable(): Boolean = config.reactEnable.get()
}
