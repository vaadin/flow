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
import java.io.File
import java.net.URI

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
    convention.getByType(VaadinFlowPluginExtension::class.java).apply(block)
}

internal fun Collection<File>.toPrettyFormat(): String = joinToString(prefix = "[", postfix = "]") { if (it.isFile) it.name else it.absolutePath }

internal fun VaadinFlowPluginExtension.createFrontendTools(): FrontendTools {
    var settings = FrontendToolsSettings(npmFolder.absolutePath, SerializableSupplier { FrontendUtils.getVaadinHomeDirectory().absolutePath })
    settings.setNodeVersion(nodeVersion)
    settings.setNodeDownloadRoot(URI(nodeDownloadRoot))
    return FrontendTools(settings)
}

/**
 * Returns only jar files from given file collection.
 */
internal val Configuration.jars: FileCollection
    get() = filter { it.name.endsWith(".jar", true) }
