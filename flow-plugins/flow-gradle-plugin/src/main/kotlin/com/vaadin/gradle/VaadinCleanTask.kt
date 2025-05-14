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

import com.vaadin.flow.plugin.base.CleanFrontendUtil
import com.vaadin.flow.plugin.base.CleanOptions
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Cleans everything Vaadin-related. Useful if npm fails to run after Vaadin
 * version upgrade. Deletes:
 *
 * * `${frontendDirectory}/generated`
 * * `${generatedTsFolder}`
 * * `node_modules`
 * * `package.json`
 * * `package-lock.json`
 * * `package-lock.yaml` (used by Vaadin 14.2+ pnpm)
 *
 * Doesn't delete `webpack.config.js` since it is intended to contain
 * user-specific code. See https://github.com/vaadin/vaadin-gradle-plugin/issues/43
 * for more details.
 *
 * After this task is run, remember to run the `vaadinPrepareFrontend` task to re-create some of the files;
 * the rest of the files will be re-created by Vaadin Servlet, simply by running the application
 * in the development mode.
 */
public abstract class VaadinCleanTask : DefaultTask() {

    init {
        group = "Vaadin"
        description = "Cleans the project completely and removes 'generated' folders, node_modules, src/main/bundles/, " +
                "vite.generated.js, pnpm-lock.yaml, .pnpmfile.cjs and package-lock.json"

        dependsOn("clean")
    }

    @get:Internal
    internal abstract val adapter: Property<GradlePluginAdapter>

    internal fun configure(config: PluginEffectiveConfiguration) {
        adapter.set(GradlePluginAdapter(this, config, false))
    }

    @TaskAction
    public fun clean() {
        CleanFrontendUtil.runCleaning(adapter.get(), CleanOptions())
    }
}
