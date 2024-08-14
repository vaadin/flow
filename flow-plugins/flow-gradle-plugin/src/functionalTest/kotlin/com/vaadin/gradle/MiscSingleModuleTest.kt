/**
 *    Copyright 2000-2024 Vaadin Ltd
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
import elemental.json.Json
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File
import kotlin.test.expect
import org.junit.Ignore

class MiscSingleModuleTest : AbstractGradleTest() {

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
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
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
        val tokenFile = File(testProject.dir, "build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val tokenFileContent = Json.parse(tokenFile.readText())
        expect(testProject.dir.name) { tokenFileContent.getString(InitParameters.APPLICATION_IDENTIFIER) }
    }

    @Test
    fun testWarProjectProductionModeWithCustomName() {
        testProject.settingsFile.writeText("rootProject.name = 'my-test-project'")
        doTestWarProjectProductionMode()
        val tokenFile = File(testProject.dir, "build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val tokenFileContent = Json.parse(tokenFile.readText())
        expect("my-test-project") { tokenFileContent.getString(InitParameters.APPLICATION_IDENTIFIER) }
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
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
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
            def jettyVersion = "11.0.12"
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
                implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")

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
            def jettyVersion = "11.0.12"
            vaadin {
                nodeAutoUpdate = true // test the vaadin{} block by changing some innocent property with limited side-effect
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
                implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")

                implementation("org.eclipse.jetty:jetty-server:${"$"}{jettyVersion}")
                implementation("org.eclipse.jetty.websocket:websocket-jakarta-server:${"$"}{jettyVersion}")
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

        val springBootVersion = "3.0.0"

        testProject.settingsFile.writeText(
            """
            pluginManagement {
                repositories {
                  maven { url 'https://repo.spring.io/milestone' }
                  gradlePluginPortal()
                }
              }
            """
        )
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
                maven { url 'https://repo.spring.io/milestone' }
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
        doTestCircularDepsBug()
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/42
     */
    @Ignore("Webpack uses gzip compression")
    @Test
    fun testCircularDepsBugWebpack() {
        doTestCircularDepsBug("*.gz")
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
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
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
        val result = testProject.build("vaadinPrepareFrontend")
        expect(false) { result.output.contains("org.reflections.ReflectionsException") }
    }

    @Test
    fun prepareFrontendIncrementalBuilds_featureEnabled() {
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
        """)
        var result = testProject.build("vaadinPrepareFrontend")
        expect(true) { result.output.contains(
            "Task ':vaadinPrepareFrontend' is not up-to-date") }

        result = testProject.build("vaadinPrepareFrontend", checkTasksSuccessful = false)
        result.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.UP_TO_DATE)
        println("Caching: " + result.output)
        expect(true) { result.output.contains(
            "Skipping task ':vaadinPrepareFrontend' as it is up-to-date") }

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
                frontendHotdeploy = true
            }
        """)
        result = testProject.build("vaadinPrepareFrontend")
        println("Caching: " + result.output)
        expect(true) { result.output.contains(
            "Task ':vaadinPrepareFrontend' is not up-to-date") }
    }

    @Test
    fun prepareFrontendIncrementalBuilds_disableWithProperty() {
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
                providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            }
            vaadin {
                alwaysExecutePrepareFrontend = true
            }
        """
        )
        repeat(5) {
            val result = testProject.build("vaadinPrepareFrontend")
            expect(true) {
                result.output.contains(
                    "Task ':vaadinPrepareFrontend' is not up-to-date"
                )
            }
        }
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
        expect(listOf("flow-client-", "flow-html-components-", "flow-lit-template-", "flow-push-", "flow-react-", "flow-server-", "flow-webpush-")) {
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
                id 'org.springframework.boot' version '3.0.0'
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

    /**
     * Tests https://github.com/vaadin/flow/issues/17665
     */
    @Test
    fun testEagerTaskInstantiationWontFail() {
        testProject.buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'com.vaadin'
            }
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion")
                implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
            }
            tasks.whenTaskAdded {} // reproduces #17665
        """.trimIndent()
        )

        testProject.build("build")
    }

    /**
     * Tests https://github.com/vaadin/flow/issues/18572
     */
    @Test
    fun testPluginEffectiveConfiguration() {
        testProject.buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'com.vaadin'
            }
            repositories {
                mavenLocal()
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                implementation("com.vaadin:flow:$flowVersion") {
                    println("!!!effective1.productionMode=" + vaadin.effective.productionMode.get() + "!!!")
                    afterEvaluate {
                        println("!!!cfg2.productionMode=" + vaadin.productionMode.get() + "!!!")
                        println("!!!effective2.productionMode=" + vaadin.effective.productionMode.get() + "!!!")
                    }
                }
            }
        """.trimIndent()
        )

        val buildResult = testProject.build("assemble", "-Pvaadin.productionMode")
        expect(true, buildResult.output) { buildResult.output.contains("!!!cfg2.productionMode=false!!!") }
        expect(true, buildResult.output) { buildResult.output.contains("!!!effective1.productionMode=true!!!") }
        expect(true, buildResult.output) { buildResult.output.contains("!!!effective2.productionMode=true!!!") }
    }
}
