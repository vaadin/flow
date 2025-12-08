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

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.expect
import com.vaadin.flow.internal.JacksonUtils
import com.vaadin.flow.internal.StringUtil
import com.vaadin.flow.server.InitParameters
import com.vaadin.flow.server.frontend.FrontendUtils
import tools.jackson.databind.JsonNode
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
                eagerServerLoad = false // test the vaadin{} block by changing some innocent property with limited side-effect
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
        val buildInfo: JsonNode = JacksonUtils.readTree(tokenFile.readText())
        expect(false, buildInfo.toString()) { buildInfo.get(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE).booleanValue() }
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
        val buildInfo: JsonNode = JacksonUtils.readTree(tokenFile.readText())
        expect(true, buildInfo.toString()) { buildInfo.get(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE).booleanValue() }
        expect("app-" + StringUtil.getHash(testProject.dir.name,
            java.nio.charset.StandardCharsets.UTF_8
        ), buildInfo.toString()) { buildInfo.get(InitParameters.APPLICATION_IDENTIFIER).textValue() }
    }

    @Test
    fun testBuildFrontendInProductionMode_customApplicationIdentifier() {
        val result: BuildResult = testProject.build("-Pvaadin.applicationIdentifier=MY_APP_ID", "-Pvaadin.productionMode", "vaadinBuildFrontend", debug = true)
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        // let's explicitly check that vaadinPrepareFrontend has been run
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val tokenFile = File(testProject.dir, "build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val buildInfo: JsonNode = JacksonUtils.readTree(tokenFile.readText())
        expect("MY_APP_ID", buildInfo.toString()) { buildInfo.get(InitParameters.APPLICATION_IDENTIFIER).textValue() }
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
                id 'com.vaadin.flow'
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
                eagerServerLoad = false // test the vaadin{} block by changing some innocent property with limited side-effect
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
                id 'com.vaadin.flow'
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

        val result: BuildResult = testProject.build("-Pvaadin.productionMode", "build", debug = true)
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
    fun testPrepareFrontend_configurationCache() {
        // Create frontend folder, that will otherwise be created by the first
        // execution of vaadinPrepareFrontend, invalidating the cache on the
        // second run
        testProject.newFolder("src/main/frontend")

        val result = testProject.build("--configuration-cache", "vaadinPrepareFrontend")
        result.expectTaskSucceded("vaadinPrepareFrontend")
        assertContains(result.output, "Calculating task graph as no cached configuration is available for tasks: vaadinPrepareFrontend")
        assertContains(result.output, "Configuration cache entry stored")

        val result2 = testProject.build("--configuration-cache", "vaadinPrepareFrontend", checkTasksSuccessful = false)
        result2.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.UP_TO_DATE)
        assertContains(result2.output, "Reusing configuration cache")
    }

    @Test
    fun testPrepareFrontend_configurationCache_configurationChange_cacheInvalidated() {
        // Create frontend folder, that will otherwise be created by the first
        // execution of vaadinPrepareFrontend, invalidating the cache on the
        // second run
        testProject.newFolder("src/main/frontend")

        val result = testProject.build("--configuration-cache", "vaadinPrepareFrontend")
        result.expectTaskSucceded("vaadinPrepareFrontend")
        assertContains(result.output, "Calculating task graph as no cached configuration is available for tasks: vaadinPrepareFrontend")
        assertContains(result.output, "Configuration cache entry stored")

        val buildFile = testProject.buildFile.readText()
            .replace("eagerServerLoad = false", "eagerServerLoad = true")
        testProject.buildFile.writeText(buildFile)

        val result2 = testProject.build("--configuration-cache", "vaadinPrepareFrontend", checkTasksSuccessful = false)
        result2.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.SUCCESS)
        assertContains(result.output, "Calculating task graph as no cached configuration is available for tasks: vaadinPrepareFrontend")
    }

    @Test
    fun testPrepareFrontend_configurationCache_gradlePropertyChange_cacheInvalidated() {
        // Create frontend folder, that will otherwise be created by the first
        // execution of vaadinPrepareFrontend, invalidating the cache on the
        // second run
        testProject.newFolder("src/main/frontend")

        val result = testProject.build("--configuration-cache", "vaadinPrepareFrontend")
        result.expectTaskSucceded("vaadinPrepareFrontend")
        assertContains(result.output, "Calculating task graph as no cached configuration is available for tasks: vaadinPrepareFrontend")
        assertContains(result.output, "Configuration cache entry stored")

        val result2 = testProject.build("--configuration-cache", "vaadinPrepareFrontend", "-Pvaadin.eagerServerLoad=true", checkTasksSuccessful = false)
        result2.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.SUCCESS)
        assertContains(result.output, "Calculating task graph as no cached configuration is available for tasks: vaadinPrepareFrontend")
    }

    @Test
    fun testPrepareFrontend_configurationCache_systemPropertyChange_cacheInvalidated() {
        // Create frontend folder, that will otherwise be created by the first
        // execution of vaadinPrepareFrontend, invalidating the cache on the
        // second run
        testProject.newFolder("src/main/frontend")

        val result = testProject.build("--configuration-cache", "vaadinPrepareFrontend")
        result.expectTaskSucceded("vaadinPrepareFrontend")
        assertContains(result.output, "Calculating task graph as no cached configuration is available for tasks: vaadinPrepareFrontend")
        assertContains(result.output, "Configuration cache entry stored")

        val result2 = testProject.build("--configuration-cache", "vaadinPrepareFrontend", "-Dvaadin.eagerServerLoad=true", checkTasksSuccessful = false)
        result2.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.SUCCESS)
        assertContains(result.output, "Calculating task graph as no cached configuration is available for tasks: vaadinPrepareFrontend")
    }

    // When Hilla is detected, frontend hot deploy should be automatically
    // enabled and as a consequence prepare frontend task should create react
    // related files
    @Test
    fun vaadinPrepareFrontend_hillaAvailable_frontendHotDeployEnabled() {
        // Setup a project local maven repo to simulate the presence of Hilla
        val fakeHillaVersion = "0.0.0.localtest";
        val projectLocalMavenRepo = testProject.newFolder("libs").absoluteFile
        val hillaEndpointFolder =
            projectLocalMavenRepo.toPath() / "com" / "vaadin" / "hilla-endpoint" / fakeHillaVersion
        Files.createDirectories(hillaEndpointFolder)

        // hilla-endpoint-stub.jar contains only stub for com.vaadin.hilla.EndpointController.class
        val hillaEndpointJar =
            hillaEndpointFolder / "hilla-endpoint-${fakeHillaVersion}.jar"
        Files.copy(
            File(javaClass.classLoader.getResource("hilla-endpoint-stub.jar").path).toPath(),
            hillaEndpointJar, StandardCopyOption.REPLACE_EXISTING
        )
        val hillaEndpointPom =
            hillaEndpointFolder / "hilla-endpoint-${fakeHillaVersion}.pom"
        hillaEndpointPom.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd"
                xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.vaadin</groupId>
                <artifactId>hilla-endpoint</artifactId>
                <version>${fakeHillaVersion}</version>
                <name>Fake Hilla Endpoint</name>
            </project>
            """.trimIndent()
        )
        enableHilla()

        testProject.buildFile.writeText(
            """
            plugins {
                id 'war'
                id 'com.vaadin.flow'
            }
            repositories {
                maven {
                    url = '${projectLocalMavenRepo.toURI().toURL()}'
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
                implementation("com.vaadin:hilla-endpoint:${fakeHillaVersion}")
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            }
            vaadin {
            }
        """.trimIndent()
        )

        val flowTsx = File(
            testProject.dir,
            FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "flow/Flow.tsx"
        )
        val vaadinReactTsx = File(
            testProject.dir,
            FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "vaadin-react.tsx"
        )

        testProject.build("vaadinPrepareFrontend")
        expect(
            true,
            "Expected Flow.tsx to be created when Hilla is available"
        ) { flowTsx.exists() }
        expect(
            true,
            "Expected vaadin-react.tsx to be created when Hilla is available"
        ) { vaadinReactTsx.exists() }
    }

    // When Hilla is not available, frontend hot deploy should not be enabled
    // by default and react related files should not be created
    @Test
    fun vaadinPrepareFrontend_hillaNotAvailable_frontendHotDeployNotEnabled() {
        val flowTsx = File(
            testProject.dir,
            FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "flow/Flow.tsx"
        )
        val vaadinReactTsx = File(
            testProject.dir,
            FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "vaadin-react.tsx"
        )

        testProject.build("vaadinPrepareFrontend")
        expect(
            false,
            "Expected Flow.tsx not to be created when Hilla is not available"
        ) { flowTsx.exists() }
        expect(
            false,
            "Expected vaadin-react.tsx not to be created when Hilla is not available"
        ) { vaadinReactTsx.exists() }
    }

    private fun enableHilla() {
        testProject.newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR)
        testProject.newFile(FrontendUtils.DEFAULT_FRONTEND_DIR + "index.ts")
    }
}
