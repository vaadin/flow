package com.vaadin.gradle.worker

import com.vaadin.flow.plugin.base.PluginAdapterBuild
import com.vaadin.flow.server.frontend.scanner.ClassFinder
import java.io.File
import java.net.URI
import java.nio.file.Path

internal class GradleWorkerApiAdapter(
    taskConfiguration: VaadinTaskConfiguration,
    private val classFinderAdapter: ClassFinderAdapter,
    private val loggerAdapter: LoggerAdapter,
) : PluginAdapterBuild {

    private val flags = taskConfiguration.flags
    private val locations = taskConfiguration.locations
    private val strings = taskConfiguration.strings

    //    flags
    override fun eagerServerLoad(): Boolean = flags.eagerServerLoad
    override fun isJarProject(): Boolean = flags.jarProject
    override fun isDebugEnabled(): Boolean = flags.debugEnabled
    override fun nodeAutoUpdate(): Boolean = flags.nodeAutoUpdate
    override fun pnpmEnable(): Boolean = flags.pnpmEnable
    override fun bunEnable(): Boolean = flags.bunEnable
    override fun useGlobalPnpm(): Boolean = flags.useGlobalPnpm
    override fun requireHomeNodeExec(): Boolean = flags.requireHomeNodeExec
    override fun isFrontendHotdeploy(): Boolean = flags.frontendHotdeploy
    override fun skipDevBundleBuild(): Boolean = flags.skipDevBundleBuild
    override fun isPrepareFrontendCacheDisabled(): Boolean = flags.prepareFrontendCacheDisabled
    override fun isReactEnabled(): Boolean = flags.reactEnabled
    override fun generateBundle(): Boolean = flags.generateBundle
    override fun generateEmbeddableWebComponents(): Boolean = flags.generateEmbeddableWebComponents
    override fun optimizeBundle(): Boolean = flags.optimizeBundle
    override fun runNpmInstall(): Boolean = flags.runNpmInstall
    override fun ciBuild(): Boolean = flags.ciBuild
    override fun forceProductionBuild(): Boolean = flags.forceProductionBuild
    override fun compressBundle(): Boolean = flags.compressBundle

    //    locations
    override fun applicationProperties(): File = locations.applicationProperties
    override fun frontendDirectory(): File = locations.frontendDirectory
    override fun generatedTsFolder(): File = locations.generatedTsFolder
    override fun getJarFiles(): Set<File> = locations.jarFiles
    override fun javaSourceFolder(): File = locations.javaSourceFolder
    override fun javaResourceFolder(): File = locations.javaResourceFolder
    override fun npmFolder(): File = locations.npmFolder
    override fun openApiJsonFile(): File = locations.openApiJsonFile
    override fun projectBaseDirectory(): Path = locations.projectBaseDirectory.toPath()
    override fun servletResourceOutputDirectory(): File = locations.servletResourceOutputDirectory
    override fun webpackOutputDirectory(): File = locations.webpackOutputDirectory
    override fun buildFolder(): String = locations.buildFolder
    override fun frontendResourcesDirectory(): File = locations.frontendResourcesDirectory
    override fun nodeDownloadRoot(): URI = locations.nodeDownloadRoot
    override fun postinstallPackages(): List<String> = locations.postinstallPackages

    //    strings
    override fun nodeVersion(): String = strings.nodeVersion
    override fun applicationIdentifier(): String = strings.applicationIdentifier

    //    classLoading
    override fun getClassFinder(): ClassFinder = classFinderAdapter.getClassFinder()

    //    logging 
    override fun logDebug(p0: CharSequence?): Unit = loggerAdapter.logDebug(p0)
    override fun logDebug(p0: CharSequence?, p1: Throwable?): Unit = loggerAdapter.logDebug(p0, p1)
    override fun logInfo(p0: CharSequence?): Unit = loggerAdapter.logInfo(p0)
    override fun logWarn(p0: CharSequence?): Unit = loggerAdapter.logWarn(p0)
    override fun logWarn(p0: CharSequence?, p1: Throwable?): Unit = loggerAdapter.logWarn(p0, p1)
    override fun logError(p0: CharSequence?): Unit = loggerAdapter.logError(p0)
    override fun logError(p0: CharSequence?, p1: Throwable?): Unit = loggerAdapter.logError(p0, p1)

    public companion object
}
