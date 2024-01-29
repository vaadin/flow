/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import java.util.Set;

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

    private final Options options;

    private final Set<String> webComponentTags;
    private static final String[] reactPluginTemplatesUsedInStarters = new String[] {
            getSimplifiedTemplate("vite.config-react.ts"),
            getSimplifiedTemplate("vite.config-react-swc.ts") };

    TaskUpdateVite(Options options, Set<String> webComponentTags) {
        this.options = options;
        this.webComponentTags = webComponentTags;
    }

    private static String getSimplifiedTemplate(String string) {
        return simplifyTemplate(getTemplate(string));
    }

    private static String getTemplate(String string) {
        try {
            return IOUtils.toString(
                    TaskUpdateVite.class.getResourceAsStream(string),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String simplifyTemplate(String text) {
        return text.replace("\n", "").replace("\r", "").replace("\t", "")
                .replace(" ", "");
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
        File configFile = new File(options.getNpmFolder(),
                FrontendUtils.VITE_CONFIG);
        if (configFile.exists()) {
            if (!replaceWithDefault(configFile)) {
                return;
            }
            log().info(
                    "Replacing vite.config.ts with the default version as the React plugin is now automatically included");
        }

        URL resource = this.getClass().getClassLoader()
                .getResource(FrontendUtils.VITE_CONFIG);
        String template = IOUtils.toString(resource, StandardCharsets.UTF_8);
        FileUtils.write(configFile, template, StandardCharsets.UTF_8);
        log().debug("Created vite configuration file: '{}'", configFile);

    }

    private boolean replaceWithDefault(File configFile) throws IOException {
        String text = simplifyTemplate(
                IOUtils.toString(configFile.toURI(), StandardCharsets.UTF_8));
        for (String template : reactPluginTemplatesUsedInStarters) {
            if (text.equals(template)) {
                return true;
            }
        }
        return false;
    }

    private void createGeneratedConfig() throws IOException {
        // Always overwrite this
        File generatedConfigFile = new File(options.getNpmFolder(),
                FrontendUtils.VITE_GENERATED_CONFIG);
        URL resource = this.getClass().getClassLoader()
                .getResource(FrontendUtils.VITE_GENERATED_CONFIG);
        String template = IOUtils.toString(resource, StandardCharsets.UTF_8);

        template = template
                .replace("#settingsImport#",
                        "./" + options.getBuildDirectoryName() + "/"
                                + TaskUpdateSettingsFile.DEV_SETTINGS_FILE)
                .replace("#buildFolder#",
                        "./" + options.getBuildDirectoryName())
                .replace("#webComponentTags#",
                        webComponentTags == null || webComponentTags.isEmpty()
                                ? ""
                                : String.join(";", webComponentTags));
        FileIOUtils.writeIfChanged(generatedConfigFile, template);
        log().debug("Created vite generated configuration file: '{}'",
                generatedConfigFile);
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
