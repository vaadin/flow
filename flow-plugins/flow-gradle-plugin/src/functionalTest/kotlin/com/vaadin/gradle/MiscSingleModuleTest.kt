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
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File
import kotlin.test.expect
import org.junit.Ignore

class MiscSingleModuleTest : AbstractGradleTest() {
    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/26
     */
    @Ignore("The devsoap plugin does not work with Gradle 7")
    @Test
    fun testVaadin8VaadinPlatformMPRProject() {
        testProject.buildFile.writeText(
                """
            plugins {
                id "com.devsoap.plugin.vaadin" version "1.4.1"
                id 'com.vaadin'
            }
            repositories {
                mavenLocal()
                mavenCentral()
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
                id 'org.gretty' version '4.0.3'
                id("com.vaadin")
            }
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
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
        doTestWarProjectProductionMode()
    }

    /**
     * This test covers the [Base Starter Gradle](https://github.com/vaadin/base-starter-gradle)
     * example project.
     */
    @Ignore("Webpack uses gzip compression")
    @Test
    fun testWarProjectProductionModeWebpack() {
        doTestWarProjectProductionMode("*.gz")
    }

    fun doTestWarProjectProductionMode(compressedExtension: String = "*.br") {
        testProject.buildFile.writeText(
                """
            plugins {
                id 'war'
                id 'org.gretty' version '4.0.3'
                id("com.vaadin")
            }
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            }
        """.trimIndent()
        )

        val build: BuildResult =
                testProject.build("-Pvaadin.productionMode", "build")
        // vaadinBuildFrontend should have been executed automatically
        build.expectTaskSucceded("vaadinBuildFrontend")

        val war: File = testProject.builtWar
        expectArchiveContainsVaadinBundle(war, false, compressedExtension)
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
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            def jettyVersion = "10.0.8"
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
                implementation("javax.servlet:javax.servlet-api:3.1.0")

                implementation("org.eclipse.jetty:jetty-server:${"$"}{jettyVersion}")
                implementation("org.eclipse.jetty.websocket:websocket-jetty-server:${"$"}{jettyVersion}")
            }
        """.trimIndent()
        )

