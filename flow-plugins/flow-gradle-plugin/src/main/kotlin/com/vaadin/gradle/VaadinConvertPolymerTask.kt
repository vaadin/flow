/**
 *    Copyright 2000-2022 Vaadin Ltd
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
