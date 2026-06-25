/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.GradleVersion

/**
 * The main class of the Vaadin Gradle Plugin.
 * @author mavi@vaadin.com
 */
public class FlowPlugin : Plugin<Project> {
    public companion object {
        public const val GRADLE_MINIMUM_SUPPORTED_VERSION: String = "8.7"
    }

    override fun apply(project: Project) {
        verifyGradleVersion()

        // we need Java Plugin conventions so that we can ensure the order of tasks
        project.pluginManager.apply(JavaPlugin::class.java)
        var extensionName = "vaadin"
        if (project.extensions.findByName(extensionName) != null) {
            // fixes https://github.com/vaadin/vaadin-gradle-plugin/issues/26
            extensionName = "vaadinPlatform"
        }
        project.extensions.create(extensionName, VaadinFlowPluginExtension::class.java)
        val config = PluginEffectiveConfiguration.get(project)

        project.tasks.apply {
            register("vaadinClean", VaadinCleanTask::class.java) {
                it.configure(config)
            }
            register("vaadinPrepareFrontend", VaadinPrepareFrontendTask::class.java) {
                it.configure(config)
            }
            register("vaadinBuildFrontend", VaadinBuildFrontendTask::class.java) {
                it.configure(config)
            }
            register("vaadinConvertPolymer", VaadinConvertPolymerTask::class.java) {
                it.configure(project, config)
            }
        }

        project.afterEvaluate {

            // add a new source-set folder for generated stuff, by default `vaadin-generated`
            it.getSourceSet(config.sourceSetName.get()).resources.srcDirs(
                config.resourceOutputDirectory
            )

            // the processResources copies stuff from build/vaadin-generated
            // (which is populated by this task) and therefore must run after vaadinPrepareFrontend task.
            project.tasks.getByPath(config.processResourcesTaskName.get()).dependsOn("vaadinPrepareFrontend")

            // auto-activate tasks: https://github.com/vaadin/vaadin-gradle-plugin/issues/48
            if (config.productionMode.get()) {
                // this will also catch the War task since it extends from Jar
                project.tasks.withType(Jar::class.java) { task: Jar ->
                    task.dependsOn("vaadinBuildFrontend")
                }
            }

            val toolsService = project.gradle.sharedServices.registerIfAbsent(
                "vaadinTools",
                FrontendToolService::class.java
            ) {
                it.parameters.getToolsSettings().set(config.toolsSettings)
            }

            // make sure all dependent projects have finished building their jars, otherwise
            // the Vaadin classpath scanning will not work properly. See
            // https://github.com/vaadin/vaadin-gradle-plugin/issues/38
            // for more details.
            project.tasks.getByName("vaadinPrepareFrontend").dependsOn(
                project.configurations.getByName(config.dependencyScope.get()).jars
            ).usesService(toolsService)

            if (config.alwaysExecutePrepareFrontend.get()) {
                project.tasks.getByName("vaadinPrepareFrontend")
                    .doNotTrackState("State tracking is disabled. Use the 'alwaysExecutePrepareFrontend' plugin setting to enable the feature")
            }
        }
    }

    private fun verifyGradleVersion() {
        val currentVersion = GradleVersion.current();
        val supportedVersion =
            GradleVersion.version(GRADLE_MINIMUM_SUPPORTED_VERSION)
        if (currentVersion < supportedVersion) {
            throw GradleException(
                "Vaadin plugin requires Gradle ${GRADLE_MINIMUM_SUPPORTED_VERSION} or later. "
                        + "The current version is ${currentVersion.version}."
            )
        }
    }
}
