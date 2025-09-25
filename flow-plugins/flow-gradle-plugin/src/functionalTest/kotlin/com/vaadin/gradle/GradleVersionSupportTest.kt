/**
 * Copyright 2000-2025 Vaadin Ltd
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

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.test.assertContains


/**
 * Tests supported and unsupported Gradle versions.
 * <p>
 * Vaadin plugin should fail fast when the project is using an unsupported Gradle version.
 */
@RunWith(Parameterized::class)
class GradleVersionSupportTest(private val versionUnderTest: GradleVersion) : AbstractGradleTest() {

    companion object {
        @JvmStatic
        @Parameters(name = "Gradle version {0}")
        fun gradleVersionsUnderTest(): List<GradleVersion> =
            arrayOf("8.3", "8.6", "8.9").map { GradleVersion(it, false) } +
                    arrayOf(
                        FlowPlugin.GRADLE_MINIMUM_SUPPORTED_VERSION,
                        "8.11",
                        "8.12",
                        "8.13",
                        "8.14"
                    ).map { GradleVersion(it, true) }
    }

    @Before
    fun setup() {
        testProject = TestProject(versionUnderTest.version)
        testProject.buildFile.writeText(
            """
            plugins {
                id 'war'
                id 'com.vaadin.flow'
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


    @Test
    fun pluginShouldFailWithUnsupportedGradleVersion() {

        if (versionUnderTest.supported) {
            val result = testProject.build("vaadinClean")
            result.expectTaskSucceded("vaadinClean")
        } else {
            val result = testProject.buildAndFail("vaadinClean")
            if (result.output.contains("Unsupported class file major version")) {
                assertContains(
                    result.output,
                    Regex("Failed to process the entry 'META-INF/versions/(\\d+)/(tools/jackson|ch/randelshofer)/"),
                    "Expecting plugin execution to fail for version ${versionUnderTest.version} " +
                            "as it is lower than the supported one (${FlowPlugin.GRADLE_MINIMUM_SUPPORTED_VERSION}) " +
                            "and it is incompatible with Jackson library used by Flow"
                )
            } else {
                assertContains(
                    result.output,
                    "requires Gradle ${FlowPlugin.GRADLE_MINIMUM_SUPPORTED_VERSION} or later",
                    true,
                    "Expecting plugin execution to fail for version ${versionUnderTest.version} " +
                            "as it is lower than the supported one (${FlowPlugin.GRADLE_MINIMUM_SUPPORTED_VERSION})"
                )
                assertContains(
                    result.output,
                    "current version is ${versionUnderTest.version}"
                )
            }
        }
    }

    data class GradleVersion(val version: String, val supported: Boolean);
}
