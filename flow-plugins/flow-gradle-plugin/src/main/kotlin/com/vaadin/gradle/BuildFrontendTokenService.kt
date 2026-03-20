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
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * A shared build service that manages the `flow-build-info.json` token
 * file lifecycle.
 *
 * Before the build (during input evaluation), the service restores the
 * production token from a cached copy if the original has been deleted
 * (e.g. by a previous build's cleanup). After the build completes,
 * [close] deletes the token so that running the application from an IDE
 * does not pick up a stale `productionMode=true` token.
 *
 * The cached copy in the build directory
 * ([VaadinBuildFrontendTask.CACHED_BUILD_INFO_FILE]) is written by the
 * task action after a successful production build and persists across
 * builds for restore purposes.
 */
internal abstract class BuildFrontendTokenService
    : BuildService<BuildFrontendTokenService.Parameters>, AutoCloseable {

    companion object {
        private const val HASH_FILE_SUFFIX = ".hash"
    }

    interface Parameters : BuildServiceParameters {
        /** Absolute path to the token file in build/resources/main/. */
        fun getTokenFilePath(): Property<String>
        /** Absolute path to the cached copy in build/. */
        fun getCachedTokenFilePath(): Property<String>
    }

    private fun hashFile(): File =
        File(parameters.getCachedTokenFilePath().get() + HASH_FILE_SUFFIX)

    /**
     * Ensures the token file exists by restoring it from the cached
     * copy if needed. Returns the cached hash from the previous build
     * so that identical builds produce the same value without depending
     * on the exact file content.
     */
    fun ensureTokenAndComputeHash(): String {
        val tokenFile = File(parameters.getTokenFilePath().get())
        val cachedFile = File(parameters.getCachedTokenFilePath().get())
        if (!tokenFile.exists() && cachedFile.exists()) {
            tokenFile.parentFile.mkdirs()
            cachedFile.copyTo(tokenFile, overwrite = true)
        }
        // Return the persisted hash from the previous build if available,
        // so the value is stable across builds even if the token file
        // content has minor non-deterministic differences.
        val hf = hashFile()
        if (hf.exists()) {
            return hf.readText()
        }
        return if (tokenFile.exists())
            tokenFile.readText().hashCode().toString()
        else ""
    }

    /**
     * Persists the content hash of the current token file so that
     * subsequent builds can use the same value for up-to-date checking.
     */
    fun cacheHash() {
        val tokenFile = File(parameters.getTokenFilePath().get())
        if (tokenFile.exists()) {
            val hf = hashFile()
            hf.parentFile.mkdirs()
            hf.writeText(tokenFile.readText().hashCode().toString())
        }
    }

    /**
     * Called by Gradle after all tasks that declared [usesService] for
     * this service have completed (including jar/war packaging tasks).
     * Deletes the production token so IDE runs default to development
     * mode.
     */
    override fun close() {
        val tokenFile = File(parameters.getTokenFilePath().get())
        if (tokenFile.exists()) {
            tokenFile.delete()
        }
    }
}
