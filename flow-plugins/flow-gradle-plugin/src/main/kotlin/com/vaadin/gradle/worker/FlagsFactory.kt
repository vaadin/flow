package com.vaadin.gradle.worker

import com.vaadin.gradle.PluginEffectiveConfiguration
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.War
import java.util.function.Supplier
import javax.inject.Inject

internal class FlagsFactory @Inject constructor(
    private val project: Project,
    private val config: PluginEffectiveConfiguration,
) : Supplier<Flags> {

    override fun get(): Flags {
        with(config) {
            return Flags(
                eagerServerLoad = eagerServerLoad.get(),
                jarProject = isProjectHasWarTask(),
                debugEnabled = true,
                nodeAutoUpdate = nodeAutoUpdate.get(),
                pnpmEnable = pnpmEnable.get(),
                bunEnable = bunEnable.get(),
                useGlobalPnpm = useGlobalPnpm.get(),
                requireHomeNodeExec = requireHomeNodeExec.get(),
                frontendHotdeploy = frontendHotdeploy.get(),
                skipDevBundleBuild = skipDevBundleBuild.get(),
                prepareFrontendCacheDisabled = alwaysExecutePrepareFrontend.get(),
                reactEnabled = reactEnable.get(),
                generateBundle = generateBundle.get(),
                generateEmbeddableWebComponents = generateEmbeddableWebComponents.get(),
                optimizeBundle = optimizeBundle.get(),
                runNpmInstall = runNpmInstall.get(),
                ciBuild = ciBuild.get(),
                forceProductionBuild = forceProductionBuild.get(),
                compressBundle = true,
                cleanFrontendFiles = cleanFrontendFiles.get()
            )
        }
    }

    private fun isProjectHasWarTask(): Boolean {
        return project.tasks.withType(War::class.java).isEmpty()
    }
}