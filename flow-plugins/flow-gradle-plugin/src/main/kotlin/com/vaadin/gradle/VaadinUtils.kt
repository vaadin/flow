/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.gradle


import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.provider.Providers
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

/**
 * Finds the value of a boolean property. It searches in gradle and system properties.
 *
 * If the property is defined in both gradle and system properties, then the system property is taken.
 *
 * @param propertyName the property name
 *
 * @return `null` if the property is not present, `true` if it's defined or if it's set to "true"
 * and `false` otherwise.
 */
public fun Project.getBooleanProperty(propertyName: String) : Provider<Boolean> =
    getStringProperty(propertyName)
        .map { it.isBlank() || it.toBoolean() }
/**
 * Finds the value of a string property. It searches in gradle and system properties.
 *
 * If the property is defined in both gradle and system properties, then the system property is taken.
 *
 * @param propertyName the property name
 *
 * @return the value of the property or `null` if the property is not present.
 */
public fun Project.getStringProperty(propertyName: String) : Provider<String> =
    providers.systemProperty(propertyName)
        .orElse(providers.gradleProperty(propertyName))


/**
 * Allows Kotlin-based gradle scripts to be configured via the `vaadin{}` DSL block:
 * ```
 * vaadin {
 *   optimizeBundle = false
 * }
 * ```
 */
public fun Project.vaadin(block: VaadinFlowPluginExtension.() -> Unit) {
    VaadinFlowPluginExtension.get(this).apply(block)
}

internal fun Collection<File>.toPrettyFormat(): String = joinToString(prefix = "[", postfix = "]") { if (it.isFile) it.name else it.absolutePath }

/**
 * Returns only jar files from given file collection.
 */
internal val Configuration.jars: FileCollection
    get() = filter { it.name.endsWith(".jar", true) }

internal val Project.sourceSets: SourceSetContainer get() = project.properties["sourceSets"] as SourceSetContainer
internal fun Project.getSourceSet(sourceSetName: String): SourceSet = sourceSets.getByName(sourceSetName)
internal fun Project.getBuildResourcesDir(sourceSetName: String): File = getSourceSet(sourceSetName).output.resourcesDir!!

internal val Provider<File>.absolutePath: Provider<String> get() = map { it.absolutePath }

/**
 * Same thing as [Provider.map]. Works around the bug in Gradle+Kotlin which
 * renders [Provider.map] unable to return null in Kotlin: https://github.com/gradle/gradle/issues/12388
 */
internal fun <IN: Any, OUT> Provider<IN>.mapOrNull(block: (IN) -> OUT?): Provider<OUT> = flatMap { Providers.ofNullable(block(it)) }

/**
 * Workaround for https://github.com/gradle/gradle/issues/19981
 */
internal fun <T: Any> Provider<T>.filterBy(block: (T) -> Boolean): Provider<T> = mapOrNull { if (block(it)) it else null }

/**
 * Passes the value if the file exists.
 */
internal fun Provider<File>.filterExists(): Provider<File> = filterBy { it.exists() }

/**
 * Passes the value if the file denoted by the string value exists.
 */
@JvmName("filterExistsString")
internal fun Provider<String>.filterExists(): Provider<String> = filterBy { File(it).exists() }

internal fun Provider<RegularFile>.asFile(): Provider<File> = map { it.asFile }
@JvmName("directoryAsFile")
internal fun Provider<Directory>.asFile(): Provider<File> = map { it.asFile }
