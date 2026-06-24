/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.gradle

import com.vaadin.flow.plugin.base.ConvertPolymerCommand
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * This task converts Polymer-based source files into Lit.
 * By default, the task tries to convert all `*.js` and `*.java` files.
 */
public abstract class VaadinConvertPolymerTask : DefaultTask() {

    init {
        group = "Vaadin"
        description = "converts Polymer-based source files into Lit."
    }

    @get:Internal
    internal abstract val adapter: Property<GradlePluginAdapter>

    @get:Internal
    internal abstract val useLit1Property: Property<Boolean>

    @get:Internal
    internal abstract val disableOptionalChainingProperty: Property<Boolean>

    @get:Internal
    internal abstract val pathProperty: Property<String>

    internal fun configure(project: Project, config: PluginEffectiveConfiguration) {
        adapter.set(GradlePluginAdapter(this, config, false))
        useLit1Property.set(project.getBooleanProperty("useLit1").orElse(false))
        disableOptionalChainingProperty.set(
            project.getBooleanProperty("disableOptionalChaining").orElse(false)
        )
        pathProperty.set(project.providers.systemProperty("path").orElse(""))
    }

    @TaskAction
    public fun vaadinConvertPolymer() {
        logger.info("Running the vaadinConvertPolymer task with effective configuration ${adapter.get().config}")

        ConvertPolymerCommand(
            adapter.get(),
            pathProperty.get(),
            useLit1Property.get(),
            disableOptionalChainingProperty.get()
        )
            .use { it.execute() }
    }
}
