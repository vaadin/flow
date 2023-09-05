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

import java.io.File
import kotlin.test.assertContains
import kotlin.test.expect
import com.vaadin.flow.server.InitParameters
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Test

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
                implementation("com.vaadin:flow:$flowVersion")
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
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
    fun testBuildFrontendInProductionMode() {
        val result: BuildResult = testProject.build("-Pvaadin.productionMode", "vaadinBuildFrontend")
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        // let's explicitly check that vaadinPrepareFrontend has been run
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProject.dir, "build/resources/main/META-INF/VAADIN/webapp/VAADIN/build")
        expect(true, build.toString()) { build.isDirectory }
        expect(true) { build.listFiles()!!.isNotEmpty() }
        build.find("*.br", 4..10)
        build.find("*.js", 4..10)
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
     * Tests that VaadinClean task removes default fronted/generated directory
     */
    @Test
    fun vaadinPrepareFrontendDeletesFrontendGeneratedFolder() {
        val generatedFolder = testProject.newFolder("frontend/generated")
        val generatedFile = testProject.newFile("frontend/generated/index.ts")
        val generatedFlowFolder = testProject.newFolder("frontend/generated/flow")
        val generatedOldFlowFile = testProject.newFolder("frontend/generated/flow/extra.js")
        testProject.build("vaadinPrepareFrontend")
        expect(true) { generatedFolder.exists() }
        expect(false) { generatedFile.exists() }
        expect(false) { generatedOldFlowFile.exists() }
        expect(false) { generatedFlowFolder.exists() }
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
                implementation("com.vaadin:flow:$flowVersion")
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
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
                implementation("com.vaadin:flow:$flowVersion")
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
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
                implementation("com.vaadin:flow:$flowVersion")
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            }
            vaadin {
                frontendDirectory = file("src/main/frontend")
            }
        """)
        // let's explicitly check that vaadinPrepareFrontend has been run.
        val result: BuildResult = testProject.build("-Pvaadin.productionMode", "build")
        result.expectTaskSucceded("vaadinPrepareFrontend")
        result.expectTaskSucceded("vaadinBuildFrontend")

        expect(false) {
            File(testProject.dir, "frontend/generated/index.ts").exists()
        }
        expect(true) {
            // Only generated for executing project or building bundle
            File(testProject.dir, "src/main/frontend/generated/index.ts").exists()
        }
    }

    /**
     * Tests that build works with resources from classpath, not only from
     * frontend directory
     *
     * https://github.com/vaadin/flow/issues/14420
     */
    @Test
    fun vaadinBuildFrontendShouldScanForResourcesOnClasspath() {
        testProject.newFile("src/main/java/org/vaadin/example/MainView.java", """
            package org.vaadin.example;

            import com.vaadin.flow.component.dependency.CssImport;
            import com.vaadin.flow.component.dependency.NpmPackage;
            import com.vaadin.flow.component.html.Div;
            import com.vaadin.flow.component.html.Span;
            import com.vaadin.flow.router.Route;

            @Route("")
            @CssImport("./mystyle.css")
            @NpmPackage(value = "@vaadin/vaadin-themable-mixin", version = "24.0.0")
            public class MainView extends Div {

                public MainView() {
                    add(new Span("It works!"));
                }
            }
        """.trimIndent())
        testProject.newFile("src/main/resources/META-INF/resources/frontend/mystyle.css", """
            #testme {
                background-color: red;
                width: 100px;
                height: 100px;
                display: block;
            }
        """.trimIndent())

        val result: BuildResult = testProject.build("-Pvaadin.productionMode", "build")
        result.expectTaskSucceded("vaadinPrepareFrontend")
        result.expectTaskSucceded("vaadinBuildFrontend")

        val cssFile = File(testProject.dir, "frontend/generated/jar-resources/mystyle.css")
        expect(true, cssFile.toString()) { cssFile.exists() }

    }

    @Test
    fun pluginShouldFailWithUnsupportedGradleVersion() {

        fun setupProjectForGradleVersion(version: String) {
            testProject.delete()
            testProject = TestProject(version)
            setup()
        }

        if(JavaVersion.current().majorVersion.toInt() >= 20) {
            // JDK 20 needs 8.3+
            setupProjectForGradleVersion("8.3")
            val result = testProject.build("vaadinClean")
            result.expectTaskSucceded("vaadinClean")
        } else {
            // Works with supported versions
            for (supportedVersion in arrayOf(VaadinPlugin.GRADLE_MINIMUM_SUPPORTED_VERSION, "7.6.2", "8.1", "8.2", "8.3") ) {
                setupProjectForGradleVersion(supportedVersion)
                val result = testProject.build("vaadinClean")
                result.expectTaskSucceded("vaadinClean")
            }
        }

        // Cannot test versions older than 7.6 because of Java version
        // incompatibilities with dependencies thar makes the build fail before
        // the plugin is applied
        // Code below is left here for future usage, when gradle supported
        // version will be greater than 7.6
        // emptyArray<String>() should be replaced by arrayOf("7.6")
        for (unsupportedVersion in emptyArray<String>()) {
            setupProjectForGradleVersion(unsupportedVersion)
            val result = testProject.buildAndFail("vaadinClean")
            assertContains(
                result.output,
                "requires Gradle ${VaadinPlugin.GRADLE_MINIMUM_SUPPORTED_VERSION} or later",
                true,
                "Expecting plugin execution to fail for version ${unsupportedVersion} " +
                        "as it is lower than the supported one (${VaadinPlugin.GRADLE_MINIMUM_SUPPORTED_VERSION})"
            )
            assertContains(
                result.output,
                "current version is ${unsupportedVersion}"
            )
        }
    }

}
