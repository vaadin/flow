/**
 *    Copyright 2000-2026 Vaadin Ltd
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
import kotlin.test.assertFalse

/**
 * Guards Gradle 10 readiness: the plugin must not use APIs that Gradle has
 * deprecated for removal in Gradle 10 (such as `Project.getProperties()`).
 * The build is run on the latest released Gradle with `--warning-mode all` so
 * deprecation stack traces are printed in full; any trace pointing back into
 * the plugin's own package means the plugin still relies on an API that will
 * break in Gradle 10.
 */
class GradleDeprecationTest : AbstractGradleTest() {

    // Latest released Gradle at the time of writing; deprecations that Gradle
    // schedules for removal in 10 are already reported by 9.x.
    private val latestGradleVersion = "9.6.1"

    @Before
    fun setup() {
        testProject = TestProject(latestGradleVersion)
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
        """
        )
    }

    @Test
    fun pluginDoesNotUseApisRemovedInGradle10() {
        val result = testProject.build("vaadinClean", "--warning-mode=all")
        result.expectTaskSucceded("vaadinClean")

        // Deprecation warnings print their originating stack trace; a frame in
        // the plugin's package only appears when the plugin itself triggered
        // the deprecation. Reject any such frame.
        assertFalse(
            result.output.lineSequence().any {
                it.trimStart().startsWith("at com.vaadin.flow.gradle")
            },
            "The Vaadin Gradle plugin triggered a Gradle deprecation warning " +
                    "(see the build output above). This API is scheduled for " +
                    "removal and will break the plugin on Gradle 10; replace it " +
                    "with a supported alternative."
        )
    }
}
