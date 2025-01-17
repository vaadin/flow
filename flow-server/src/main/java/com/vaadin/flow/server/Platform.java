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
package com.vaadin.flow.server;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides information about the current version of Vaadin Platform.
 *
 * @since 23.0
 */
public class Platform implements Serializable {
    static final String HILLA_POM_PROPERTIES = "META-INF/maven/com.vaadin/hilla/pom.properties";
    private static final Logger LOGGER = LoggerFactory
            .getLogger(Platform.class);
    /**
     * Memoized Hilla version. null if not yet calculated, empty string if Hilla
     * is not present on the classpath.
     */
    static String hillaVersion = null;

    /**
     * Memoized Vaadin version. null if not yet calculated, empty string if
     * Vaadin is not present on the classpath.
     */
    static String vaadinVersion = null;

    /**
     * Returns the platform version string, e.g., {@code "23.0.0"}.
     *
     * @return the platform version or {@link Optional#empty()} if unavailable.
     */
    public static Optional<String> getVaadinVersion() {
        // thread-safe: in the worst case vaadinVersion may be computed multiple
        // times by concurrent threads. Unsafe-publish is OK since String is
        // immutable and thread-safe.
        if (vaadinVersion == null) {
            try (final InputStream vaadinPomProperties = Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream(
                            "META-INF/maven/com.vaadin/vaadin-core/pom.properties")) {
                if (vaadinPomProperties != null) {
                    final Properties properties = new Properties();
                    properties.load(vaadinPomProperties);
                    vaadinVersion = properties.getProperty("version", "");
                } else {
                    LOGGER.info("Unable to determine Vaadin version. "
                            + "No META-INF/maven/com.vaadin/vaadin-core/pom.properties found");
                    vaadinVersion = "";
                }
            } catch (Exception e) {
                LOGGER.error("Unable to determine Vaadin version", e);
                vaadinVersion = "";
            }
        }

        return vaadinVersion.isEmpty() ? Optional.empty()
                : Optional.of(vaadinVersion);
    }

    /**
     * Returns Hilla version.
     *
     * @return Hilla version if Hilla is on the classpath; empty Optional if
     *         Hilla is not on the classpath.
     */
    public static Optional<String> getHillaVersion() {
        // thread-safe: in the worst case hillaVersion may be computed multiple
        // times by concurrent threads. Unsafe-publish is OK since String is
        // immutable and thread-safe.
        if (hillaVersion == null) {
            try (final InputStream hillaPomProperties = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(HILLA_POM_PROPERTIES)) {
                if (hillaPomProperties != null) {
                    final Properties properties = new Properties();
                    properties.load(hillaPomProperties);
                    hillaVersion = properties.getProperty("version", "");
                } else {
                    LOGGER.info("Unable to determine Hilla version. "
                            + "No {} found", HILLA_POM_PROPERTIES);
                    hillaVersion = "";
                }
            } catch (Exception e) {
                LOGGER.error("Unable to determine Hilla version", e);
                hillaVersion = "";
            }
        }
        return hillaVersion.isEmpty() ? Optional.empty()
                : Optional.of(hillaVersion);
    }
}
