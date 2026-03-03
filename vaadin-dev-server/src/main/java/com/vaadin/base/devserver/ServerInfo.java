/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /**
     * Detects if the application is running inside a container and returns the
     * container runtime name.
     *
     * @return the container runtime name (e.g. "docker", "podman",
     *         "kubernetes"), or "-" if no container is detected
     */
    public static String fetchContainerInfo() {
        // Docker creates this file inside containers
        if (Files.exists(Path.of("/.dockerenv"))) {
            return "docker";
        }

        // Podman creates this file inside containers
        if (Files.exists(Path.of("/run/.containerenv"))) {
            return "podman";
        }

        // Kubernetes sets this env var in all pods
        if (System.getenv("KUBERNETES_SERVICE_HOST") != null) {
            return "kubernetes";
        }

        // systemd-nspawn and some runtimes set the "container" env var
        String containerEnv = System.getenv("container");
        if (containerEnv != null && !containerEnv.isEmpty()) {
            return containerEnv;
        }

        // Apple Containers run each container in a lightweight VM using
        // Apple's Virtualization.framework. The device tree hypervisor node
        // identifies it with "apple" in the compatible string.
        try {
            Path hypervisorCompat = Path
                    .of("/sys/firmware/devicetree/base/hypervisor/compatible");
            if (Files.exists(hypervisorCompat)) {
                String compatible = Files.readString(hypervisorCompat).trim();
                if (compatible.contains("apple")) {
                    return "apple";
                }
            }
        } catch (IOException e) {
            // Ignore read errors
        }

        // Fall back to scanning /proc/self/cgroup for container indicators
        try {
            Path cgroupPath = Path.of("/proc/self/cgroup");
            if (Files.exists(cgroupPath)) {
                String content = Files.readString(cgroupPath);
                if (content.contains("docker")) {
                    return "docker";
                }
                if (content.contains("lxc")) {
                    return "lxc";
                }
                if (content.contains("kubepods")) {
                    return "kubernetes";
                }
            }
        } catch (IOException e) {
            // Ignore read errors
        }

        return "-";
    }

    public List<NameAndVersion> getVersions() {
        return versions;
    }

    private static boolean isVaadinAvailable() {
        return Thread.currentThread().getContextClassLoader().getResource(
                "META-INF/maven/com.vaadin/vaadin-core/pom.properties") != null;
    }
}
