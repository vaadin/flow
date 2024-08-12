package com.vaadin.gradle.worker

import com.vaadin.gradle.worker.ProcessWorkerSpecConfigurer
import org.gradle.api.Action
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * This service executes worker actions in an isolated JVM with respect of Gradle JVM toolchains configuration.
 */
internal open class JavaExecutionService @Inject constructor(
    executor: WorkerExecutor,
    processConfigurer: ProcessWorkerSpecConfigurer,
) {
    private val queue = executor.processIsolation(processConfigurer)

    /**
     * Submits a given work action for an execution.
     */
    internal fun <T : WorkParameters?> submit(actionClass: Class<out WorkAction<T>>, parameterAction: Action<in T>) {
        queue.submit(actionClass, parameterAction)
    }

    companion object
}