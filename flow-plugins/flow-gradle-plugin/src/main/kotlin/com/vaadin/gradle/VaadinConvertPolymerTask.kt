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

    init {
        group = "Vaadin"
        description = "converts Polymer-based source files into Lit."
    }

    @TaskAction
    public fun vaadinConvertPolymer() {
        val extension: VaadinFlowPluginExtension = VaadinFlowPluginExtension.get(project)
        logger.info("Running the vaadinConvertPolymer task with effective configuration $extension")
        val adapter = GradlePluginAdapter(project, true)

        val pathProperty: String = System.getProperty("path") ?: ""
        val useLit1Property: Boolean = project.getBooleanProperty("useLit1") ?: false
        val disableOptionalChainingProperty: Boolean = project.getBooleanProperty("disableOptionalChaining") ?: false

        ConvertPolymerCommand(adapter, pathProperty, useLit1Property, disableOptionalChainingProperty)
            .use { it.execute() }
    }
}
