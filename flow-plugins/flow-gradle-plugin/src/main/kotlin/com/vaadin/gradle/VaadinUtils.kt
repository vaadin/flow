/**
 *    Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.gradle


import java.io.File
import com.vaadin.flow.server.Constants
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
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

// `Project.getProperties()["sourceSets"]` is deprecated and scheduled to fail
// in Gradle 10. The Java plugin registers the source sets as a typed extension,
// so fetch it through the extensions API instead.
internal val Project.sourceSets: SourceSetContainer get() = project.extensions.getByType(SourceSetContainer::class.java)
internal fun Project.getSourceSet(sourceSetName: String): SourceSet = sourceSets.getByName(sourceSetName)
internal fun Project.getBuildResourcesDir(sourceSetName: String): File = getSourceSet(sourceSetName).output.resourcesDir!!

internal val Provider<File>.absolutePath: Provider<String> get() = map { it.absolutePath }

/**
 * Whether this directory follows the standard `META-INF/VAADIN/webapp`
 * layout that holds the production frontend bundle. When it does, the Vaadin
 * servlet resources (config, token, `stats.json`) live in the parent
 * `META-INF/VAADIN` directory, next to the `webapp` bundle, so the whole tree
 * is a self-contained, task-owned unit that can be packaged into the
 * application archive. A custom `frontendOutputDirectory` that does not follow
 * this layout cannot be packaged that way and is rejected by
 * [GradlePluginAdapter.servletResourceOutputDirectory].
 */
internal fun File.hasVaadinWebappResourcesPath(): Boolean =
    path.replace(File.separatorChar, '/').removeSuffix("/").endsWith(
        Constants.VAADIN_WEBAPP_RESOURCES.removeSuffix("/")
    )

/**
 * Passes the value through only when [block] returns `true`, otherwise the
 * returned provider has no value. Backed by the public [Provider.filter] API.
 */
internal fun <T: Any> Provider<T>.filterBy(block: (T) -> Boolean): Provider<T> = filter { block(it) }

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
