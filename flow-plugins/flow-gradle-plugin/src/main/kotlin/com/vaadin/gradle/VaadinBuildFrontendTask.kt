/**
 *    Copyright 2000-2021 Vaadin Ltd
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
import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.frontend.FrontendUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

/**
 * Task that builds the frontend bundle.
 *
 * It performs the following actions when creating a package:
 * * Update [Constants.PACKAGE_JSON] file with the [com.vaadin.flow.component.dependency.NpmPackage]
 * annotations defined in the classpath,
 * * Copy resource files used by flow from `.jar` files to the `node_modules`
 * folder
 * * Install dependencies by running `npm install`
 * * Update the [FrontendUtils.IMPORTS_NAME] file imports with the
 * [com.vaadin.flow.component.dependency.JsModule] [com.vaadin.flow.theme.Theme] and [com.vaadin.flow.component.dependency.JavaScript] annotations defined in
 * the classpath,
 * * Update [FrontendUtils.WEBPACK_CONFIG] file.
 *
 */
public open class VaadinBuildFrontendTask : DefaultTask() {
    init {
        group = "Vaadin"
        description = "Builds the frontend bundle with webpack"

        // we need the flow-build-info.json to be created, which is what the vaadinPrepareFrontend task does
        dependsOn("vaadinPrepareFrontend")
        // Maven's task run in the LifecyclePhase.PREPARE_PACKAGE phase

        // We need access to the produced classes, to be able to analyze e.g.
        // @CssImport annotations used by the project.
        dependsOn("classes")

        // Make sure to run this task before the `war`/`jar` tasks, so that
        // webpack bundle will end up packaged in the war/jar archive. The inclusion
        // rule itself is configured in the VaadinPlugin class.
        project.tasks.withType(Jar::class.java) { task: Jar ->
            task.mustRunAfter("vaadinBuildFrontend")
        }
    }

    @TaskAction
    public fun vaadinBuildFrontend() {
        val extension: VaadinFlowPluginExtension =
            VaadinFlowPluginExtension.get(project)
        logger.info("Running the vaadinBuildFrontend task with effective configuration $extension")
        val adapter = GradlePluginAdapter(project, false)
        // sanity check
        val tokenFile = BuildFrontendUtil.getTokenFile(adapter)
        check(tokenFile.exists()) { "token file $tokenFile doesn't exist!" }

        BuildFrontendUtil.updateBuildFile(adapter)

        BuildFrontendUtil.runNodeUpdater(adapter)

        if (adapter.generateBundle()) {
            BuildFrontendUtil.runFrontendBuild(adapter)
        } else {
            logger.info("Not running webpack since generateBundle is false")
        }
    }
}
