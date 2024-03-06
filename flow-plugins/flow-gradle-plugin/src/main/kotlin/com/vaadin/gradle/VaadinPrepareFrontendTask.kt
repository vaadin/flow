/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.gradle

import com.vaadin.flow.plugin.base.BuildFrontendUtil
import com.vaadin.flow.server.frontend.FrontendTools
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * This task checks that node and npm tools are installed, copies frontend
 * resources available inside `.jar` dependencies to `node_modules`, and creates
 * or updates `package.json` and `webpack.config.json` files.
 */
public open class VaadinPrepareFrontendTask : DefaultTask() {

    init {
        group = "Vaadin"
        description = "checks that node and npm tools are installed, copies frontend resources available inside `.jar` dependencies to `node_modules`, and creates or updates `package.json` and `webpack.config.json` files."

        val extension: VaadinFlowPluginExtension = VaadinFlowPluginExtension.get(project)
        // Maven's task run in the LifecyclePhase.PROCESS_RESOURCES phase

        // the processResources copies stuff from build/vaadin-generated
        // (which is populated by this task) and therefore must run after this task.
        project.tasks.getByName(extension.processResourcesTaskName!!).mustRunAfter("vaadinPrepareFrontend")

        // make sure all dependent projects have finished building their jars, otherwise
        // the Vaadin classpath scanning will not work properly. See
        // https://github.com/vaadin/vaadin-gradle-plugin/issues/38
        // for more details.
        dependsOn(project.configurations.getByName(extension.dependencyScope!!).jars)
    }

    @TaskAction
    public fun vaadinPrepareFrontend() {
        val extension: VaadinFlowPluginExtension = VaadinFlowPluginExtension.get(project)
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
