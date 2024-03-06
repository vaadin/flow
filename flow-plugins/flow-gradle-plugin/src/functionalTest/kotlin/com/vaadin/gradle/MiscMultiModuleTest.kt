/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.gradle

import org.gradle.testkit.runner.BuildResult
import org.junit.Test
import kotlin.test.expect

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
                id 'com.vaadin' apply false
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
                apply plugin: 'com.vaadin'
                
                dependencies {
                    implementation project(':lib')
                    implementation("com.vaadin:flow:$flowVersion")
                }

                vaadin {
                    pnpmEnable = true
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
                id 'com.vaadin' apply false
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
                apply plugin: 'com.vaadin'
                
                dependencies {
                    implementation project(':lib')
                    implementation("com.vaadin:flow:$flowVersion")
                }

                vaadin {
                    pnpmEnable = true
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
    }
}
