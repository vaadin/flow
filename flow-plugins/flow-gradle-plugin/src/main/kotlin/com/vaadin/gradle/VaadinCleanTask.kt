/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.gradle

import org.gradle.api.DefaultTask
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
 * * `webpack.generated.js`
 * * `package-lock.yaml` (used by Vaadin 14.2+ pnpm)
 * * `pnpm-file.js` (used by Vaadin 14.2+ pnpm)
 * * `tsconfig.json` (used by Vaadin 15+)
 * * `types.d.ts` (used by Vaadin 15+)
 *
 * Doesn't delete `webpack.config.js` since it is intended to contain
 * user-specific code. See https://github.com/vaadin/vaadin-gradle-plugin/issues/43
 * for more details.
 *
 * After this task is run, remember to run the `vaadinPrepareFrontend` task to re-create some of the files;
 * the rest of the files will be re-created by Vaadin Servlet, simply by running the application
 * in the development mode.
 */
public open class VaadinCleanTask : DefaultTask() {
    init {
        group = "Vaadin"
        description = "Cleans the project completely and removes 'generated' folders, node_modules, webpack.generated.js, " +
                "tsconfig.json, types.d.ts, pnpm-lock.yaml, pnpmfile.js and package-lock.json"

        dependsOn("clean")
    }

    @TaskAction
    public fun clean() {
        val extension: VaadinFlowPluginExtension =
                VaadinFlowPluginExtension.get(project)
        project.delete(
                extension.generatedTsFolder.absolutePath,
                extension.frontendDirectory.resolve("generated").absolutePath,
                "${project.projectDir}/node_modules",
                "${project.projectDir}/package-lock.json",
                "${project.projectDir}/webpack.generated.js",
                "${project.projectDir}/pnpm-lock.yaml", // used by Vaadin 14.2+ pnpm
                "${project.projectDir}/pnpmfile.js", // used by Vaadin 14.2+ pnpm
                "${project.projectDir}/tsconfig.json", // used by Vaadin 15+
                "${project.projectDir}/types.d.ts" // used by Vaadin 15+
        )
    }
}
