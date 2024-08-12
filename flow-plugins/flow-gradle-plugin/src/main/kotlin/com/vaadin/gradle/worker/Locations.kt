package com.vaadin.gradle.worker

import java.io.File
import java.io.Serializable
import java.net.URI

public data class Locations(
    val applicationProperties: File,
    val frontendDirectory: File,
    val generatedTsFolder: File,
    val jarFiles: Set<File>,
    val javaSourceFolder: File,
    val javaResourceFolder: File,
    val npmFolder: File,
    val openApiJsonFile: File,
    val projectBaseDirectory: File,
    val servletResourceOutputDirectory: File,
    val webpackOutputDirectory: File,
    val buildFolder: String,
    val frontendResourcesDirectory: File,
    val nodeDownloadRoot: URI,
    val postinstallPackages: List<String>,
) : Serializable

