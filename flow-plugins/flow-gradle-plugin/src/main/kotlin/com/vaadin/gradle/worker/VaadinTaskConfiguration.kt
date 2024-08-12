package com.vaadin.gradle.worker

import java.io.File
import java.io.Serializable

public data class VaadinTaskConfiguration(
    val flags: Flags,
    val locations: Locations,
    val strings: Strings,
    val classFinderClasspath: Set<File>
) : Serializable
