/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.gradle

import org.junit.After
import org.junit.Before
import java.io.File

/**
 * Prepares a test Gradle project - creates a temp dir for the [testProject] and allow you to run gradle
 * tasks. See [TestProject] for more details.
 * @author mavi
 */
abstract class AbstractGradleTest {

    val flowVersion = resolveFlowVersion()
    val slf4jVersion = "2.0.3"

    /**
     * The testing Gradle project. Automatically deleted after every test.
     * Don't use TemporaryFolder JUnit `@Rule` since it will always delete the folder afterwards,
     * making it impossible to investigate the folder in case of failure.
     */
    lateinit var testProject: TestProject

    @Before
    fun createTestProjectFolder() {
        testProject = TestProject()
    }

    @After
    fun deleteTestProjectFolder() {
        // comment out if a test is failing and you need to investigate the project files.
        testProject.delete()
    }

    @Before
    fun dumpEnvironment() {
        println("Test project directory: ${testProject.dir}")
        println("Building the test projects against Flow $flowVersion")
    }

    private companion object {
        const val VERSION_ENV = "vaadin.version"
        const val FALLBACK_VERSION = "24.9-SNAPSHOT"

        /**
         * The version the generated test projects build against, taken from the
         * `vaadin.version` environment variable that the `functionalTest` task
         * sets to the version being built.
         *
         * The fallback to a published snapshot only exists for running the
         * tests outside that task. On CI it would be silent and harmful: the
         * tests would pass against a released version instead of the one just
         * built, so the build would report green without ever exercising the
         * changes under test. Fail there instead.
         */
        fun resolveFlowVersion(): String {
            val version = System.getenv(VERSION_ENV).takeUnless { it.isNullOrEmpty() }
            check(version != null || System.getenv("CI").isNullOrEmpty()) {
                "The $VERSION_ENV environment variable is not set, so the test " +
                        "projects would be built against the published " +
                        "$FALLBACK_VERSION rather than against the version under " +
                        "test. The functionalTest task in build.gradle sets it; " +
                        "run the tests through that task."
            }
            return version ?: FALLBACK_VERSION
        }
    }
}
