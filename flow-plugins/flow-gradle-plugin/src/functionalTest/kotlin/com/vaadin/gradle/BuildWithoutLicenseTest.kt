/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.gradle

import java.io.File
import org.gradle.testkit.runner.TaskOutcome
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.expect

class BuildWithoutLicenseTest : AbstractGradleTest() {

    @Test
    fun testBuildFrontendInProductionMode_bundleNotBuilt_buildFails() {
        // The frontend bundle is not built, as when the default bundle or an
        // existing bundle is used, but the license is still checked
        testProject.buildFile.writeText(
            """
            plugins {
                id 'war'
                id("com.vaadin")
            }
            repositories {
                maven {
                    url = '${realUserHome}/.m2/repository'
                }
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
                generateBundle = false
            }
        """.trimIndent()
        )
        val result = testProject.buildAndFail(
            "-Duser.home=${testingHomeFolder}",
            "-Pvaadin.productionMode",
            "vaadinBuildFrontend"
        )
        result.expectTaskOutcome("vaadinBuildFrontend", TaskOutcome.FAILED)
        expect(true, result.output) {
            result.output.contains("MissingLicenseKeyException")
        }
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
            realUserHome = System.getProperty("user.home")
            val userHomeFolder = File(realUserHome)
            val vaadinHomeNodeFolder =
                userHomeFolder.resolve(".vaadin").resolve("node")
            // Try to speed up test by copying existing node into the fake home
            if (vaadinHomeNodeFolder.isDirectory) {
                val fakeVaadinHomeNode =
                    tempHomeFolder.root.resolve(".vaadin").resolve("node")
                fakeVaadinHomeNode.mkdirs()
                vaadinHomeNodeFolder.copyRecursively(fakeVaadinHomeNode)
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
