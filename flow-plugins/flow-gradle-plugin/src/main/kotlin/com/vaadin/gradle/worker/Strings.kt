package com.vaadin.gradle.worker

import java.io.Serializable

public data class Strings(
    val applicationIdentifier: String,
    val nodeVersion: String,
    val projectName: String,
) : Serializable

