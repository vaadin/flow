/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
 */
public class ServerInfo implements Serializable {

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
