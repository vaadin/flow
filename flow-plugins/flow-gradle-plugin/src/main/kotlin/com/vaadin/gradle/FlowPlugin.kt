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

import com.vaadin.flow.plugin.base.BuildFrontendUtil
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
        public const val GRADLE_MINIMUM_SUPPORTED_VERSION: String = "8.14"
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

            // auto-activate tasks: https://github.com/vaadin/vaadin-gradle-plugin/issues/48
            if (config.productionMode.get()) {
                // In production mode, vaadinBuildFrontend is self-contained
                // and performs its own frontend preparation, so there is no
                // need for vaadinPrepareFrontend to run beforehand.
                // this will also catch the War task since it extends from Jar
                project.tasks.withType(Jar::class.java) { task: Jar ->
                    task.dependsOn("vaadinBuildFrontend")
                    // Restore the production token before packaging in
                    // case it was deleted by a previous build's cleanup.
                    task.doFirst {
                        val svc = (project.tasks.getByName("vaadinBuildFrontend")
                            as VaadinBuildFrontendTask).getTokenService().orNull
                        svc?.ensureToken()
                    }
                }
            } else if (config.alwaysExecutePrepareFrontend.get()) {
                // In development mode, vaadinPrepareFrontend is not
                // auto-triggered by default. Since Vaadin 25, the dev
                // server handles frontend preparation at runtime, so
                // running the task during every IDE-triggered build is
                // unnecessary and can interfere with the running Vite
                // dev server.
                // However, if alwaysExecutePrepareFrontend is set,
                // restore the old behavior and chain the task to
                // processResources.
                project.tasks.getByPath(config.processResourcesTaskName.get()).dependsOn("vaadinPrepareFrontend")
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

            // In production mode, vaadinBuildFrontend performs frontend
            // preparation itself and needs dependent project jars to be
            // built for classpath scanning to work properly.
            val buildFrontendTask = project.tasks.getByName("vaadinBuildFrontend")
            buildFrontendTask.dependsOn(
                project.configurations.getByName(config.dependencyScope.get()).jars
            ).usesService(toolsService)
            if (config.alwaysExecuteBuildFrontend.get()) {
                buildFrontendTask
                    .doNotTrackState("State tracking is disabled. Use the 'alwaysExecuteBuildFrontend' plugin setting to enable the feature")
            }

            // Register a build service that restores the production token
            // file before builds and cleans it up when the build finishes.
            // The service is looked up by @ServiceReference on the task.
            if (config.productionMode.get()) {
                val buildAdapter = GradlePluginAdapter(buildFrontendTask, config, false)
                val tokenService = project.gradle.sharedServices.registerIfAbsent(
                    "vaadinBuildFrontendToken",
                    BuildFrontendTokenService::class.java
                ) {
                    it.parameters.getTokenFilePath().set(
                        BuildFrontendUtil.getTokenFile(buildAdapter).absolutePath
                    )
                    it.parameters.getCachedTokenFilePath().set(
                        java.io.File(config.projectBuildDir.get(),
                            VaadinBuildFrontendTask.CACHED_BUILD_INFO_FILE).absolutePath
                    )
                }
                // Ensure close() fires after vaadinBuildFrontend and
                // all Jar/War packaging tasks have completed.
                buildFrontendTask.usesService(tokenService)
                project.tasks.withType(Jar::class.java) { task: Jar ->
                    task.usesService(tokenService)
                }
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
