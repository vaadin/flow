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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.ExecutionFailedException;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Generate <code>tsconfig.json</code> if it is missing in project folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateTsConfig extends AbstractTaskClientGenerator {

    static final String TSCONFIG_JSON = "tsconfig.json";

    private static final String VERSION = "version";
    private static final String ES_TARGET_VERSION = "target";

    //@formatter:off
    static final String ERROR_MESSAGE =
            "%n%n**************************************************************************"
            + "%n*  TypeScript config file 'tsconfig.json' has been updated to the latest *"
            + "%n*  version by Vaadin. Please verify that the updated 'tsconfig.json'     *"
            + "%n*  file contains configuration needed for your project (add missing part *"
            + "%n*  from the old file if needed) and restart the application.             *"
            + "%n**************************************************************************%n%n";
    //@formatter:on

    private final File projectRootDir;
    private final FeatureFlags featureFlags;

    /**
     * Create a task to generate <code>tsconfig.json</code> file.
     *
     * @param projectRootDir
     *            project folder where the file will be generated.
     * @param featureFlags
     *            available feature flags and their status
     */
    TaskGenerateTsConfig(File projectRootDir, FeatureFlags featureFlags) {
        this.projectRootDir = projectRootDir;
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
        } else {
            overrideIfObsolete();
            if (featureFlags.isEnabled(FeatureFlags.WEBPACK)) {
                ensureTarget("es2019");
            } else {
                ensureTarget(getDefaultEsTargetVersion());
            }
        }
    }

    private void ensureTarget(String esVersion) {
        try {
            File projectTsconfig = new File(projectRootDir, TSCONFIG_JSON);
            String current = FileUtils.readFileToString(projectTsconfig,
                    StandardCharsets.UTF_8);
            String currentEsVersion = getEsTargetVersion(current);
            if (!currentEsVersion.equals(esVersion)) {
                current = current.replace(currentEsVersion, esVersion);
                writeIfChanged(projectTsconfig, current);
            }
        } catch (Exception e) {
            // This could be a malformed tsconfig, leave it alone
            log().debug("Unable to modify target version in tsconfig.json", e);
        }
    }

    private String getDefaultEsTargetVersion() throws ExecutionFailedException {
        try {
            String defaultTsConfig = getFileContent();
            return getEsTargetVersion(defaultTsConfig);
        } catch (Exception e) {
            throw new ExecutionFailedException(
                    "Error finding default es target value", e);
        }

    }

    private String getEsTargetVersion(String tsConfig) {
        JsonObject parsed = parseTsConfig(tsConfig);
        return parsed.getObject("compilerOptions").getString(ES_TARGET_VERSION);
    }

    private JsonObject parseTsConfig(String tsConfig) {
        // remove comments so parser works
        String json = tsConfig.replaceAll("//.*", "");
        return Json.parse(json);
    }

    @Override
    protected File getGeneratedFile() {
        return new File(projectRootDir, TSCONFIG_JSON);
    }

    @Override
    protected boolean shouldGenerate() {
        return !new File(projectRootDir, TSCONFIG_JSON).exists();
    }

    private void overrideIfObsolete() throws ExecutionFailedException {
        try {
            File projectTsConfigFile = new File(projectRootDir.getPath(),
                    TSCONFIG_JSON);
            String projectRsConfigAsString = FileUtils
                    .readFileToString(projectTsConfigFile, UTF_8);
            JsonObject projectTsConfigContent = parseTsConfig(
                    projectRsConfigAsString);
            String tsConfigTemplate = getFileContent();

            JsonObject tsConfigTemplateJson = parseTsConfig(tsConfigTemplate);

            if (projectTsConfigContent.hasKey(VERSION)) {
                String version = projectTsConfigContent.getString(VERSION);
                String templateVersion = tsConfigTemplateJson
                        .getString(VERSION);
                if (templateVersion.equals(version)) {
                    return;
                }
            }

            writeIfChanged(projectTsConfigFile, tsConfigTemplate);

            if (!tsConfigEquals(tsConfigTemplateJson, projectTsConfigContent)) {
                throw new ExecutionFailedException(
                        String.format(ERROR_MESSAGE));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String removeWhiteSpaces(String content) {
        return content.replaceAll("\\s", "");
    }

    private boolean tsConfigEquals(JsonObject template,
            JsonObject projectTsConfig) {
        // exclude ES version and file version from comparison, because it might
        // be different for webpack and vite
        template.remove(ES_TARGET_VERSION);
        template.remove(VERSION);
        projectTsConfig.remove(ES_TARGET_VERSION);
        projectTsConfig.remove(VERSION);
        return removeWhiteSpaces(template.toJson())
                .hashCode() == removeWhiteSpaces(projectTsConfig.toJson())
                        .hashCode();
    }
}
