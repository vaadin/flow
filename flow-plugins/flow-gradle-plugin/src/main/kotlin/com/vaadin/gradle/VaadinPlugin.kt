/**
 *    Copyright 2000-2023 Vaadin Ltd
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

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.GradleVersion

/**
 * The main class of the Vaadin Gradle Plugin.
 * @author mavi@vaadin.com
 */
public class VaadinPlugin : Plugin<Project> {
    public companion object {
        public const val GRADLE_MINIMUM_SUPPORTED_VERSION: String = "7.6"
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
        project.extensions.create(extensionName, VaadinFlowPluginExtension::class.java, project)

        project.tasks.apply {
            register("vaadinClean", VaadinCleanTask::class.java)
            register("vaadinPrepareFrontend", VaadinPrepareFrontendTask::class.java)
            register("vaadinBuildFrontend", VaadinBuildFrontendTask::class.java)
            register("vaadinConvertPolymer", VaadinConvertPolymerTask::class.java)
        }

        project.afterEvaluate {
            val extension: VaadinFlowPluginExtension = VaadinFlowPluginExtension.get(it)
            extension.autoconfigure(project)

            // add a new source-set folder for generated stuff, by default `vaadin-generated`
            val sourceSets: SourceSetContainer = it.properties["sourceSets"] as SourceSetContainer
            sourceSets.getByName(extension.sourceSetName).resources.srcDirs(extension.resourceOutputDirectory)

            // auto-activate tasks: https://github.com/vaadin/vaadin-gradle-plugin/issues/48
            project.tasks.getByPath(extension.processResourcesTaskName!!).dependsOn("vaadinPrepareFrontend")
            if (extension.productionMode) {
                // this will also catch the War task since it extends from Jar
                project.tasks.withType(Jar::class.java) { task: Jar ->
                    task.dependsOn("vaadinBuildFrontend")
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
