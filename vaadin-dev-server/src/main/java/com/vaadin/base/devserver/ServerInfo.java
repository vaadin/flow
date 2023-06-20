/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.util.LinkedHashSet;
import java.util.Set;

import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.server.Platform;
import com.vaadin.flow.server.Version;

/**
 * Data for a info message to the debug window.
 */
public class ServerInfo implements Serializable {

    private final String flowVersion;
    private final String vaadinVersion;
    private final String javaVersion;
    private final String osVersion;
    private final String productName;
    private final String hillaVersion;

    /**
     * Creates a new instance.
     */
    public ServerInfo() {
        this.flowVersion = Version.getFullVersion();
        this.vaadinVersion = fetchVaadinVersion();
        this.javaVersion = fetchJavaVersion();
        this.osVersion = fetchOperatingSystem();
        this.productName = fetchProductName();
        this.hillaVersion = fetchHillaVersion();
    }

    private String fetchJavaVersion() {
        String vendor = System.getProperty("java.vendor");
        String version = System.getProperty("java.version");

        return vendor + " " + version;
    }

    private String fetchOperatingSystem() {
        String arch = System.getProperty("os.arch");
        String name = System.getProperty("os.name");
        String version = System.getProperty("os.version");

        return arch + " " + name + " " + version;
    }

    private String fetchVaadinVersion() {
        return isVaadinAvailable() ? Platform.getVaadinVersion().orElse("?")
                : "-";
    }

    private String fetchHillaVersion() {
        return EndpointRequestUtil.isHillaAvailable()
                ? Platform.getHillaVersion().orElse("?")
                : "-";
    }

    private String fetchProductName() {
        final Set<String> result = new LinkedHashSet<>();
        if (isVaadinAvailable()) {
            result.add("Vaadin");
        }
        if (EndpointRequestUtil.isHillaAvailable()) {
            result.add("Hilla");
        }
        return String.join(",", result);
    }

    public String getFlowVersion() {
        return flowVersion;
    }

    public String getVaadinVersion() {
        return vaadinVersion;
    }

    public String getHillaVersion() {
        return hillaVersion;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getOsVersion() {
        return osVersion;
    }

    /**
     * Returns the product name.
     *
     * @return the product name, one of "Vaadin", "Hilla" or "Vaadin,Hilla".
     */
    public String getProductName() {
        return productName;
    }

    private static boolean isVaadinAvailable() {
        return Thread.currentThread().getContextClassLoader().getResource(
                "META-INF/maven/com.vaadin/vaadin-core/pom.properties") != null;
    }
}
