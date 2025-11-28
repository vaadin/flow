/**
 *    Copyright 2000-2022 Vaadin Ltd
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

import com.vaadin.flow.internal.JacksonUtils
import com.vaadin.flow.internal.StringUtil
import com.vaadin.flow.server.InitParameters
import org.gradle.testkit.runner.BuildResult
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.expect
import org.gradle.testkit.runner.TaskOutcome

class MiscMultiModuleTest : AbstractGradleTest() {
    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/38
     */
    @Test
    fun `vaadinPrepareFrontend waits for artifacts from dependent projects`() {
        testProject.settingsFile.writeText("include 'lib', 'web'")
        testProject.buildFile.writeText("""
            plugins {
                id 'java'
                id 'com.vaadin.flow' apply false
            }
            allprojects {
                repositories {
                    mavenLocal()
                    mavenCentral()
                    maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
                }
            }
            project(':lib') {
                apply plugin: 'java'
            }
            project(':web') {
                apply plugin: 'war'
                apply plugin: 'com.vaadin.flow'
                
                dependencies {
                    implementation project(':lib')
                    implementation("com.vaadin:flow:$flowVersion")
                }

                vaadin {
                    eagerServerLoad = false // test the vaadin{} block by changing some innocent property with limited side-effect
                }
            }
        """.trimIndent())
        testProject.newFolder("lib")
        testProject.newFolder("web")

        // the vaadinPrepareFrontend task would work erratically because of dependent jars not yet produced,
        // or it would blow up with FileNotFoundException straight away.
        testProject.build("web:vaadinPrepareFrontend")
    }

    /**
     * Tests that `vaadinPrepareFrontend` and `vaadinBuildFrontend` tasks are run only
     * on the `web` project.
     */
    @Test
    fun `vaadinBuildFrontend only runs on the web project`() {
        testProject.settingsFile.writeText("include 'lib', 'web'")
        testProject.buildFile.writeText("""
            plugins {
                id 'java'
                id 'com.vaadin.flow' apply false
            }
            allprojects {
                repositories {
                    mavenLocal()
                    mavenCentral()
                    maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
                }
            }
            project(':lib') {
                apply plugin: 'java'
            }
            project(':web') {
                apply plugin: 'war'
                apply plugin: 'com.vaadin.flow'
                
                dependencies {
                    implementation project(':lib')
                    implementation("com.vaadin:flow:$flowVersion")
                }

                vaadin {
                    eagerServerLoad = false // test the vaadin{} block by changing some innocent property with limited side-effect
                }
            }
        """.trimIndent())
        testProject.newFolder("lib")
        testProject.newFolder("web")

        val b: BuildResult = testProject.build("-Pvaadin.productionMode", "vaadinBuildFrontend", checkTasksSuccessful = false)
        b.expectTaskSucceded("web:vaadinPrepareFrontend")
        b.expectTaskSucceded("web:vaadinBuildFrontend")
        expect(null) { b.task(":lib:vaadinPrepareFrontend") }
        expect(null) { b.task(":lib:vaadinBuildFrontend") }
        expect(null) { b.task(":vaadinPrepareFrontend") }
        expect(null) { b.task(":vaadinBuildFrontend") }

        val tokenFile = File(testProject.dir, "web/build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val tokenFileContent = JacksonUtils.readTree(tokenFile.readText())
        expect("app-" + StringUtil.getHash("web",
            java.nio.charset.StandardCharsets.UTF_8
        )) { tokenFileContent.get(InitParameters.APPLICATION_IDENTIFIER).textValue() }
    }

    @Test
    fun `vaadinBuildFrontend application identifier from custom project name`() {
        testProject.settingsFile.writeText("""
            include 'lib', 'web'
            project(':web').name = 'MY_APP_ID'
        """.trimIndent())
        testProject.buildFile.writeText("""
            plugins {
                id 'java'
                id 'com.vaadin.flow' apply false
            }
            allprojects {
                repositories {
                    mavenLocal()
                    mavenCentral()
                    maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
                }
            }
            project(':lib') {
                apply plugin: 'java'
            }
        """.trimIndent())
        testProject.newFolder("lib")
        val webFolder = testProject.newFolder("web")
        val webBuildFile = Files.createFile(webFolder.toPath().resolve("build.gradle"))
        webBuildFile.writeText("""
            apply plugin: 'war'
            apply plugin: 'com.vaadin.flow'
            
            dependencies {
                implementation project(':lib')
                implementation("com.vaadin:flow:$flowVersion")
            }

            vaadin {
                eagerServerLoad = false // test the vaadin{} block by changing some innocent property with limited side-effect
            }
        """.trimIndent())

        val b: BuildResult = testProject.build("-Pvaadin.productionMode", "vaadinBuildFrontend", checkTasksSuccessful = false)
        b.expectTaskSucceded("MY_APP_ID:vaadinPrepareFrontend")
        b.expectTaskSucceded("MY_APP_ID:vaadinBuildFrontend")
        expect(null) { b.task(":lib:vaadinPrepareFrontend") }
        expect(null) { b.task(":lib:vaadinBuildFrontend") }
        expect(null) { b.task(":vaadinPrepareFrontend") }
        expect(null) { b.task(":vaadinBuildFrontend") }

        val tokenFile = File(testProject.dir, "web/build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val tokenFileContent = JacksonUtils.readTree(tokenFile.readText())
        expect("app-" + StringUtil.getHash("MY_APP_ID",
            java.nio.charset.StandardCharsets.UTF_8
        )) { tokenFileContent.get(InitParameters.APPLICATION_IDENTIFIER).textValue() }
    }

    @Test
    fun `vaadinBuildFrontend application identifier from plugin configuration`() {
        testProject.settingsFile.writeText("include 'lib', 'web'")
        testProject.buildFile.writeText("""
            plugins {
                id 'java'
                id 'com.vaadin.flow' apply false
            }
            allprojects {
                repositories {
                    mavenLocal()
                    mavenCentral()
                    maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
                }
            }
            project(':lib') {
                apply plugin: 'java'
            }
        """.trimIndent())
        testProject.newFolder("lib")
        val webFolder = testProject.newFolder("web")
        val webBuildFile = Files.createFile(webFolder.toPath().resolve("build.gradle"))
        webBuildFile.writeText("""
            apply plugin: 'war'
            apply plugin: 'com.vaadin.flow'
            
            dependencies {
                implementation project(':lib')
                implementation("com.vaadin:flow:$flowVersion")
            }

            vaadin {
                eagerServerLoad = false // test the vaadin{} block by changing some innocent property with limited side-effect
                applicationIdentifier = 'MY_APP_ID'
            }
        """.trimIndent())

        val b: BuildResult = testProject.build("-Pvaadin.productionMode", "vaadinBuildFrontend", checkTasksSuccessful = false)
        b.expectTaskSucceded("web:vaadinPrepareFrontend")
        b.expectTaskSucceded("web:vaadinBuildFrontend")
        expect(null) { b.task(":lib:vaadinPrepareFrontend") }
        expect(null) { b.task(":lib:vaadinBuildFrontend") }
        expect(null) { b.task(":vaadinPrepareFrontend") }
        expect(null) { b.task(":vaadinBuildFrontend") }

        val tokenFile = File(testProject.dir, "web/build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val tokenFileContent = JacksonUtils.readTree(tokenFile.readText())
        expect("MY_APP_ID") { tokenFileContent.get(InitParameters.APPLICATION_IDENTIFIER).textValue() }
    }

    @Test
    fun testPrepareFrontend_configurationCache() {
        testProject.settingsFile.writeText("include 'lib', 'web'")
        testProject.buildFile.writeText("""
            plugins {
                id 'java'
                id 'com.vaadin.flow' apply false
            }
            allprojects {
                repositories {
                    mavenLocal()
                    mavenCentral()
                    maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
                }
            }
            project(':lib') {
                apply plugin: 'java'
            }
        """.trimIndent())
        testProject.newFolder("lib")
        val webFolder = testProject.newFolder("web")
        // Create frontend folder, that will otherwise be created by the first
        // execution of vaadinPrepareFrontend, invalidating the cache on the
        // second run
        webFolder.resolve("src/main/frontend").mkdirs()
        val webBuildFile = Files.createFile(webFolder.toPath().resolve("build.gradle"))
        webBuildFile.writeText("""
            apply plugin: 'war'
            apply plugin: 'com.vaadin.flow'
            
            dependencies {
                implementation project(':lib')
                implementation("com.vaadin:flow:$flowVersion")
            }

            vaadin {
                eagerServerLoad = false // test the vaadin{} block by changing some innocent property with limited side-effect
                applicationIdentifier = 'MY_APP_ID'
            }
        """.trimIndent())

        val result = testProject.build("--configuration-cache", "vaadinPrepareFrontend", checkTasksSuccessful = false)
        result.expectTaskSucceded("web:vaadinPrepareFrontend")
        assertContains(result.output, "Calculating task graph as no cached configuration is available for tasks: vaadinPrepareFrontend")
        assertContains(result.output, "Configuration cache entry stored")

        val result2 = testProject.build("--configuration-cache", "vaadinPrepareFrontend", checkTasksSuccessful = false)
        result2.expectTaskOutcome("web:vaadinPrepareFrontend", TaskOutcome.UP_TO_DATE)
        assertContains(result2.output, "Reusing configuration cache")
    }


}
