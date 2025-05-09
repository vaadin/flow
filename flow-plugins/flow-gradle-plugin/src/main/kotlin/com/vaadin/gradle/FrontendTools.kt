package com.vaadin.flow.gradle

import javax.inject.Inject
import com.vaadin.flow.server.frontend.FrontendTools
import com.vaadin.flow.server.frontend.FrontendToolsSettings
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Custom ValueSource to support Gradle configuration cache by catching values
 * produced by Vaadin FrontendTools processing shell commands.
 */
internal abstract class FrontendToolsValueSource :
    ValueSource<String, FrontendToolsValueSource.Parameters> {

    override fun obtain(): String? {
        return parameters.getToolsSettings().map { FrontendTools(it) }
            .flatMap { tools ->
                parameters.getAction().map { act -> act.invoke(tools) }
            }.orNull
    }

    interface Parameters : ValueSourceParameters {
        fun getToolsSettings(): Property<FrontendToolsSettings>
        fun getAction(): Property<(FrontendTools) -> String>
    }
}

/**
 * Shared service to create Gradle properties based on Vaadin FrontendTools
 * execution.
 * Properties generated by the toolsProperty method can safely be referenced by
 * Tasks input and outputs without breaking the Gradle configuration cache.
 */
internal abstract class FrontendToolService @Inject constructor(private val providerFactory: ProviderFactory) :
    BuildService<FrontendToolService.Parameters> {

    fun tools(): FrontendTools =
        parameters.getToolsSettings().map { FrontendTools(it) }.get()

    fun toolsProperty(getter: (FrontendTools) -> String): Provider<String> {
        return parameters.getToolsSettings().flatMap { toolsSettings ->
            providerFactory.of(FrontendToolsValueSource::class.java) {
                it.parameters.getToolsSettings().set(toolsSettings)
                it.parameters.getAction().set(getter)
            }
        }
    }

    interface Parameters : BuildServiceParameters {
        fun getToolsSettings(): Property<FrontendToolsSettings>
    }
}
