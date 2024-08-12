package com.vaadin.gradle.worker

import org.gradle.workers.ProcessWorkerSpec
import javax.inject.Inject

private const val COM_VAADIN_GRADLE_WORKER_DEBUG = "com.vaadin.gradle.worker.debug"

internal open class DebugFlagProcessConfigurer @Inject constructor() : ProcessWorkerSpecConfigurer {

    override fun execute(spec: ProcessWorkerSpec) {
        if (System.getProperty(COM_VAADIN_GRADLE_WORKER_DEBUG)?.toBoolean() == true) {
            spec.forkOptions.debugOptions {
                it.enabled.set(true)
                it.server.set(true)
                it.suspend.set(true)
            }
        }
    }

    companion object
}