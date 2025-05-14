/**
 *    Copyright 2000-2025 Vaadin Ltd.
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
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
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
public abstract class VaadinPrepareFrontendTask : DefaultTask() {

    //private val config = PluginEffectiveConfiguration.get(project)

    @ServiceReference
    internal abstract fun getSvc(): Property<FrontendToolService>

    @get:Internal
    internal abstract val adapter: Property<GradlePluginAdapter>

    internal fun configure(config: PluginEffectiveConfiguration) {
        adapter.set(GradlePluginAdapter(this, config, true))
    }

    /**
     * Defines an object containing all the inputs of this task.
     */
    @get:Nested
    internal val inputProperties = adapter.zip(getSvc()) { adp, svc ->
        PrepareFrontendInputProperties(
            adp,
            svc
        )
    }

    /**
     * Defines an object containing all the outputs of this task.
     */
    @get:Nested
    internal val outputProperties =
        adapter.map { PrepareFrontendOutputProperties(it) }

    init {
        group = "Vaadin"
        description =
            "checks that node and npm tools are installed, copies frontend resources available inside `.jar` dependencies to `node_modules`, and creates or updates `package.json` and `webpack.config.json` files."
        // Maven's task run in the LifecyclePhase.PROCESS_RESOURCES phase
    }

    @TaskAction
    public fun vaadinPrepareFrontend() {
        //val adapter = GradlePluginAdapter(this, config, true)
        // Remove Frontend/generated folder to get clean files copied/generated
        logger.debug("Running the vaadinPrepareFrontend task with effective configuration ${adapter.get().config}")
        val tokenFile = BuildFrontendUtil.propagateBuildInfo(adapter.get())

        logger.info("Generated token file $tokenFile")
        check(tokenFile.exists()) { "token file $tokenFile doesn't exist!" }
        BuildFrontendUtil.prepareFrontend(adapter.get())
    }
}
