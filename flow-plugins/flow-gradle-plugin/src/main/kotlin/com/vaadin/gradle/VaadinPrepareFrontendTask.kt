/**
 *    Copyright 2000-2024 Vaadin Ltd
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
import com.vaadin.gradle.worker.*
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import javax.inject.Inject


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
public open class VaadinPrepareFrontendTask @Inject constructor(objectFactory: ObjectFactory) : DefaultTask() {

    private val config = PluginEffectiveConfiguration.get(project)

    private val javaExecutionService = JavaExecutionService.from(objectFactory)

    private val taskConfigurationFactory = VaadinTaskConfigurationFactory.from(project, config, true)

    /**
     * Defines an object containing all the inputs of this task.
     */
    @get:Nested
    internal val inputProperties = PrepareFrontendInputProperties(config)

    /**
     * Defines an object containing all the outputs of this task.
     */
    @get:Nested
    internal val outputProperties = PrepareFrontendOutputProperties(project, config)

    init {
        group = "Vaadin"
        description = "checks that node and npm tools are installed, copies frontend resources available inside `.jar` dependencies to `node_modules`, and creates or updates `package.json` and `webpack.config.json` files."
        // Maven's task run in the LifecyclePhase.PROCESS_RESOURCES phase
    }

    @TaskAction
    public fun vaadinPrepareFrontend() {
        // Remove Frontend/generated folder to get clean files copied/generated
        logger.debug("Running the vaadinPrepareFrontend task with effective configuration $config")
        
        val taskConfiguration = taskConfigurationFactory.get()

        javaExecutionService.submit(VaadinPrepareFrontendWorkAction::class.java) {
            it.getVaadinTaskConfiguration().set(taskConfiguration)
        }
    }

    public abstract class VaadinPrepareFrontendWorkAction : WorkAction<VaadinWorkActionParameter> {
        private val logger: Logger = Logging.getLogger(VaadinPrepareFrontendWorkAction::class.java)

        override fun execute() {
            val taskConfiguration = parameters.getVaadinTaskConfiguration().get()
            val adapter = GradleWorkerApiAdapter.from(taskConfiguration, logger)

            val tokenFile = BuildFrontendUtil.propagateBuildInfo(adapter)

            logger.info("Generated token file $tokenFile")
            check(tokenFile.exists()) { "token file $tokenFile doesn't exist!" }
            BuildFrontendUtil.prepareFrontend(adapter)
        }
    }
}