        val build: BuildResult = testProject.build("build")
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
        doTestJarProjectProductionMode()
    }

    /**
     * This test covers the https://github.com/mvysny/vaadin14-embedded-jetty-gradle example.
     */
    @Ignore("Webpack uses gzip compression")
    @Test
    fun testJarProjectProductionModeWebpack() {
        doTestJarProjectProductionMode("*.gz")
    }

    private fun doTestJarProjectProductionMode(compressedExtension: String = "*.br") {
        testProject.buildFile.writeText(
                """
            plugins {
                id 'java'
                id("com.vaadin")
            }
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            def jettyVersion = "9.4.20.v20190813"
            vaadin {
                pnpmEnable = true
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
                implementation("javax.servlet:javax.servlet-api:3.1.0")

                implementation("org.eclipse.jetty:jetty-continuation:${"$"}{jettyVersion}")
                implementation("org.eclipse.jetty:jetty-server:${"$"}{jettyVersion}")
                implementation("org.eclipse.jetty.websocket:websocket-server:${"$"}{jettyVersion}")
                implementation("org.eclipse.jetty.websocket:javax-websocket-server-impl:${"$"}{jettyVersion}") {
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
        expectArchiveContainsVaadinBundle(jar, false, compressedExtension)
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
        doTestSpringProjectProductionMode();
    }

    @Ignore("Webpack uses gzip compression")
    @Test
    fun testSpringProjectProductionModeWebpack() {
        doTestSpringProjectProductionMode("*.gz")
    }

    private fun doTestSpringProjectProductionMode(compressedExtension: String = "*.br") {

        val springBootVersion = "2.7.4"

        testProject.buildFile.writeText(
                """
            plugins {
                id 'org.springframework.boot' version '$springBootVersion'
                id 'io.spring.dependency-management' version '1.0.11.RELEASE'
                id 'java'
                id("com.vaadin")
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url 'https://maven.vaadin.com/vaadin-prereleases' }
            }

            configurations {
                developmentOnly
                runtimeClasspath {
                    extendsFrom developmentOnly
                }
            }
            
            dependencies {
                implementation('com.vaadin:flow:$flowVersion')
                implementation('com.vaadin:vaadin-spring:$flowVersion')
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

            jar {
                enabled = false // Do not build a separate "plain" jar, see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/#packaging-executable.and-plain-archives
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
        expectArchiveContainsVaadinBundle(jar, true, compressedExtension)
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/42
     */
    @Test
    fun testCircularDepsBug() {
        doTestCircularDepsBug();
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/42
     */
    @Ignore("Webpack uses gzip compression")
    @Test
    fun testCircularDepsBugWebpack() {
        doTestCircularDepsBug("*.gz");
    }

    private fun doTestCircularDepsBug(compressedExtension: String = "*.br") {

        testProject.buildFile.writeText(
                """
            plugins {
                id 'war'
                id 'org.gretty' version '4.0.3'
                id("com.vaadin")
            }
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            }
            
            sourceSets {
              guiceConfig
            }

            configurations {
              guiceConfigCompile.extendsFrom implementation
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
        expectArchiveContainsVaadinBundle(war, false, compressedExtension)
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
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
            }
        """
        )
        val result = testProject.build("vaadinPrepareFrontend", debug = true)
        expect(false) { result.output.contains("org.reflections.ReflectionsException") }
    }

    @Test
    fun testIncludeExclude() {
        testProject.buildFile.writeText("""
            plugins {
                id 'com.vaadin'
            }
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
            }
            vaadin {
                pnpmEnable = true
                filterClasspath {
                    include("com.vaadin:flow-*")
                    exclude("com.vaadin:flow-data")
                    exclude("com.vaadin:flow-dnd")
                }
            }
        """)

        val output = testProject.build("vaadinPrepareFrontend").output
        val classpathLines = output.lines().filter { it.startsWith("Passing this classpath to NodeTasks.Builder") }
        expect(1, output) { classpathLines.size }
        // parse the list of jars out of the classpath line
        val classpath = classpathLines[0].dropWhile { it != '[' } .trim('[', ']') .split(',')
            .map { it.trim() } .sorted()
        // remove version numbers to make the test more stable: drop -2.7.4.jar from flow-dnd-2.7.4.jar
        expect(listOf("flow-client-", "flow-html-components-", "flow-lit-template-", "flow-polymer-template-", "flow-push-", "flow-server-")) {
            classpath.map { it.removeSuffix("-SNAPSHOT.jar").dropLastWhile { it != '-' } }
        }
    }

    @Test
    fun testUsingNonMainSourceSet() {
        testProject.settingsFile.writeText(
            """
            pluginManagement {
                repositories {
                  gradlePluginPortal()
                }
              }
            """
        )
        testProject.buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'org.springframework.boot' version '2.7.4'
                id 'io.spring.dependency-management' version '1.0.11.RELEASE'
                id("com.vaadin")
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            
            sourceSets {
                ui {
                    java
                }
                main {
                    java {
                        compileClasspath += ui.output
                        runtimeClasspath += ui.output + ui.runtimeClasspath
                    }
                }
            }
            
            vaadin {
                productionMode = true
                sourceSetName = 'ui'
            }
            
            dependencies {
                uiImplementation('com.vaadin:flow:$flowVersion')
                implementation('org.springframework.boot:spring-boot-starter-web')
            }
            
            jar {
                enabled = false
            }
            """
        )

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

        testProject.newFile(
            "src/ui/java/com/example/demo/AppShell.java", """
            package com.example.demo;
            
            import com.vaadin.flow.component.page.AppShellConfigurator;
            import com.vaadin.flow.server.PWA;

            @PWA(name = "Demo application", shortName = "Demo")
            public class AppShell implements AppShellConfigurator {
            }
        """.trimIndent()
        )

        val build: BuildResult = testProject.build("build")
        build.expectTaskSucceded("vaadinPrepareFrontend")
        build.expectTaskSucceded("vaadinBuildFrontend")

        val jar: File = testProject.builtJar
        expectArchiveContainsVaadinBundle(jar, true)
    }
}
