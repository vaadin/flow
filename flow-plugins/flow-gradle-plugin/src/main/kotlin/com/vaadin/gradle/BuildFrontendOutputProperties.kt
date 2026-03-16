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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputFile

/**
 * Declaratively defines the outputs of the [VaadinBuildFrontendTask].
 * Uses a marker file in the Vaadin-generated directory to track build
 * completion. The actual production bundle is written to the shared
 * resources directory (build/resources/main/META-INF/VAADIN/) which
 * cannot be declared as a task output because it overlaps with other
 * Gradle tasks (e.g. processResources, Spring Boot's resolveMainClassName).
 */
internal class BuildFrontendOutputProperties(
    adapter: GradlePluginAdapter
) {

    private val markerFile: File =
        File(adapter.config.resourceOutputDirectory.get(), "build-frontend.marker")

    @OutputFile
    fun getBuildFrontendMarker(): File = markerFile
}
