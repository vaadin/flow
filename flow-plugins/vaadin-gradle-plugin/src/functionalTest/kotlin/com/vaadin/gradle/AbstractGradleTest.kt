package com.vaadin.gradle

import org.junit.After
import org.junit.Before
import java.io.File

/**
 * Prepares a test Gradle project - creates a temp dir for the [testProject] and allow you to run gradle
 * tasks. See [TestProject] for more details.
 * @author mavi
 */
abstract class AbstractGradleTest {

    // find the newest Vaadin version here: https://github.com/vaadin/platform/tags
    val vaadinVersion = "20.0.0.alpha3"

    /**
     * The testing Gradle project. Automatically deleted after every test.
     * Don't use TemporaryFolder JUnit `@Rule` since it will always delete the folder afterwards,
     * making it impossible to investigate the folder in case of failure.
     */
    lateinit var testProject: TestProject

    @Before
    fun createTestProjectFolder() {
        testProject = TestProject()
    }

    @After
    fun deleteTestProjectFolder() {
        // comment out if a test is failing and you need to investigate the project files.
        testProject.delete()
    }

    @Before
    fun dumpEnvironment() {
        println("Test project directory: ${testProject.dir}")
    }
}
