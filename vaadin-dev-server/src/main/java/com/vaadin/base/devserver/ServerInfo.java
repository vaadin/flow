/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Version;

import org.slf4j.LoggerFactory;

/**
 * Data for a info message to the debug window.
 */
public class ServerInfo {

    private final String flowVersion;
    private final String vaadinVersion;
    private final String javaVersion;
    private final String osVersion;

    /**
     * Creates a new instance.
     */
    public ServerInfo() {
        this.flowVersion = Version.getFullVersion();
        this.vaadinVersion = fetchVaadinVersion();
        this.javaVersion = fetchJavaVersion();
        this.osVersion = fetchOperatingSystem();

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
        try (InputStream vaadinVersionsStream = getClass().getClassLoader()
                .getResourceAsStream(Constants.VAADIN_VERSIONS_JSON)) {
            if (vaadinVersionsStream != null) {
                ObjectMapper m = new ObjectMapper();
                JsonNode vaadinVersions = m.readTree(vaadinVersionsStream);
                return vaadinVersions.get("platform").asText();
            } else {
                LoggerFactory.getLogger(getClass())
                        .info("Unable to determine version information. No vaadin_versions.json found");
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Unable to determine version information", e);
        }

        return "?";
    }

    public String getFlowVersion() {
        return flowVersion;
    }

    public String getVaadinVersion() {
        return vaadinVersion;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getOsVersion() {
        return osVersion;
    }
}
