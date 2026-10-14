/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.gradle

import com.vaadin.flow.plugin.base.BuildFrontendUtil
import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import kotlin.math.log

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

    private val config = PluginEffectiveConfiguration.get(project)

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
        val adapter = GradlePluginAdapter(project, config, true)
        logger.debug("Running the vaadinPrepareFrontend task with effective configuration $config")
        val tokenFile = BuildFrontendUtil.propagateBuildInfo(adapter)

        logger.info("Generated token file $tokenFile")
        check(tokenFile.exists()) { "token file $tokenFile doesn't exist!" }
        BuildFrontendUtil.prepareFrontend(adapter)
    }
}
