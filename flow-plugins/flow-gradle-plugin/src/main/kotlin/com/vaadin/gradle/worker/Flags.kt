package com.vaadin.gradle.worker

import java.io.Serializable

public data class Flags(
    val eagerServerLoad: Boolean,
    val jarProject: Boolean,
    val debugEnabled: Boolean,
    val nodeAutoUpdate: Boolean,
    val pnpmEnable: Boolean,
    val bunEnable: Boolean,
    val useGlobalPnpm: Boolean,
    val requireHomeNodeExec: Boolean,
    val frontendHotdeploy: Boolean,
    val skipDevBundleBuild: Boolean,
    val prepareFrontendCacheDisabled: Boolean,
    val reactEnabled: Boolean,
    val generateBundle: Boolean,
    val generateEmbeddableWebComponents: Boolean,
    val optimizeBundle: Boolean,
    val runNpmInstall: Boolean,
    val ciBuild: Boolean,
    val forceProductionBuild: Boolean,
    val compressBundle: Boolean,
    val cleanFrontendFiles: Boolean,
) : Serializable