/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.server.Platform;
import com.vaadin.flow.server.Version;

/**
 * Data for a info message to the debug window.
 *
 * @since 9.0
 */
public class ServerInfo implements Serializable {

    /**
     * @since 24.2.1
     */
    public record NameAndVersion(String name,
            String version) implements Serializable {
    };

    private List<NameAndVersion> versions = new ArrayList<>();

    /**
     * Creates a new instance.
     */
    public ServerInfo() {
        // The order here is the order shown in dev tools
        if (EndpointRequestUtil.isHillaAvailable()) {
            versions.add(new NameAndVersion("Hilla", fetchHillaVersion()));
        }
        versions.add(new NameAndVersion("Flow", Version.getFullVersion()));
        if (isVaadinAvailable()) {
            versions.add(new NameAndVersion("Vaadin", fetchVaadinVersion()));
        }
        versions.add(new NameAndVersion("Java", fetchJavaVersion()));
        versions.add(new NameAndVersion("OS", fetchOperatingSystem()));
    }

    public static String fetchJavaVersion() {
        String vendor = System.getProperty("java.vendor");
        String version = System.getProperty("java.version");

        return vendor + " " + version;
    }

    public static String fetchOperatingSystem() {
        String arch = System.getProperty("os.arch");
        String name = System.getProperty("os.name");
        String version = System.getProperty("os.version");

        return arch + " " + name + " " + version;
    }

    public static String fetchVaadinVersion() {
        return isVaadinAvailable() ? Platform.getVaadinVersion().orElse("?")
                : "-";
    }

    public static String fetchHillaVersion() {
        return EndpointRequestUtil.isHillaAvailable()
                ? Platform.getHillaVersion().orElse("?")
                : "-";
    }

    public List<NameAndVersion> getVersions() {
        return versions;
    }

    private static boolean isVaadinAvailable() {
        return Thread.currentThread().getContextClassLoader().getResource(
                "META-INF/maven/com.vaadin/vaadin-core/pom.properties") != null;
    }
}
