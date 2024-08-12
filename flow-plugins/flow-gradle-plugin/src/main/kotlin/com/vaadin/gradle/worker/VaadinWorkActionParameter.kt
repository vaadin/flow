package com.vaadin.gradle.worker

import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters

public interface VaadinWorkActionParameter : WorkParameters {
    public fun getVaadinTaskConfiguration(): Property<VaadinTaskConfiguration>
}
