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

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the Vite config file according with current project settings.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskUpdateVite implements FallibleCommand, Serializable {

    private File configFolder;
    private String buildFolder;

    Pattern frontendFilePattern = Pattern
            .compile("import flowSettings from '(.*)';");

    TaskUpdateVite(File configFolder, String buildFolder) {
        this.configFolder = configFolder;
        this.buildFolder = buildFolder;
    }

    @Override
    public void execute() {
        try {
            createConfig();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void createConfig() throws IOException {
        File configFile = new File(configFolder, FrontendUtils.VITE_CONFIG);

        if (!configFile.exists()) {
            URL resource = this.getClass().getClassLoader()
                    .getResource(FrontendUtils.VITE_CONFIG);
            String template = IOUtils.toString(resource,
                    StandardCharsets.UTF_8);

            template = updateSettings(template);
            FileUtils.write(configFile, template, StandardCharsets.UTF_8);
            log().debug("Created vite configuration file: '{}'", configFile);
        } else {
            String template = IOUtils.toString(configFile.toURI(),
                    StandardCharsets.UTF_8);

            String updatedTemplate = updateSettings(template);

            if (!template.equals(updatedTemplate)) {
                FileUtils.write(configFile, updatedTemplate,
                        StandardCharsets.UTF_8);
                log().debug("Updated vite configuration settings path: '{}'",
                        configFile);
            }

        }
    }

    private String updateSettings(String template) {
        final Matcher matcher = frontendFilePattern.matcher(template);
        if (matcher.find() && !matcher.group(1)
                .equals("./" + buildFolder + "/flow-settings.json")) {
            template = template.replace(
                    "import flowSettings from '" + matcher.group(1) + "';",
                    "import flowSettings from './" + buildFolder
                            + "/flow-settings.json';");
        }
        return template;
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
