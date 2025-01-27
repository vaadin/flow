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

import com.vaadin.flow.plugin.base.BuildFrontendUtil
import com.vaadin.flow.plugin.base.PluginAdapterBuild
import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.frontend.BundleValidationUtil
import com.vaadin.flow.server.frontend.FrontendUtils
import com.vaadin.flow.server.frontend.TaskCleanFrontendFiles
import com.vaadin.gradle.worker.JavaExecutionService
import com.vaadin.gradle.worker.GradleWorkerApiAdapter
import com.vaadin.gradle.worker.VaadinTaskConfigurationFactory
import com.vaadin.gradle.worker.VaadinWorkActionParameter
import com.vaadin.gradle.worker.from
import com.vaadin.pro.licensechecker.LicenseChecker
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.workers.WorkAction
import javax.inject.Inject

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
public open class VaadinBuildFrontendTask @Inject constructor(objectFactory: ObjectFactory) : DefaultTask() {
    
    private val config: PluginEffectiveConfiguration = PluginEffectiveConfiguration.get(project)
    
    private val javaExecutionService = JavaExecutionService.from(objectFactory)
    
    private val taskConfigurationFactory = VaadinTaskConfigurationFactory.from(project, config, false)

    init {
        group = "Vaadin"
        description = "Builds the frontend bundle with webpack"

        // we need the flow-build-info.json to be created, which is what the vaadinPrepareFrontend task does
        dependsOn("vaadinPrepareFrontend")
        // Maven's task run in the LifecyclePhase.PROCESS_CLASSES phase

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
        logger.info("Running the vaadinBuildFrontend task with effective configuration $config")
        val taskConfiguration = taskConfigurationFactory.get()

        javaExecutionService.submit(VaadinBuildFrontendWorkAction::class.java) {
            it.getVaadinTaskConfiguration().set(taskConfiguration)
        }
    }

    public abstract class VaadinBuildFrontendWorkAction : WorkAction<VaadinWorkActionParameter> {
        private val logger: Logger = Logging.getLogger(VaadinBuildFrontendWorkAction::class.java)

        override fun execute() {
            val taskConfiguration = parameters.getVaadinTaskConfiguration().get()
            val adapter = GradleWorkerApiAdapter.from(taskConfiguration, logger)

            // sanity check
            val tokenFile = BuildFrontendUtil.getTokenFile(adapter)
            check(tokenFile.exists()) { "token file $tokenFile doesn't exist!" }

            val cleanTask = TaskCleanFrontendFiles(taskConfiguration.locations.npmFolder,
                BuildFrontendUtil.getGeneratedFrontendDirectory(adapter), adapter.classFinder
            )
            BuildFrontendUtil.runNodeUpdater(adapter)

            if (adapter.generateBundle() && BundleValidationUtil.needsBundleBuild
                    (adapter.servletResourceOutputDirectory())) {
                BuildFrontendUtil.runFrontendBuild(adapter)
                if (cleanFrontendFiles(adapter, taskConfiguration.flags.cleanFrontendFiles)) {
                    cleanTask.execute()
                }
            }
            LicenseChecker.setStrictOffline(true)
            val licenseRequired = BuildFrontendUtil.validateLicenses(adapter)
    
            BuildFrontendUtil.updateBuildFile(adapter, licenseRequired)
        }


        /**
         * Define if frontend files generated by bundle build should be cleaned or
         * not.
         *
         * The targeted frontend files are files that do not exist when
         * build-frontend target is executed.
         *
         * Extending mojo can override this method to return false so that any
         * frontend files created for the bundle build are not removed.
         *
         * @return `true` to remove created files, `false` to keep the files
         */
        private fun cleanFrontendFiles(adapter: PluginAdapterBuild, cleanFrontendFiles: Boolean): Boolean {
            if (FrontendUtils.isHillaUsed(BuildFrontendUtil.getGeneratedFrontendDirectory(adapter),
                    adapter.classFinder)) {
                /*
                 * Override this to not clean generated frontend files after the
                 * build. For Hilla, the generated files can still be useful for
                 * developers after the build. For example, a developer can use
                 * {@code vite.generated.ts} to run tests with vitest in CI.
                 */
                return false
            }
            return cleanFrontendFiles
        }
    }
}
