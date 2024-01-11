/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createTempDirectory

class CacheTest : AbstractGradleTest() {

    lateinit var cacheDir: File

    @Before
    fun setup() {
        cacheDir = createTempDirectory("junit-vaadin-gradle-plugin-build-cache").toFile()
        setupTestProject(cacheDir);
    }

    fun setupTestProject(cacheDir: File) {
        testProject = TestProject("8.5")
        testProject.settingsFile.writeText(
            """
            buildCache {
                local {
                    directory = new File("${cacheDir.absolutePath}")
                    removeUnusedEntriesAfterDays = 30
                }
            }            
        """.trimIndent()
        )
        testProject.buildFile.writeText(
            """
            plugins {
                id 'war'
                id 'com.vaadin'
            }
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            }
            vaadin {
                nodeAutoUpdate = true // test the vaadin{} block by changing some innocent property with limited side-effect
            }
        """
        )
    }

    // Executing the build twice should create the cache on first run and reuse it on second execution
    // Possibly existing output files (e.g. package.json) should not result in overlapping output
    // TODO: repeatable test for all potential output files that might exists on first run
    @Test
    fun vaadinPrepareFrontend_outputFileExistingOnFirstRun_noOverlapCacheBuiltAndUsed() {
        File(testProject.dir, "package.json").writeText("{}")
        val result = testProject.build("--build-cache", "vaadinPrepareFrontend", checkTasksSuccessful = false)
        result.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.SUCCESS)
        testProject.clean()

        val result2 = testProject.build("--build-cache", "vaadinPrepareFrontend", checkTasksSuccessful = false)
        result2.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.FROM_CACHE)
    }

    // Simulate a project checked out on two different locations, A and B,
    // sharing the same cache folder.
    // First execution on project A builds the caches, whereas the second run
    // on project B should load data from the cache
    @Test
    fun vaadinPrepareFrontend_differentLocations_outputFileExistingOnFirst_noOverlapCacheBuiltAndUsed() {
        File(testProject.dir, "package.json").writeText("{}")
        val result = testProject.build(
            "--build-cache",
            "-Dorg.gradle.caching.debug=true",
            "vaadinPrepareFrontend",
            checkTasksSuccessful = false, debug = true
        )
        result.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.SUCCESS)

        setupTestProject(cacheDir);
        File(testProject.dir, "package.json").writeText("{}")
        val result2 = testProject.build(
            "--build-cache",
            "-Dorg.gradle.caching.debug=true",
            "vaadinPrepareFrontend",
            checkTasksSuccessful = false,
            debug = true
        )
        result2.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.FROM_CACHE)

    }

}