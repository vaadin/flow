package com.vaadin.gradle.worker

import com.vaadin.flow.server.Constants
import com.vaadin.gradle.PluginEffectiveConfiguration
import com.vaadin.gradle.getBuildResourcesDir
import com.vaadin.gradle.jars
import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.io.File
import java.net.URI
import java.util.function.Supplier
import javax.inject.Inject

internal class LocationsFactory @Inject constructor(
    private val project: Project,
    private val config: PluginEffectiveConfiguration,
    private val isBeforeProcessResources: Boolean
) : Supplier<Locations> {

    override fun get(): Locations {
        with(config) {
            return Locations(
                applicationProperties = applicationProperties.get(),
                frontendDirectory = frontendDirectory.get(),
                generatedTsFolder = generatedTsFolder.get(),
                jarFiles = getJarFiles(),
                javaSourceFolder = javaSourceFolder.get(),
                javaResourceFolder = javaResourceFolder.get(),
                npmFolder = npmFolder.get(),
                openApiJsonFile = openApiJsonFile.get(),
                projectBaseDirectory = getProjectBaseDir(),
                servletResourceOutputDirectory = getServletResourceOutputDirectory(),
                webpackOutputDirectory = webpackOutputDirectory.get(),
                buildFolder = getBuildFolder(),
                frontendResourcesDirectory = frontendResourcesDirectory.get(),
                nodeDownloadRoot = getNodeDownloadRoot(nodeDownloadRoot),
                postinstallPackages = postinstallPackages.get(),
            )
        }
    }


    private fun getNodeDownloadRoot(nodeDownloadRoot: Property<String>): URI = URI.create(nodeDownloadRoot.get())

    private fun getProjectBaseDir(): File {
        return project.projectDir
    }


    private fun getJarFiles(): Set<File> {
        val jarFiles: Set<File> = project.configurations.getByName(config.dependencyScope.get()).jars.toSet()
        return jarFiles.toMutableSet()
    }

    private fun getServletResourceOutputDirectory(): File {
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

    private fun getBuildFolder(): String {
        val projectBuildDir = config.projectBuildDir.get()
        if (projectBuildDir.startsWith(project.projectDir.toString())) {
            return File(projectBuildDir).relativeTo(project.projectDir).toString()
        }
        return projectBuildDir
    }

}