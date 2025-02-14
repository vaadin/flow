package com.vaadin.gradle.worker

import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.workers.ProcessWorkerSpec
import javax.inject.Inject

internal open class ToolchainJavaExecutableProcessConfigurer @Inject constructor(
    private val toolchainService: JavaToolchainService,
    private val toolchainSpecSupplier: JavaToolchainSpecFactory,
) : ProcessWorkerSpecConfigurer {

    override fun execute(spec: ProcessWorkerSpec) {
        val javaExeAbsolutePath = toolchainService.launcherFor(toolchainSpecSupplier.get())
            .map { it.executablePath.asFile.absolutePath }
            .get()

        spec.forkOptions.executable(javaExeAbsolutePath)
    }

    internal companion object
}