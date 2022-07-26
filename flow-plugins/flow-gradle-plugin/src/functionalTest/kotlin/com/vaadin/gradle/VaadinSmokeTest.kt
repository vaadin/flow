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
package com.vaadin.gradle

import com.vaadin.flow.server.InitParameters
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.expect

/**
 * The most basic tests. If these fail, the plugin is completely broken and all
 * other test classes will possibly fail as well.
 * @author mavi
 */
class VaadinSmokeTest : AbstractGradleTest() {
    @Before
    fun setup() {
        testProject.buildFile.writeText("""
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
                compile("com.vaadin:flow:$flowVersion")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                compile("org.slf4j:slf4j-simple:1.7.30")
            }
            vaadin {
                pnpmEnable = true
            }
        """)
    }

    @Test
    fun smoke() {
        testProject.build("vaadinClean")
    }

    @Test
    fun testPrepareFrontend() {
        testProject.build("vaadinPrepareFrontend")

        val tokenFile = File(testProject.dir, "build/vaadin-generated/META-INF/VAADIN/config/flow-build-info.json")
        expect(true, tokenFile.toString()) { tokenFile.isFile }
        val buildInfo: JsonObject = JsonUtil.parse(tokenFile.readText())
        expect(false, buildInfo.toJson()) { buildInfo.getBoolean(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE) }
    }

    @Test
    fun `vaadinBuildFrontend not ran by default in development mode`() {
        val result: BuildResult = testProject.build("build")
        // let's explicitly check that vaadinPrepareFrontend has been run.
        result.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.SUCCESS)
        // vaadinBuildFrontend should NOT have been executed automatically
        result.expectTaskNotRan("vaadinBuildFrontend")

