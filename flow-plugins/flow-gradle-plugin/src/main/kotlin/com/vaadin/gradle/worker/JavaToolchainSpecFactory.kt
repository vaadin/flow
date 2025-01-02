package com.vaadin.gradle.worker

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaToolchainSpec
import java.util.function.Supplier
import javax.inject.Inject

internal open class JavaToolchainSpecFactory @Inject constructor(private val project: Project) :
    Supplier<Action<JavaToolchainSpec>> {

    override fun get(): Action<JavaToolchainSpec> {
        val javaPluginExtension = project.extensions.findByType(JavaPluginExtension::class.java)
        val spec = javaPluginExtension?.toolchain

        return Action {
            if (spec != null) {
                it.languageVersion.set(spec.languageVersion)
                it.implementation.set(spec.implementation)
                it.vendor.set(spec.vendor)
            } else {
                //Gradle's default spec configures nothing.
            }
        }
    }

    companion object
}