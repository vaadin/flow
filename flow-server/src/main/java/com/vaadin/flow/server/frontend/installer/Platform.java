/*
 * Copyright 2000-2020 Vaadin Ltd.
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

/**
 * Platform contains information about system architecture and OS.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 *
 * @since
 */
public class Platform {

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

    enum OS {
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

    private final OS os;
    private final Architecture architecture;

    /**
     * Construct a new Platform.
     *
     * @param os
     *            platform OS
     * @param architecture
     *            platform Architecture
     */
    public Platform(OS os, Architecture architecture) {
        this.os = os;
        this.architecture = architecture;
    }

    /**
     * Create a Platform and figure out OS and Architecture.
     *
     * @return platform instance
     */
    public static Platform guess() {
        OS os = OS.guess();
        Architecture architecture = Architecture.guess();
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
     * @return true is mac
     */
    public boolean isMac() {
        return os == OS.MAC;
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
     * @return platform node classifier
     */
    public String getNodeClassifier() {
        return getCodename() + "-" + getArchitecture().getName();
    }
}
