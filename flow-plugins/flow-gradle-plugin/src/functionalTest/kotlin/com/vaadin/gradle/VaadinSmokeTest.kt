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
import com.vaadin.flow.server.frontend.FrontendUtils
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.StandardCopyOption


/**
 * The most basic tests. If these fail, the plugin is completely broken and all
 * other test classes will possibly fail as well.
 * @author mavi
 */
open class VaadinSmokeTest : AbstractGradleTest() {
    @Before
    open fun setup() {
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
                nodeAutoUpdate = true // test the vaadin{} block by changing some innocent property with limited side-effect
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
        expect(testProject.dir.name, buildInfo.toJson()) { buildInfo.getString(InitParameters.APPLICATION_IDENTIFIER) }
    }

    @Test
    fun testBuildFrontendInProductionMode_customApplicationIdentifier() {
        val result: BuildResult = testProject.build("-Pvaadin.applicationIdentifier=MY_APP_ID", "-Pvaadin.productionMode", "vaadinBuildFrontend")
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        // let's explicitly check that vaadinPrepareFrontend has been run
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val tokenFile = File(testProject.dir, "build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val buildInfo: JsonObject = JsonUtil.parse(tokenFile.readText())
        expect("MY_APP_ID", buildInfo.toJson()) { buildInfo.getString(InitParameters.APPLICATION_IDENTIFIER) }
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
        val generatedFolder = testProject.newFolder(FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR)
        val generatedFile = testProject.newFile(FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "index.ts")
        val generatedFlowFolder = testProject.newFolder(FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "flow")
        val generatedOldFlowFile = testProject.newFolder(FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "/flow/extra.js")
        testProject.build("vaadinPrepareFrontend")
        expect(true) { generatedFolder.exists() }
        expect(false) { generatedFile.exists() }
        expect(false) { generatedOldFlowFile.exists() }
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/73
     */
    @Test
    fun vaadinCleanDoesntDeletePnpmFiles() {
        val pnpmLockYaml = testProject.newFile("pnpm-lock.yaml")
        val pnpmFileCjs = testProject.newFile(".pnpmfile.cjs")
        val webpackConfigJs = testProject.newFile("webpack.config.js")
        testProject.build("vaadinClean")
        expect(false) { pnpmLockYaml.exists() }
        expect(false) { pnpmFileCjs.exists() }
        // don't delete webpack.config.js: https://github.com/vaadin/vaadin-gradle-plugin/pull/74#discussion_r444457296
        expect(true) { webpackConfigJs.exists() }
    }

    /**
     * Tests that VaadinClean task removes default fronted/generated directory
     */
    @Test
    fun vaadinCleanDeletesGeneratedFolder() {
        val generatedFolder = testProject.newFolder(FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR)
        val generatedFile = testProject.newFile(FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "index.ts")
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

    @Test
    fun vaadinCleanShouldRemoveNodeModulesAndPackageLock() {
        val nodeModules: File = testProject.newFolder(FrontendUtils.NODE_MODULES)
        val packageLock: File = testProject.newFile("package-lock.json")
        expect(true) { nodeModules.exists() }
        expect(true) { packageLock.exists() }
        testProject.build("vaadinClean")
        expect(false) { nodeModules.exists() }
        expect(false) { packageLock.exists() }
    }

    @Test
    fun vaadinCleanShouldNotRemoveNodeModulesAndPackageLockWithHilla() {
        testProject.buildFile.writeText("""
            plugins {
                id 'war'
                id 'com.vaadin'
            }
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
                flatDir {
                   dirs("libs")
                }
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                implementation name:'hilla-endpoint-stub'
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            }
            vaadin {
                nodeAutoUpdate = true // test the vaadin{} block by changing some innocent property with limited side-effect
            }
        """)
        testProject.newFolder("libs")
        // hilla-endpoint-stub.jar contains only stub for com.vaadin.hilla.EndpointController.class
        val hillaEndpointJar: File = testProject.newFile("libs/hilla-endpoint-stub.jar")
        Files.copy(
                File(javaClass.classLoader.getResource("hilla-endpoint-stub.jar").path).toPath(),
                hillaEndpointJar.toPath(),  StandardCopyOption.REPLACE_EXISTING)
        enableHilla()
        val nodeModules: File = testProject.newFolder(FrontendUtils.NODE_MODULES)
        val packageLock: File = testProject.newFile("package-lock.json")
        expect(true) { nodeModules.exists() }
        expect(true) { packageLock.exists() }
        testProject.build("vaadinClean")
        expect(true) { nodeModules.exists() }
        expect(true) { packageLock.exists() }
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
            File(testProject.dir, FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "index.ts").exists()
        }
        expect(true) {
            // Only generated for executing project or building bundle
            File(testProject.dir, FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "index.tsx").exists()
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

        val cssFile = File(testProject.dir, FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "jar-resources/mystyle.css")
        expect(true, cssFile.toString()) { cssFile.exists() }

    }

    /**
     * Tests that build works with resources from classpath, not only from
     * frontend directory
     *
     * https://github.com/vaadin/flow/issues/14420
     */
    @Test
    fun vaadinBuildFrontendShouldScanFilesystemDependencies() {
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
                implementation(files('libs/addon.jar'))
            }
        """
        )

        testProject.newFile(
            "src/main/java/org/vaadin/example/MainView.java", """
            package org.vaadin.example;

            import com.vaadin.flow.component.html.Div;
            import com.vaadin.flow.component.html.Span;
            import com.vaadin.flow.router.Route;
            import com.vaadin.example.Addon;

            @Route("")
            public class MainView extends Div {

                public MainView() {
                    add(new Addon());
                }
            }
        """.trimIndent()
        )

        testProject.newFolder("libs")
        val addonJar: File = testProject.newFile("libs/addon.jar")
        Files.copy(
            File(javaClass.classLoader.getResource("addon.jar").path).toPath(),
            addonJar.toPath(), StandardCopyOption.REPLACE_EXISTING
        )

        val result: BuildResult = testProject.build("-Pvaadin.productionMode", "build")
        result.expectTaskSucceded("vaadinPrepareFrontend")
        result.expectTaskSucceded("vaadinBuildFrontend")

        val addonFile =
            File(testProject.dir, FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "jar-resources/my-addon.js")
        expect(true, addonFile.toString()) { addonFile.exists() }

        val importsFile = File(
            testProject.dir,
            FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "flow/" + FrontendUtils.IMPORTS_NAME
        )
        expect(true, importsFile.toString()) { importsFile.exists() }
        expect(true, "Addon javascript should be found and imported") {
            importsFile.readText().contains("import 'Frontend/generated/jar-resources/my-addon.js'")
        }
    }

    @Test
    fun pluginShouldFailWithUnsupportedGradleVersion() {

        fun setupProjectForGradleVersion(version: String) {
            testProject.delete()
            testProject = TestProject(version)
            setup()
        }

        for (supportedVersion in arrayOf(VaadinPlugin.GRADLE_MINIMUM_SUPPORTED_VERSION, "8.5", "8.6") ) {
                setupProjectForGradleVersion(supportedVersion)
                val result = testProject.build("vaadinClean")
                result.expectTaskSucceded("vaadinClean")
        }

        for (unsupportedVersion in arrayOf("8.3")) {
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

    private fun enableHilla() {
        testProject.newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR)
        testProject.newFile(FrontendUtils.DEFAULT_FRONTEND_DIR + "index.ts")
    }
}
