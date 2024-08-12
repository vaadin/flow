package com.vaadin.gradle.worker

import com.vaadin.gradle.PluginEffectiveConfiguration
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.workers.ProcessWorkerSpec

internal fun GradleWorkerApiAdapter.Companion.from(
    taskConfiguration: VaadinTaskConfiguration,
    logger: Logger
): GradleWorkerApiAdapter {

    return GradleWorkerApiAdapter(
        taskConfiguration,
        ClassFinderAdapter(taskConfiguration.classFinderClasspath, logger, taskConfiguration.strings.projectName),
        LoggerAdapter(logger),
    )
}

internal fun VaadinTaskConfigurationFactory.Companion.from(
    project: Project,
    config: PluginEffectiveConfiguration,
    isBeforeProcessResources: Boolean
): VaadinTaskConfigurationFactory {

    return VaadinTaskConfigurationFactory(
        FlagsFactory(project, config),
        LocationsFactory(project, config, isBeforeProcessResources),
        StringsFactory(project, config),
        ClassFinderClasspathFactory(project, config),
    )
}


internal fun JavaExecutionService.Companion.from(objectFactory: ObjectFactory): JavaExecutionService {
    return objectFactory.newInstance(
        JavaExecutionService::class.java,
        ProcessWorkerSpecConfigurer.chainFrom(
            DebugFlagProcessConfigurer.from(objectFactory),
            InheritEnvironmentVariablesProcessConfigurer.from(objectFactory),
            ToolchainJavaExecutableProcessConfigurer.from(objectFactory),
        )
    )
}


internal fun ProcessWorkerSpecConfigurer.Companion.chainFrom(vararg configurer: ProcessWorkerSpecConfigurer): ProcessWorkerSpecConfigurer {
    return object : ProcessWorkerSpecConfigurer {
        override fun execute(spec: ProcessWorkerSpec) {
            configurer.forEach { it.execute(spec) }
        }
    }
}

internal fun InheritEnvironmentVariablesProcessConfigurer.Companion.from(objectFactory: ObjectFactory): InheritEnvironmentVariablesProcessConfigurer {
    return objectFactory.newInstance(InheritEnvironmentVariablesProcessConfigurer::class.java)
}

internal fun ToolchainJavaExecutableProcessConfigurer.Companion.from(objectFactory: ObjectFactory): ToolchainJavaExecutableProcessConfigurer {
    return objectFactory.newInstance(
        ToolchainJavaExecutableProcessConfigurer::class.java,
        JavaToolchainSpecFactory.from(objectFactory)
    )
}

internal fun DebugFlagProcessConfigurer.Companion.from(objectFactory: ObjectFactory): DebugFlagProcessConfigurer {
    return objectFactory.newInstance(DebugFlagProcessConfigurer::class.java)
}

internal fun JavaToolchainSpecFactory.Companion.from(objectFactory: ObjectFactory): JavaToolchainSpecFactory {
    return objectFactory.newInstance(JavaToolchainSpecFactory::class.java)
}
