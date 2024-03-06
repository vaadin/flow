/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the Vite configuration files according with current project settings.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskUpdateVite implements FallibleCommand, Serializable {

    private File configFolder;
    private String buildFolder;

    TaskUpdateVite(File configFolder, String buildFolder) {
        this.configFolder = configFolder;
        this.buildFolder = buildFolder;
    }

    @Override
    public void execute() {
        try {
            createConfig();
            createGeneratedConfig();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void createConfig() throws IOException {
        // Only create it if it does not exist
        File configFile = new File(configFolder, FrontendUtils.VITE_CONFIG);
        if (configFile.exists()) {
            return;
        }

        URL resource = this.getClass().getClassLoader()
                .getResource(FrontendUtils.VITE_CONFIG);
        String template = IOUtils.toString(resource, StandardCharsets.UTF_8);
        FileUtils.write(configFile, template, StandardCharsets.UTF_8);
        log().debug("Created vite configuration file: '{}'", configFile);

    }

    private void createGeneratedConfig() throws IOException {
        // Always overwrite this
        File generatedConfigFile = new File(configFolder,
                FrontendUtils.VITE_GENERATED_CONFIG);
        URL resource = this.getClass().getClassLoader()
                .getResource(FrontendUtils.VITE_GENERATED_CONFIG);
        String template = IOUtils.toString(resource, StandardCharsets.UTF_8);

        template = template
                .replace("#settingsImport#",
                        "./" + buildFolder + "/"
                                + TaskUpdateSettingsFile.DEV_SETTINGS_FILE)
                .replace("#buildFolder#", "./" + buildFolder);
        FileUtils.write(generatedConfigFile, template, StandardCharsets.UTF_8);
        log().debug("Created vite generated configuration file: '{}'",
                generatedConfigFile);
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
