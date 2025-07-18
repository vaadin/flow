/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.test.assertContains
import kotlin.test.assertTrue
import com.vaadin.flow.gradle.AbstractGradleTest
import com.vaadin.flow.gradle.expectTaskOutcome
import org.gradle.testkit.runner.TaskOutcome
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BuildWithoutLicenseTest : AbstractGradleTest() {

    lateinit var buildInfo: File

    @Before
    fun setup() {
        buildInfo = testProject.newFile("output-flow-build-info.json")
        testProject.buildFile.writeText(
            """
            plugins {
                id 'war'
                id 'com.vaadin.flow'
            }
            repositories {
                maven {
                    url = '${realUserHome}/.m2/repository'
                }
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
                flatDir {
                   dirs("libs")
                }
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                implementation name:'commercial-addon'
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            }
            
            // Copy the flow-build-info.json so that tests can assert on it
            // after the build.
            tasks.named('vaadinBuildFrontend').configure {
                doLast {
                    def mainResourcesDir = project.sourceSets.main.output.resourcesDir
                    
                    // Define source file path based on the resources directory
                    def sourceFile = new File(mainResourcesDir, "META-INF/VAADIN/config/flow-build-info.json")
                    
                    if (sourceFile.exists()) {
                        def destFile = project.file("${buildInfo.absolutePath}")
                        destFile.text = sourceFile.text
                        
                        logger.lifecycle("Copied flow-build-info.json to temporary file: ${buildInfo.absolutePath}")
                    } else {
                        logger.warn("Could not find flow-build-info.json to copy")
                    }
                }
            }
        """
        )
        // Add a dependency with commercial marker to trigger license validation
        testProject.newFolder("libs")
        val commercialAddonJar: File =
            testProject.newFile("libs/commercial-addon.jar")
        Files.copy(
            File(javaClass.classLoader.getResource("commercial-addon.jar")!!.path).toPath(),
            commercialAddonJar.toPath(), StandardCopyOption.REPLACE_EXISTING
        )
    }

    @Test
    fun testBuildFrontendInProductionMode_buildFails() {

        val result = testProject.buildAndFail(
            "-Duser.home=${testingHomeFolder}",
            "-Pvaadin.productionMode",
            "vaadinBuildFrontend"
        )
        result.expectTaskOutcome("vaadinBuildFrontend", TaskOutcome.FAILED)
        assertContains(
            result.output,
            "Commercial features require a subscription."
        )
        assertContains(result.output, "* vaadin-commercial-component")
        assertContains(result.output, "commercialWithWatermark")
    }

    @Test
    fun testBuildFrontendInProductionMode_watermarkBuildDisabled_buildFails() {
        testProject.buildFile.appendText(
            """
            vaadin {
                commercialWithWatermark = false
            }
        """.trimIndent()
        )
        val result = testProject.buildAndFail(
            "-Duser.home=${testingHomeFolder}",
            "-Pvaadin.productionMode",
            "vaadinBuildFrontend"
        )
        result.expectTaskOutcome("vaadinBuildFrontend", TaskOutcome.FAILED)
        assertContains(
            result.output,
            "Commercial features require a subscription."
        )
        assertContains(result.output, "* vaadin-commercial-component")
        assertContains(result.output, "commercialWithWatermark")
    }

    @Test
    fun testBuildFrontendInProductionMode_watermarkBuildEnabledBySystemProperty_buildSucceeds() {

        val result = testProject.build(
            "-Duser.home=${testingHomeFolder}",
            "-DcommercialWithWatermark",
            "-Pvaadin.productionMode",
            "vaadinBuildFrontend"
        )
        result.expectTaskOutcome("vaadinBuildFrontend", TaskOutcome.SUCCESS)
        assertContains(result.output, "Application watermark enabled")

        assertTrue { buildInfo.exists() }
        val buildInfoJson = buildInfo.readText()
        assertContains(
            buildInfoJson,
            Regex("(?s).*\"watermark\\.enable\"\\s*:\\s*true.*"),
            "watermark.enable token missing or incorrect in ${buildInfo.absolutePath}: ${buildInfoJson}"
        )
    }

    @Test
    fun testBuildFrontendInProductionMode_watermarkBuildEnabled_buildSucceeds() {
        testProject.buildFile.appendText(
            """
            vaadin {
                commercialWithWatermark = true
            }
        """.trimIndent()
        )
        val result = testProject.build(
            "-Duser.home=${testingHomeFolder}",
            "-Pvaadin.productionMode",
            "vaadinBuildFrontend"
        )
        result.expectTaskOutcome("vaadinBuildFrontend", TaskOutcome.SUCCESS)
        assertContains(result.output, "Application watermark enabled")

        assertTrue { buildInfo.exists() }
        val buildInfoJson = buildInfo.readText()
        assertContains(
            buildInfoJson,
            Regex("(?s).*\"watermark\\.enable\"\\s*:\\s*true.*"),
            "watermark.enable token missing or incorrect in ${buildInfo.absolutePath}: ${buildInfoJson}"
        )
    }

    @Test
    fun testBuildFrontendInProductionMode_watermarkBuildEnabledByGradleProperty_buildSucceeds() {
        val result = testProject.build(
            "-Duser.home=${testingHomeFolder}",
            "-Pvaadin.commercialWithWatermark",
            "-Pvaadin.productionMode",
            "vaadinBuildFrontend"
        )
        result.expectTaskOutcome("vaadinBuildFrontend", TaskOutcome.SUCCESS)
        assertContains(result.output, "Application watermark enabled")

        assertTrue { buildInfo.exists() }
        val buildInfoJson = buildInfo.readText()
        assertContains(
            buildInfoJson,
            Regex("(?s).*\"watermark\\.enable\"\\s*:\\s*true.*"),
            "watermark.enable token missing or incorrect in ${buildInfo.absolutePath}: ${buildInfoJson}"
        )

    }

    companion object {

        @ClassRule
        @JvmField
        val tempHomeFolder = TemporaryFolder()
        lateinit var realUserHome: String
        lateinit var testingHomeFolder: String

        @BeforeClass
        @JvmStatic
        fun createFakeHome() {
            realUserHome = System.getProperty("user.home");
            val userHomeFolder = File(realUserHome)
            val vaadinHomeNodeFolder =
                userHomeFolder.resolve(".vaadin").resolve("node")
            // Try to speed up test by copying existing node into the fake home
            if (vaadinHomeNodeFolder.isDirectory) {
                val fakeVaadinHomeNode =
                    tempHomeFolder.root.resolve(".vaadin").resolve("node")
                fakeVaadinHomeNode.mkdirs();
                vaadinHomeNodeFolder.copyRecursively(fakeVaadinHomeNode);
                // copyRecursively does not preserve file attributes
                // fix it by manually setting executable flag so that node can
                // be launched
                vaadinHomeNodeFolder.walkTopDown().filter {
                    it.canExecute()
                }.forEach {
                    val relative = it.relativeTo(vaadinHomeNodeFolder)
                    val destination = fakeVaadinHomeNode.resolve(relative.path)
                    destination.setExecutable(true)
                }
            }
            testingHomeFolder = tempHomeFolder.root.absolutePath
            System.setProperty("user.home", testingHomeFolder)
        }

        @AfterClass
        @JvmStatic
        fun restoreUserHomeSystemProperty() {
            System.setProperty("user.home", realUserHome)
        }

    }

}