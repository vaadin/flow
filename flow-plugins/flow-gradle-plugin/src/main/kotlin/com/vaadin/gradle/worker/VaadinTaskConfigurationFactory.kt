package com.vaadin.gradle.worker

import java.util.function.Supplier
import javax.inject.Inject

internal class VaadinTaskConfigurationFactory @Inject constructor(
    private val flagsFactory: FlagsFactory,
    private val locationsFactory: LocationsFactory,
    private val stringsFactory: StringsFactory,
    private val classpathFactory: ClassFinderClasspathFactory
) : Supplier<VaadinTaskConfiguration> {

    override fun get(): VaadinTaskConfiguration {
        return VaadinTaskConfiguration(
            flagsFactory.get(),
            locationsFactory.get(),
            stringsFactory.get(),
            classpathFactory.get()
        )
    }

    public companion object
}