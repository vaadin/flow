/*
 * Copyright 2000-2020 Vaadin Ltd
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
import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.frontend.scanner.ClassFinder
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.War
import java.io.File
import java.net.URI
import java.nio.file.Path

private val servletApiJarRegex = Regex(".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$")

internal class GradlePluginAdapter(val project: Project, private val isBeforeProcessResources: Boolean): PluginAdapterBuild {
    val extension: VaadinFlowPluginExtension =
        VaadinFlowPluginExtension.get(project)

    override fun applicationProperties(): File = extension.applicationProperties

    override fun eagerServerLoad(): Boolean = extension.eagerServerLoad

    override fun frontendDirectory(): File = extension.frontendDirectory

    override fun generatedFolder(): File = extension.generatedFolder

    override fun generatedTsFolder(): File = extension.generatedTsFolder

    override fun getClassFinder(): ClassFinder {
        val runtimeClasspathJars: List<File> = project.configurations.findByName("runtimeClasspath")
            ?.toList() ?: listOf()

        // we need to also analyze the project's classes
        val sourceSet: SourceSetContainer = project.properties["sourceSets"] as SourceSetContainer
        val classesDirs: List<File> = sourceSet.getByName("main").output.classesDirs
            .toList()
            .filter { it.exists() }

        // for Spring Boot project there is no "providedCompile" scope: the WAR plugin brings that in.
        val providedDeps: Configuration? = project.configurations.findByName("providedCompile")
        val servletJar: List<File> = providedDeps
            ?.filter { it.absolutePath.matches(servletApiJarRegex) }
            ?.toList()
            ?: listOf()

        val apis: Set<File> = (runtimeClasspathJars + classesDirs + servletJar).toSet()

        // eagerly check that all the files/folders exist, to avoid spamming the console later on
        // see https://github.com/vaadin/vaadin-gradle-plugin/issues/38 for more details
        apis.forEach {
            check(it.exists()) { "$it doesn't exist" }
        }

//        val classFinder = BuildFrontendUtil.getClassFinder(apis.map { it.absolutePath })
        // temporarily use patched ReflectionsClassFinder until https://github.com/vaadin/vaadin-gradle-plugin/issues/99 is fixed
        val classFinder = ReflectionsClassFinder(*apis.map { it.toURI().toURL() }.toTypedArray())

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
        val jarFiles = project.configurations
            .getByName("runtimeClasspath")
            .resolve()
            .filter { it.name.endsWith(".jar", true) }
        return jarFiles.toMutableSet()
    }

    override fun isJarProject(): Boolean = project.tasks.withType(War::class.java).isEmpty()

    override fun getUseDeprecatedV14Bootstrapping(): String = extension.useDeprecatedV14Bootstrapping.toString()

    override fun isDebugEnabled(): Boolean = true

    override fun javaSourceFolder(): File = extension.javaSourceFolder

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
        URI.create(extension.nodeDownloadRoot)

    override fun nodeVersion(): String = extension.nodeVersion

    override fun npmFolder(): File = extension.npmFolder

    override fun openApiJsonFile(): File = extension.openApiJsonFile

    override fun pnpmEnable(): Boolean = extension.pnpmEnable

    override fun productionMode(): Boolean = extension.productionMode

    override fun projectBaseDirectory(): Path = project.projectDir.toPath()

    override fun requireHomeNodeExec(): Boolean = extension.requireHomeNodeExec

    override fun servletResourceOutputDirectory(): File {
        // when running a task which runs before processResources, we need to
        // generate stuff to build/vaadin-generated.
        //
        // However, after processResources is done, anything generated into
        // build/vaadin-generated would simply be ignored. In such case we therefore
        // need to generate stuff directly to build/resources/main.
        if (isBeforeProcessResources) {
            return File(
                extension.resourceOutputDirectory,
                Constants.VAADIN_SERVLET_RESOURCES
            )
        }
        return File(project.buildResourcesDir, Constants.VAADIN_SERVLET_RESOURCES)
    }

    override fun webpackOutputDirectory(): File =
        requireNotNull(extension.webpackOutputDirectory) { "VaadinFlowPluginExtension.autoconfigure() was not called" }

    override fun frontendResourcesDirectory(): File = extension.frontendResourcesDirectory

    override fun generateBundle(): Boolean = extension.generateBundle

    override fun generateEmbeddableWebComponents(): Boolean = extension.generateEmbeddableWebComponents

    override fun optimizeBundle(): Boolean = extension.optimizeBundle

    override fun runNpmInstall(): Boolean = extension.runNpmInstall

    override fun webpackGeneratedTemplate(): String = extension.webpackGeneratedTemplate

    override fun webpackTemplate(): String = extension.webpackTemplate
}
