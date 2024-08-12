package com.vaadin.gradle.worker

import com.vaadin.gradle.PluginEffectiveConfiguration
import org.gradle.api.Project
import java.util.function.Supplier
import javax.inject.Inject

internal class StringsFactory @Inject constructor(
    private val project: Project,
    private val config: PluginEffectiveConfiguration,
) : Supplier<Strings> {

    override fun get(): Strings {
        with(config) {
            return Strings(
                applicationIdentifier = applicationIdentifier.get(),
                nodeVersion = nodeVersion.get(),
                projectName = project.name
            )
        }
    }
}