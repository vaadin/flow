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
package com.vaadin.flow.server.frontend.installer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.vaadin.flow.server.frontend.FrontendVersion;

/**
 * Platform contains information about system architecture and OS.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public class Platform {
    
    public static final String DEFAULT_NODEJS_DOWNLOAD_ROOT = "https://nodejs.org/dist/";

    public static final String UNOFFICIAL_NODEJS_DOWNLOAD_ROOT = "https://unofficial-builds.nodejs.org/download/release/";

    enum Architecture {
        X86, X64, PPC64LE, S390X, ARM64, ARMV7L;

        /**
         * Try to get the system architecture by system properties.
         *
         * @return system architecture
         */
        public static Architecture guess() {
            String arch = System.getProperty("os.arch");
            String version = System.getProperty("os.version");

            if ("ppc64le".equals(arch)) {
                return PPC64LE;
            } else if ("aarch64".equals(arch)) {
                return ARM64;
            } else if ("s390x".equals(arch)) {
                return S390X;
            } else if ("arm".equals(arch) && version.contains("v7")) {
                return ARMV7L;
            } else {
                return arch.contains("64") ? X64 : X86;
            }
        }

        /**
         * Get the architecture name.
         *
         * @return formatted architecture name
         */
        public String getName() {
            return this.name().toLowerCase();
        }
    }

    public enum OS {
        WINDOWS, MAC, LINUX, SUN_OS;

        /**
         * Use system property to figure out the operating system.
         *
         * @return current operating system
         */
        public static OS guess() {
            final String osName = System.getProperty("os.name");
            return osName.contains("Windows") ? OS.WINDOWS
                    : osName.contains("Mac") ? OS.MAC
                            : osName.contains("SunOS") ? OS.SUN_OS : OS.LINUX;
        }

        /**
         * Get the compressed archive extension for this OS.
         *
         * @return archive extension used for OS
         */
        public String getArchiveExtension() {
            if (this == OS.WINDOWS) {
                return "zip";
            } else {
                return "tar.gz";
            }
        }

        /**
         * Get the codename for this OS.
         *
         * @return codename for os
         */
        public String getCodename() {
            if (this == OS.MAC) {
                return "darwin";
            } else if (this == OS.WINDOWS) {
                return "win";
            } else if (this == OS.SUN_OS) {
                return "sunos";
            } else {
                return "linux";
            }
        }
    }

    private final String nodeDownloadRoot;
    private final OS os;
    private final Architecture architecture;
    private final String classifier;

    // Node.js supports Apple silicon from v16.0.0
    private static final int NODE_VERSION_THRESHOLD_MAC_ARM64 = 16;
    public static final String ALPINE_RELEASE_FILE_PATH = "/etc/alpine-release";

    /**
     * Construct a new Platform.
     *
     * @param os
     *            platform OS
     * @param architecture
     *            platform Architecture
     */
    public Platform(OS os, Architecture architecture) {
        this(DEFAULT_NODEJS_DOWNLOAD_ROOT, os, architecture, null);
    }

    public Platform(String nodeDownloadRoot, OS os, Architecture architecture,
            String classifier) {
        this.nodeDownloadRoot = nodeDownloadRoot;
        this.os = os;
        this.architecture = architecture;
        this.classifier = classifier;
    }

    /**
     * Create a Platform and figure out OS and Architecture.
     *
     * @return platform instance
     */
    public static Platform guess() {
        OS os = OS.guess();
        Architecture architecture = Architecture.guess();
        // The default libc is glibc, but Alpine uses musl. When not default,
        // the nodejs download
        // (and path within it) needs a classifier in the suffix (ex. -musl).
        // We know Alpine is in use if the release file exists, and this is the
        // simplest check.
        Path alpineReleaseFilePath = Paths.get(ALPINE_RELEASE_FILE_PATH);
        if (os == OS.LINUX && Files.exists(alpineReleaseFilePath)) {
            return new Platform(
                    // Currently, musl is Experimental. The download root can be
                    // overridden with config
                    // if this changes and there's not been an update to this
                    // project, yet.
                    // See
                    // https://github.com/nodejs/node/blob/master/BUILDING.md#platform-list
                    UNOFFICIAL_NODEJS_DOWNLOAD_ROOT, os, architecture, "musl");
        }
        return new Platform(os, architecture);
    }

    /**
     * Get the archive extension used with this platform.
     *
     * @return archive extension
     */
    public String getArchiveExtension() {
        return os.getArchiveExtension();
    }

    /**
     * Get the codename used with this Platform.
     *
     * @return codename
     */
    public String getCodename() {
        return os.getCodename();
    }

    /**
     * Check if platform is windows.
     *
     * @return true if windows
     */
    public boolean isWindows() {
        return os == OS.WINDOWS;
    }

    /**
     * Check if platform is mac.
     *
     * @return true if mac
     */
    public boolean isMac() {
        return os == OS.MAC;
    }

    /**
     * Check if platform is linux.
     *
     * @return true if linux
     */
    public boolean isLinux() {
        return os == OS.LINUX;
    }

    /**
     * Get platform architecture.
     *
     * @return architecture
     */
    public Architecture getArchitecture() {
        return architecture;
    }

    /**
     * Get platform OS.
     *
     * @return os
     */
    public OS getOs() {
        return os;
    }

    /**
     * Get the node classifier for current platform.
     *
     * @param nodeVersion
     *            node version to get classifier for
     * @return platform node classifier
     */
    public String getNodeClassifier(FrontendVersion nodeVersion) {
        String result = getCodename() + "-"
                + resolveArchitecture(nodeVersion).getName();
        return classifier != null ? result + "-" + classifier : result;
    }

    /**
     * Gets the platform dependent download root.
     *
     * @return platform download root
     */
    public String getNodeDownloadRoot() {
        return nodeDownloadRoot;
    }

    private Architecture resolveArchitecture(FrontendVersion nodeVersion) {
        if (isMac() && architecture == Architecture.ARM64) {
            Integer nodeMajorVersion = nodeVersion.getMajorVersion();
            if (nodeMajorVersion == null
                    || nodeMajorVersion < NODE_VERSION_THRESHOLD_MAC_ARM64) {
                return Architecture.X64;
            }
        }

        return architecture;
    }
}
