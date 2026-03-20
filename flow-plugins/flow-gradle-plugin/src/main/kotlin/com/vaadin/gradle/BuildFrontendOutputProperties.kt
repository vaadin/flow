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
import com.vaadin.flow.internal.FrontendUtils
import com.vaadin.flow.plugin.base.BuildFrontendUtil
import org.gradle.api.provider.Property
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

/**
 * Declaratively defines the outputs of the [VaadinBuildFrontendTask].
 *
 * A cached copy of the production `flow-build-info.json` token file is
 * stored in the project build directory (e.g. `build/`). This serves
 * both as the Gradle up-to-date marker (its existence and content drive
 * the up-to-date check) and as a source for restoring the token when
 * the original in `build/resources/main/` has been deleted (e.g. by
 * post-packaging cleanup or deleteOnExit).
 *
 * The [getFrontendIndexHtml] output tracks the `index.html` file that the
 * task creates if it is missing. Declaring it as an output means Gradle
 * also tracks its content for up-to-date checking, so user edits to the
 * file will trigger a rebuild.
 *
 * The generated frontend directory is declared as [LocalState] so that
 * Gradle will clean it before re-execution and restore it from cache,
 * but its contents do not participate in up-to-date checking (the
 * generated files are non-deterministic across runs).
 */
internal class BuildFrontendOutputProperties(
    adapter: GradlePluginAdapter
) {

    private val cachedBuildInfoFile: File =
        File(adapter.config.projectBuildDir.get(),
            VaadinBuildFrontendTask.CACHED_BUILD_INFO_FILE)
    private val generatedTsFolder: File =
        BuildFrontendUtil.getGeneratedFrontendDirectory(adapter)
    private val frontendIndexHtml: File =
        File(BuildFrontendUtil.getFrontendDirectory(adapter),
            FrontendUtils.INDEX_HTML)

    @OutputFile
    fun getCachedBuildInfoFile(): File = cachedBuildInfoFile

    @OutputFile
    @Optional
    fun getFrontendIndexHtml(): File = frontendIndexHtml

    @LocalState
    fun getGeneratedTsFolder(): File = generatedTsFolder
}
