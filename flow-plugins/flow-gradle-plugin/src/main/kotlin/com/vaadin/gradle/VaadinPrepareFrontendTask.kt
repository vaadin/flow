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

import com.vaadin.flow.plugin.base.BuildFrontendUtil
import com.vaadin.flow.server.frontend.FrontendTools
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

/**
 * This task checks that node and npm tools are installed, copies frontend
 * resources available inside `.jar` dependencies to `node_modules`, and creates
 * or updates `package.json` and `webpack.config.json` files.
 * <p>
 * Uses Gradle incremental builds feature, i.e. Gradle skips this tasks if
 * all the inputs (config parameters, Node.js version) and outputs (generated
 * files) are up-to-date and have the same values as for previous build.
 */
@CacheableTask
public open class VaadinPrepareFrontendTask : DefaultTask() {

    private val extension: VaadinFlowPluginExtension

    private val inputProperties: PrepareFrontendInputProperties

    private val outputProperties: PrepareFrontendOutputProperties

    /**
     * Defines an object containing all the inputs of this task.
     */
    @Nested
    public open fun getTaskInputProperties(): PrepareFrontendInputProperties {
        return inputProperties
    }

    /**
     * Defines an object containing all the outputs of this task.
     */
    @Nested
    public open fun getTaskOutputProperties(): PrepareFrontendOutputProperties {
        return outputProperties
    }

    init {
        group = "Vaadin"
        description = "checks that node and npm tools are installed, copies frontend resources available inside `.jar` dependencies to `node_modules`, and creates or updates `package.json` and `webpack.config.json` files."

        extension = VaadinFlowPluginExtension.get(project)
        // Maven's task run in the LifecyclePhase.PROCESS_RESOURCES phase

        inputProperties = PrepareFrontendInputProperties(project)
        outputProperties = PrepareFrontendOutputProperties(project)

        // the processResources copies stuff from build/vaadin-generated
        // (which is populated by this task) and therefore must run after this task.
        project.tasks.getByName(extension.processResourcesTaskName!!).mustRunAfter("vaadinPrepareFrontend")

        // make sure all dependent projects have finished building their jars, otherwise
        // the Vaadin classpath scanning will not work properly. See
        // https://github.com/vaadin/vaadin-gradle-plugin/issues/38
        // for more details.
        dependsOn(project.configurations.getByName(extension.dependencyScope!!).jars)

        if (extension.alwaysExecutePrepareFrontend) {
            doNotTrackState("State tracking is disabled. Use the 'alwaysExecutePrepareFrontend' plugin setting to enable the feature");
        }
    }

    @TaskAction
    public fun vaadinPrepareFrontend() {
        // Remove Frontend/generated folder to get clean files copied/generated
        project.delete(extension.generatedTsFolder.absolutePath)
        logger.info("Running the vaadinPrepareFrontend task with effective configuration $extension")
        val adapter = GradlePluginAdapter(project, true)
        val tokenFile = BuildFrontendUtil.propagateBuildInfo(adapter)

        if (extension.requireHomeNodeExec) {
            // make sure the frontend tools are installed properly from configured URL.
            // fixes https://github.com/vaadin/vaadin-gradle-plugin/issues/76
            val tools: FrontendTools = extension.createFrontendTools()
            tools.forceAlternativeNodeExecutable()
        }

        logger.info("Generated token file $tokenFile")
        check(tokenFile.exists()) { "token file $tokenFile doesn't exist!" }
        BuildFrontendUtil.prepareFrontend(adapter)
    }
}
