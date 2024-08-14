package com.vaadin.gradle

import org.junit.experimental.categories.Category

@Category(GradleWorkerApiTestCategory::class)
class JvmToolchainsMiscMultiModuleTest : MiscMultiModuleTest() {
    override fun extraGradleDSL(): String {
        return """
             java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
        """.trimIndent()
    }
}