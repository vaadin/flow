/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.frontend;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.FallibleCommand;

/**
 * Abstract class for Vaadin Fusion related generators.
 */
abstract class AbstractTaskFusionGenerator implements FallibleCommand {
    private final File applicationProperties;

    AbstractTaskFusionGenerator(File applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    protected Properties readApplicationProperties() {
        Properties config = new Properties();

        if (applicationProperties != null && applicationProperties.exists()) {
            try (BufferedReader bufferedReader = Files.newBufferedReader(
                    applicationProperties.toPath(), StandardCharsets.UTF_8)) {
                config.load(bufferedReader);
            } catch (IOException e) {
                log().info(String.format(
                        "Can't read the application"
                                + ".properties file from %s",
                        applicationProperties.toString()), e);
            }
        } else {
            log().debug(
                    "Found no application properties, using default values.");
        }
        return config;
    }

    Logger log() {
        return LoggerFactory.getLogger(AbstractTaskFusionGenerator.class);
    }
}
