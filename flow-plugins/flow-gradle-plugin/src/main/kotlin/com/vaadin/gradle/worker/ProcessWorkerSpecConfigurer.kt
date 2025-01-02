package com.vaadin.gradle.worker

import org.gradle.api.Action
import org.gradle.workers.ProcessWorkerSpec

internal interface ProcessWorkerSpecConfigurer : Action<ProcessWorkerSpec> {
    companion object
}