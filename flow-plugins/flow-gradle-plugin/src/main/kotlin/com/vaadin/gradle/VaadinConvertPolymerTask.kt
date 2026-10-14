/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.gradle

import com.vaadin.flow.plugin.base.ConvertPolymerCommand
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskAction

/**
 * This task converts Polymer-based source files into Lit.
 * By default, the task tries to convert all `*.js` and `*.java` files.
 */
public open class VaadinConvertPolymerTask : DefaultTask() {

    private val config = PluginEffectiveConfiguration.get(project)

    init {
        group = "Vaadin"
        description = "converts Polymer-based source files into Lit."
    }

    @TaskAction
    public fun vaadinConvertPolymer() {
        logger.info("Running the vaadinConvertPolymer task with effective configuration $config")
        val adapter = GradlePluginAdapter(project, config, true)

        val pathProperty: String = System.getProperty("path") ?: ""
        val useLit1Property: Boolean = project.getBooleanProperty("useLit1") ?: false
        val disableOptionalChainingProperty: Boolean = project.getBooleanProperty("disableOptionalChaining") ?: false

        ConvertPolymerCommand(adapter, pathProperty, useLit1Property, disableOptionalChainingProperty)
            .use { it.execute() }
    }
}
