/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;

/**
 * Provides information about the current version of Vaadin Platform.
 *
 * @since 23.0
 */
public class Platform implements Serializable {

    private static boolean versionErrorLogged = false;

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
}
