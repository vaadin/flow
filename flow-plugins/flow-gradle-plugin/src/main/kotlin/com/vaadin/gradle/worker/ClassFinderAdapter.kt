package com.vaadin.gradle.worker

import com.vaadin.flow.plugin.base.BuildFrontendUtil
import com.vaadin.flow.server.frontend.scanner.ClassFinder
import com.vaadin.gradle.toPrettyFormat
import org.gradle.api.logging.Logger
import java.io.File

internal class ClassFinderAdapter(
    private val classFinderClasspath: Set<File>,
    private val logger: Logger,
    private val projectName: String
) {

    internal fun getClassFinder(): ClassFinder {
        val classFinder = BuildFrontendUtil.getClassFinder(classFinderClasspath.map { it.absolutePath })

        // sanity check that the project has flow-server.jar as a dependency
        try {
            classFinder.loadClass<Any>("com.vaadin.flow.server.webcomponent.WebComponentModulesWriter")
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Failed to find classes from flow-server.jar. The project '${projectName}' needs to have a dependency on flow-server.jar")
        }

        logger.info("Passing this classpath to NodeTasks.Builder: ${classFinderClasspath.toPrettyFormat()}")

        return classFinder
    }
}
