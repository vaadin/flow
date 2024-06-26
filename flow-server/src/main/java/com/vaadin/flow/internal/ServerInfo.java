/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Version;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Data for a info message to the debug window.
 */
public class ServerInfo implements DebugWindowData {

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
                .getResourceAsStream(Constants.VAADIN_CORE_VERSIONS_JSON)) {
            if (vaadinVersionsStream != null) {
                JsonObject vaadinVersions = Json.parse(IOUtils.toString(
                        vaadinVersionsStream, StandardCharsets.UTF_8));
                return vaadinVersions.get("platform").asString();
            } else {
                LoggerFactory.getLogger(getClass()).info(
                        "Unable to determine version information. No {} found",
                        Constants.VAADIN_CORE_VERSIONS_JSON);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass())
                    .error("Unable to determine version information", e);
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

    public String toJson() {
        return String.format(
                "{\"flowVersion\": \"%s\", \"vaadinVersion\": \"%s\", \"javaVersion\": \"%s\", \"osVersion\": \"%s\"}",
                flowVersion, vaadinVersion, javaVersion, osVersion);
    }
}
