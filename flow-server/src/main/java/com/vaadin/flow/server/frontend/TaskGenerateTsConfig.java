/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.ExecutionFailedException;

import elemental.json.Json;

/**
 * Generate <code>tsconfig.json</code> if it is missing in project folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateTsConfig extends AbstractTaskClientGenerator {

    static final String TSCONFIG_JSON = "tsconfig.json";
    private final File npmFolder;
    private FeatureFlags featureFlags;

    /**
     * Create a task to generate <code>tsconfig.json</code> file.
     *
     * @param npmFolder
     *            project folder where the file will be generated.
     * @param featureFlags
     *            available feature flags and their status
     */
    TaskGenerateTsConfig(File npmFolder, FeatureFlags featureFlags) {
        this.npmFolder = npmFolder;
        this.featureFlags = featureFlags;
    }

    @Override
    protected String getFileContent() throws IOException {
        try (InputStream tsConfStream = getClass()
                .getResourceAsStream(TSCONFIG_JSON)) {
            String config = IOUtils.toString(tsConfStream, UTF_8);
            if (featureFlags.isEnabled(FeatureFlags.WEBPACK)) {
                // webpack 4 cannot use anything newer than es2019...
                config = config.replaceFirst("\"target\".*",
                        "\"target\": \"es2019\",");
            }
            return config;
        }
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (shouldGenerate()) {
            super.execute();
        } else if (!featureFlags.isEnabled(FeatureFlags.WEBPACK)) {
            // If Vite is used, we need to upgrade the target
            File projectTsconfig = new File(npmFolder, TSCONFIG_JSON);
            if (projectTsconfig.exists()) {
                try {
                    String current = FileUtils.readFileToString(projectTsconfig,
                            StandardCharsets.UTF_8);
                    if (current.contains("es2019")) {
                        current = current.replace("es2019", getEsTarget());
                    }
                    writeIfChanged(projectTsconfig, current);
                } catch (IOException e) {
                    throw new ExecutionFailedException(
                            "Error upgrading target value", e);
                }

            }

        }
    }

    private String getEsTarget() throws IOException {
        String defaultTsConfig = getFileContent();
        defaultTsConfig = defaultTsConfig.replaceAll("//.*", ""); // remove
                                                                  // comments
        return Json.parse(defaultTsConfig).getObject("compilerOptions")
                .getString("target");
    }

    @Override
    protected File getGeneratedFile() {
        return new File(npmFolder, TSCONFIG_JSON);
    }

    @Override
    protected boolean shouldGenerate() {
        return !new File(npmFolder, TSCONFIG_JSON).exists();
    }
}
