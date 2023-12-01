/**
 *    Copyright 2000-2023 Vaadin Ltd
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


import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import com.vaadin.flow.function.SerializableSupplier
import com.vaadin.flow.server.frontend.FrontendTools
import com.vaadin.flow.server.frontend.FrontendToolsSettings
import com.vaadin.flow.server.frontend.FrontendUtils
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.provider.Providers
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import java.io.File
import java.net.URI
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Finds the value of a boolean property. It searches in gradle and system properties.
 *
 * If the property is defined in both gradle and system properties, then the gradle property is taken.
 *
 * @param propertyName the property name
 *
 * @return `null` if the property is not present, `true` if it's defined or if it's set to "true"
 * and `false` otherwise.
 */
public fun Project.getBooleanProperty(propertyName: String) : Boolean? {
    if (System.getProperty(propertyName) != null) {
        val value: String = System.getProperty(propertyName)
        val valueBoolean: Boolean = value.isBlank() || value.toBoolean()
        logger.info("Set $propertyName to $valueBoolean because of System property $propertyName='$value'")
        return valueBoolean
    }
    if (project.hasProperty(propertyName)) {
        val value: String = project.property(propertyName) as String
        val valueBoolean: Boolean = value.isBlank() || value.toBoolean()
        logger.info("Set $propertyName to $valueBoolean because of Gradle project property $propertyName='$value'")
        return valueBoolean
    }
    return null
}

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
