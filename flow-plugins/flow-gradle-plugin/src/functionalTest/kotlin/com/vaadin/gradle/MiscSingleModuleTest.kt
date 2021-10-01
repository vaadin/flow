/**
 *    Copyright 2000-2021 Vaadin Ltd
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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File
import kotlin.test.expect

class MiscSingleModuleTest : AbstractGradleTest() {
    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/26
     */
    @Test
    fun testVaadin8VaadinPlatformMPRProject() {
        testProject.buildFile.writeText(
            """
            plugins {
                id "com.devsoap.plugin.vaadin" version "1.4.1"
                id 'com.vaadin'
            }
            repositories {
                mavenCentral()
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            // test that we can configure both plugins
            vaadin {
                version = "8.9.4"
            }
            vaadinPlatform {
                optimizeBundle = true
            }
        """.trimIndent()
        )

        // the collision between devsoap's `vaadin` extension and com.vaadin's `vaadin`
        // extension would crash even this very simple build.
        testProject.build("tasks")
    }

    /**
     * This test covers the [Base Starter Gradle](https://github.com/vaadin/base-starter-gradle)
     * example project.
     */
    @Test
    fun testWarProjectDevelopmentMode() {
        testProject.buildFile.writeText(
            """
            plugins {
                id 'war'
                id 'org.gretty' version '3.0.1'
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                compile("com.vaadin:flow:$flowVersion")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                compile("org.slf4j:slf4j-simple:1.7.30")
            }
        """.trimIndent()
        )

        val build: BuildResult = testProject.build("build")
        // vaadinBuildFrontend should NOT have been executed automatically
        build.expectTaskNotRan("vaadinBuildFrontend")

        val war: File = testProject.builtWar
        expectArchiveDoesntContainVaadinWebpackBundle(war, false)
    }

    /**
     * This test covers the [Base Starter Gradle](https://github.com/vaadin/base-starter-gradle)
     * example project.
     */
    @Test
    fun testWarProjectProductionMode() {
        testProject.buildFile.writeText(
            """
            plugins {
                id 'war'
                id 'org.gretty' version '3.0.1'
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                compile("com.vaadin:flow:$flowVersion")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                compile("org.slf4j:slf4j-simple:1.7.30")
            }
        """.trimIndent()
        )

        val build: BuildResult =
            testProject.build("-Pvaadin.productionMode", "build")
        // vaadinBuildFrontend should have been executed automatically
        build.expectTaskSucceded("vaadinBuildFrontend")

        val war: File = testProject.builtWar
        expectArchiveContainsVaadinWebpackBundle(war, false)
    }

    /**
     * This test covers the https://github.com/mvysny/vaadin14-embedded-jetty-gradle example.
     */
    @Test
    fun testJarProjectDevelopmentMode() {
        testProject.buildFile.writeText(
            """
            plugins {
                id 'java'
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            def jettyVersion = "9.4.20.v20190813"
            dependencies {
                compile("com.vaadin:flow:$flowVersion")
                compile("org.slf4j:slf4j-simple:1.7.30")
                compile("javax.servlet:javax.servlet-api:3.1.0")

                compile("org.eclipse.jetty:jetty-continuation:${"$"}{jettyVersion}")
                compile("org.eclipse.jetty:jetty-server:${"$"}{jettyVersion}")
                compile("org.eclipse.jetty.websocket:websocket-server:${"$"}{jettyVersion}")
                compile("org.eclipse.jetty.websocket:javax-websocket-server-impl:${"$"}{jettyVersion}") {
                    exclude(module: "javax.websocket-client-api")
                }
            }
        """.trimIndent()
        )

        val build: BuildResult = testProject.build( "build")
        // vaadinBuildFrontend should NOT have been executed automatically
        expect(null) { build.task(":vaadinBuildFrontend") }

        val jar: File = testProject.builtJar
        expectArchiveDoesntContainVaadinWebpackBundle(jar, false)
    }

    /**
     * This test covers the https://github.com/mvysny/vaadin14-embedded-jetty-gradle example.
     */
    @Test
    fun testJarProjectProductionMode() {
        testProject.buildFile.writeText(
            """
            plugins {
                id 'java'
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            def jettyVersion = "9.4.20.v20190813"
            vaadin {
                pnpmEnable = true
            }
            dependencies {
                compile("com.vaadin:flow:$flowVersion")
                compile("org.slf4j:slf4j-simple:1.7.30")
                compile("javax.servlet:javax.servlet-api:3.1.0")

                compile("org.eclipse.jetty:jetty-continuation:${"$"}{jettyVersion}")
                compile("org.eclipse.jetty:jetty-server:${"$"}{jettyVersion}")
                compile("org.eclipse.jetty.websocket:websocket-server:${"$"}{jettyVersion}")
                compile("org.eclipse.jetty.websocket:javax-websocket-server-impl:${"$"}{jettyVersion}") {
                    exclude(module: "javax.websocket-client-api")
                }
            }
        """.trimIndent()
        )

        val build: BuildResult =
            testProject.build("-Pvaadin.productionMode", "build")
        build.expectTaskSucceded("vaadinPrepareFrontend")
        build.expectTaskSucceded("vaadinBuildFrontend")

        val jar: File = testProject.builtJar
        expectArchiveContainsVaadinWebpackBundle(jar, false)
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/24
     *
     * The `implementation()` dependency type would cause incorrect jar list computation,
     * which would then not populate the `node_modules/@vaadin/flow-frontend` folder,
     * which would cause webpack to fail during vaadinBuildFrontend.
     *
     * This build script covers the [Spring Boot example](https://github.com/vaadin/base-starter-spring-gradle)
     */
    @Test
    fun testSpringProjectProductionMode() {

        val springBootVersion = "2.2.4.RELEASE"
        val springVersion = "17.0-SNAPSHOT"

        testProject.buildFile.writeText(
            """
            plugins {
                id 'org.springframework.boot' version '$springBootVersion'
                id 'io.spring.dependency-management' version '1.0.9.RELEASE'
                id 'java'
                id("com.vaadin")
            }
            
            repositories {
                mavenCentral()
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }

            configurations {
                developmentOnly
                runtimeClasspath {
                    extendsFrom developmentOnly
                }
            }
            
            dependencies {
                implementation('com.vaadin:flow:$flowVersion')
                implementation('com.vaadin:vaadin-spring:$springVersion')
                implementation('org.springframework.boot:spring-boot-starter-web:$springBootVersion')
                developmentOnly 'org.springframework.boot:spring-boot-devtools'
                testImplementation('org.springframework.boot:spring-boot-starter-test') {
                    exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
                }
            }
            
            dependencyManagement {
                imports {
                    mavenBom "com.vaadin:flow:$flowVersion"
                }
            }
        """
        )

        // need to create the Application.java file otherwise bootJar will fail
        testProject.newFile(
            "src/main/java/com/example/demo/DemoApplication.java", """
            package com.example.demo;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            @SpringBootApplication
            public class DemoApplication {
            
                public static void main(String[] args) {
                    SpringApplication.run(DemoApplication.class, args);
                }
            
            }
        """.trimIndent()
        )

        // AppShell.java file creation
        testProject.newFile(
            "src/main/java/com/example/demo/AppShell.java", """
            package com.example.demo;
            
            import com.vaadin.flow.component.page.AppShellConfigurator;
            import com.vaadin.flow.server.PWA;

            @PWA(name = "Demo application", shortName = "Demo")
            public class AppShell implements AppShellConfigurator {
            }
        """.trimIndent()
        )

        val build: BuildResult =
            testProject.build("-Pvaadin.productionMode", "build")
        build.expectTaskSucceded("vaadinPrepareFrontend")
        build.expectTaskSucceded("vaadinBuildFrontend")

        val jar: File = testProject.builtJar
        expectArchiveContainsVaadinWebpackBundle(jar, true)
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/42
     */
    @Test
    fun testCircularDepsBug() {
        testProject.buildFile.writeText(
            """
            plugins {
                id 'war'
                id 'org.gretty' version '3.0.1'
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                compile("com.vaadin:flow:$flowVersion")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                compile("org.slf4j:slf4j-simple:1.7.30")
            }
            
            sourceSets {
              guiceConfig
            }

            configurations {
              guiceConfigCompile.extendsFrom compile
            }

            dependencies {
              // This seems to be a problem with the vaadin-gradle-plugin, but we need this
              // to have access to classes of the main sourceSet in the guice sourceSet.
              guiceConfigCompile sourceSets.main.output
            }

            compileGuiceConfigJava {
              options.compilerArgs << "-Xlint:all"
              options.compilerArgs << "-Xlint:-serial"
            }

            jar {
              from sourceSets.guiceConfig.output
            }
        """.trimIndent()
        )

        val build: BuildResult =
            testProject.build("-Pvaadin.productionMode", "build")
        build.expectTaskSucceded("vaadinPrepareFrontend")
        build.expectTaskSucceded("vaadinBuildFrontend")

        val war: File = testProject.builtWar
        expectArchiveContainsVaadinWebpackBundle(war, false)
    }

    /**
     * https://github.com/vaadin/vaadin-gradle-plugin/issues/76
     */
    @Test
    fun testNodeDownload() {

        testProject.buildFile.writeText(
            """
            plugins {
                id 'com.vaadin'
            }
            repositories {
                mavenCentral()
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                compile("com.vaadin:flow:$flowVersion")
            }
            vaadin {
                requireHomeNodeExec = true
                nodeVersion = "v12.10.0"
                nodeDownloadRoot = "http://localhost:8080/non-existent"
            }
        """
        )


        val result: BuildResult

        // Vaadin downloads the node to ${user.home}/.vaadin.
        // Set user.home to be the testProject root directory
        val originalUserHome = System.getProperty("user.home")
        System.setProperty("user.home", testProject.dir.absolutePath)

        try {
            result = testProject.buildAndFail("vaadinPrepareFrontend",
                    "-Duser.home=${testProject.dir.absolutePath}")
        } finally {
            // Return original user home value
            System.setProperty("user.home", originalUserHome)
        }
        // the task should fail to download the node.js
        result.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.FAILED)
        expect(true, result.output) {
            result.output.contains("Could not download http://localhost:8080/v12.10.0/")
        }
        expect(true, result.output) {
            result.output.contains("Could not download Node.js")
        }
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/99
     */
    @Test
    fun testReflectionsException() {
        testProject.buildFile.writeText(
            """
            plugins {
                id 'com.vaadin'
            }
            repositories {
                mavenCentral()
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                compile("com.vaadin:flow:$flowVersion")
            }
        """
        )
        val result = testProject.build("vaadinPrepareFrontend", debug = true)
        expect(false) { result.output.contains("org.reflections.ReflectionsException") }
    }
}