        val build = File(testProject.dir, "build/resources/main/META-INF/VAADIN/webapp/VAADIN/build")
        expect(false, build.toString()) { build.exists() }
    }

    @Test
    fun `vaadinBuildFrontend can be run manually in development mode`() {
        val result: BuildResult = testProject.build("vaadinBuildFrontend")
        // let's explicitly check that vaadinPrepareFrontend has been run.
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProject.dir, "build/resources/main/META-INF/VAADIN/webapp/VAADIN/build")
        expect(true, build.toString()) { build.exists() }
        build.find("*.gz", 5..10)
        build.find("*.js", 5..10)

        val tokenFile = File(testProject.dir, "build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val buildInfo: JsonObject = JsonUtil.parse(tokenFile.readText())
        expect(false, buildInfo.toJson()) { buildInfo.getBoolean(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE) }
    }

    @Test
    fun testBuildFrontendInProductionMode() {
        val result: BuildResult = testProject.build("-Pvaadin.productionMode", "vaadinBuildFrontend")
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        // let's explicitly check that vaadinPrepareFrontend has been run
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProject.dir, "build/resources/main/META-INF/VAADIN/webapp/VAADIN/build")
        expect(true, build.toString()) { build.isDirectory }
        expect(true) { build.listFiles()!!.isNotEmpty() }
        build.find("*.gz", 5..10)
        build.find("*.js", 5..10)
        val tokenFile = File(testProject.dir, "build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val buildInfo: JsonObject = JsonUtil.parse(tokenFile.readText())
        expect(true, buildInfo.toJson()) { buildInfo.getBoolean(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE) }
    }

    @Test
    fun testBuildWarBuildsFrontendInProductionMode() {
        testProject.newFile("src/main/java/org/vaadin/example/MainView.java", """
            package org.vaadin.example;

            import com.vaadin.flow.component.html.Div;
            import com.vaadin.flow.component.html.Span;
            import com.vaadin.flow.router.Route;

            @Route("")
            public class MainView extends Div {

                public MainView() {
                    add(new Span("It works!"));
                }
            }
        """.trimIndent())

        val result: BuildResult = testProject.build("-Pvaadin.productionMode", "build")
        result.expectTaskSucceded("vaadinPrepareFrontend")
        result.expectTaskSucceded("vaadinBuildFrontend")
        val war = testProject.builtWar
        expect(true, "$war file doesn't exist") { war.isFile }
        // no need to check the WAR contents - this is just a smoke test class.
        // The war contents will be checked thoroughly in MiscSingleModuleTest.
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/73
     */
    @Test
    fun vaadinCleanDoesntDeletePnpmFiles() {
        val pnpmLockYaml = testProject.newFile("pnpm-lock.yaml")
        val pnpmFileJs = testProject.newFile("pnpmfile.js")
        val webpackConfigJs = testProject.newFile("webpack.config.js")
        testProject.build("vaadinClean")
        expect(false) { pnpmLockYaml.exists() }
        expect(false) { pnpmFileJs.exists() }
        // don't delete webpack.config.js: https://github.com/vaadin/vaadin-gradle-plugin/pull/74#discussion_r444457296
        expect(true) { webpackConfigJs.exists() }
    }

    /**
     * Tests that VaadinClean task removes TS-related files.
     */
    @Test
    fun vaadinCleanDeletesTsFiles() {
        val tsconfigJson = testProject.newFile("tsconfig.json")
        val typesDTs = testProject.newFile("types.d.ts")
        testProject.build("vaadinClean")
        expect(false) { tsconfigJson.exists() }
        expect(false) { typesDTs.exists() }
    }

    /**
     * Tests that VaadinClean task removes default fronted/generated directory
     */
    @Test
    fun vaadinCleanDeletesGeneratedFolder() {
        val generatedFolder = testProject.newFolder("frontend/generated")
        val generatedFile = testProject.newFile("frontend/generated/index.ts")
        testProject.build("vaadinClean")
        expect(false) { generatedFile.exists() }
        expect(false) { generatedFolder.exists() }
    }

    /**
     * Tests that VaadinClean task removes custom fronted/generated directory
     */
    @Test
    fun vaadinCleanDeletesGeneratedFolderForCustomFrontendFolder() {
        testProject.buildFile.writeText("""
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
                compile("com.vaadin:flow:$flowVersion")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                compile("org.slf4j:slf4j-simple:1.7.30")
            }
            vaadin {
                frontendDirectory = file("src/main/frontend")
            }
        """)
        val generatedFolder = testProject.newFolder("src/main/frontend/generated")
        val generatedFile = testProject.newFile("src/main/frontend/generated/index.ts")
        testProject.build("vaadinClean")
        expect(false) { generatedFile.exists() }
        expect(false) { generatedFolder.exists() }
    }

    /**
     * Tests that VaadinClean task removes custom fronted/generated directory
     */
    @Test
    fun vaadinCleanDeletesGeneratedTsFolder() {
        testProject.buildFile.writeText("""
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
                compile("com.vaadin:flow:$flowVersion")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                compile("org.slf4j:slf4j-simple:1.7.30")
            }
            vaadin {
                generatedTsFolder = file("api")
            }
        """)
        val generatedTsFolder = testProject.newFolder("api/generated")
        val generatedFile = testProject.newFile("api/generated/endpoint.ts")
        testProject.build("vaadinClean")
        expect(false) { generatedFile.exists() }
        expect(false) { generatedTsFolder.exists() }
    }

    /**
     * Tests that build works with a custom frontend directory
     */
    @Test
    fun testCustomFrontendDirectory() {
        testProject.buildFile.writeText("""
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
                compile("com.vaadin:flow:$flowVersion")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                compile("org.slf4j:slf4j-simple:1.7.30")
            }
            vaadin {
                frontendDirectory = file("src/main/frontend")
            }
        """)
        val result: BuildResult = testProject.build("vaadinPrepareFrontend")
        // let's explicitly check that vaadinPrepareFrontend has been run.
        result.expectTaskSucceded("vaadinPrepareFrontend")

        expect(false) {
            File(testProject.dir, "frontend/generated/index.ts").exists()
        }
        expect(true) {
            File(testProject.dir, "src/main/frontend/generated/index.ts").exists()
        }
    }
}
