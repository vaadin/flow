package com.vaadin.gradle.worker

import org.gradle.workers.ProcessWorkerSpec
import javax.inject.Inject

internal open class InheritEnvironmentVariablesProcessConfigurer @Inject constructor() : ProcessWorkerSpecConfigurer {

    override fun execute(spec: ProcessWorkerSpec) {
        spec.forkOptions.environment(getEnvironment())
    }

    private fun getEnvironment(): MutableMap<String, String>? {
        return System.getenv()
    }

    companion object
}