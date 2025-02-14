package com.vaadin.gradle.worker

import com.vaadin.gradle.PluginEffectiveConfiguration
import com.vaadin.gradle.getSourceSet
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import java.io.File
import java.util.function.Supplier
import javax.inject.Inject

private val servletApiJarRegex = Regex(".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$")

internal class ClassFinderClasspathFactory @Inject constructor(
    private val project: Project,
    private val config: PluginEffectiveConfiguration,
) : Supplier<Set<File>> {

    override fun get(): Set<File> {
        val dependencyConfiguration: Configuration? = project.configurations.findByName(config.dependencyScope.get())
        val dependencyConfigurationJars: List<File> = if (dependencyConfiguration != null) {
            var artifacts: List<ResolvedArtifact> =
                dependencyConfiguration.resolvedConfiguration.resolvedArtifacts.toList()

            // Detect local filesystem dependencies that are not resolved as artifacts
            // They will be added to the filtered artifacts list
            val filesystemDependencies =
                dependencyConfiguration.resolvedConfiguration.files.minus(artifacts.map { it.file }.toSet())

            val artifactFilter = config.classpathFilter.toPredicate()
            artifacts = artifacts.filter { artifactFilter.test(it.moduleVersion.id.module) }

            artifacts.map { it.file }.plus(filesystemDependencies)
        } else listOf()

        // we need to also analyze the project's classes
        val classesDirs: List<File> = project.getSourceSet(config.sourceSetName.get()).output.classesDirs
            .toList()
            .filter { it.exists() }

        val resourcesDir: List<File> =
            listOfNotNull(project.getSourceSet(config.sourceSetName.get()).output.resourcesDir)
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
        return apis
    }
}