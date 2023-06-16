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
package com.vaadin.flow.server;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helger.commons.annotation.VisibleForTesting;
import org.slf4j.LoggerFactory;

/**
 * Provides information about the current version of Vaadin Platform.
 *
 * @since 23.0
 */
public class Platform implements Serializable {

    private static boolean versionErrorLogged = false;

    /**
     * Memoized hilla version.
     */
    @VisibleForTesting
    static String hillaVersion = null;

    /**
     * Returns the platform version string, e.g., {@code "23.0.0"}.
     *
     * @return the platform version or {@link Optional#empty()} if unavailable.
     */
    public static Optional<String> getVaadinVersion() {
        try (InputStream vaadinVersionsStream = Platform.class.getClassLoader()
                .getResourceAsStream(Constants.VAADIN_CORE_VERSIONS_JSON)) {
            if (vaadinVersionsStream != null) {
                ObjectMapper m = new ObjectMapper();
                JsonNode vaadinVersions = m.readTree(vaadinVersionsStream);
                return Optional.of(vaadinVersions.get("platform").asText());
            } else {
                if (!versionErrorLogged) {
                    versionErrorLogged = true;
                    LoggerFactory.getLogger(Platform.class)
                            .info("Unable to determine version information. "
                                    + "No vaadin-core-versions.json found");
                }
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Platform.class)
                    .error("Unable to determine version information", e);
        }

        return Optional.empty();
    }

    /**
     * Returns Hilla version.
     *
     * @return Hilla version if Hilla is on the classpath; empty Optional if
     *         Hilla is not on the classpath.
     */
    public static Optional<String> getHillaVersion() {
        // thread-safe: in the worst case hillaVersion may be computed multiple
        // times by concurrent threads.
        if (hillaVersion == null) {
            try (final InputStream hillaPomProperties = Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream(
                            "META-INF/maven/dev.hilla/hilla/pom.properties")) {
                if (hillaPomProperties != null) {
                    final Properties properties = new Properties();
                    properties.load(hillaPomProperties);
                    hillaVersion = properties.getProperty("version", "");
                } else {
                    hillaVersion = "";
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(Platform.class)
                        .error("Unable to determine Hilla version", e);
                hillaVersion = "";
            }
        }
        return hillaVersion.isEmpty() ? Optional.empty()
                : Optional.of(hillaVersion);
    }
}
