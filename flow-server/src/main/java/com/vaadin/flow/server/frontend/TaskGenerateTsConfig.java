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
package com.vaadin.flow.server.frontend;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

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

    private static final String COMPILER_OPTIONS = "compilerOptions";

    static final String TSCONFIG_JSON = "tsconfig.json";

    private static final String OLD_VERSION_KEY = "flow_version";
    private static final String VERSION = "_version";
    private static final String ES_TARGET_VERSION = "target";
    private static final String TSCONFIG_JSON_OLDER_VERSIONS_TEMPLATE = "tsconfig-%s.json";
    private static final String[] tsconfigVersions = { "latest", "v23.3.0.1",
            "v23.3.0", "v23.2", "v23.1", "v22", "v14", "osgi", "v23.3.4",
            "v23.3.4-hilla" };

    //@formatter:off
    static final String ERROR_MESSAGE =
            "%n%n**************************************************************************"
            + "%n*  TypeScript config file 'tsconfig.json' has been updated to the latest *"
            + "%n*  version by Vaadin. Please verify that the updated 'tsconfig.json'     *"
            + "%n*  file contains configuration needed for your project (add missing part *"
            + "%n*  from the old file if necessary) and restart the application.          *"
            + "%n**************************************************************************%n%n";
    //@formatter:on

    private Options options;

    /**
     * Create a task to generate <code>tsconfig.json</code> file.
     *
     * @param options
     *            the task options
     */
    TaskGenerateTsConfig(Options options) {
        this.options = options;
    }

    @Override
    protected String getFileContent() throws IOException {
        return getFileContentForVersion("latest");
    }

    private String getFileContentForVersion(String vaadinVersion)
            throws IOException {
        String fileName;
        if ("latest".equals(vaadinVersion)) {
            fileName = TSCONFIG_JSON;
        } else {
            fileName = String.format(TSCONFIG_JSON_OLDER_VERSIONS_TEMPLATE,
                    vaadinVersion);
        }
        try (InputStream tsConfStream = getClass()
                .getResourceAsStream(fileName)) {
            String config = IOUtils.toString(tsConfStream, UTF_8);
            return config;
        }
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (shouldGenerate()) {
            super.execute();
        } else {
            overrideIfObsolete();
            ensureTarget(getDefaultEsTargetVersion());
        }
    }

    private void ensureTarget(String esVersion) {
        try {
            File projectTsconfig = new File(options.getNpmFolder(),
                    TSCONFIG_JSON);
            String current = FileUtils.readFileToString(projectTsconfig,
                    StandardCharsets.UTF_8);
            String currentEsVersion = getEsTargetVersion(current);
            if (isOlder(currentEsVersion, esVersion)) {
                current = current.replace(currentEsVersion, esVersion);
                FileIOUtils.writeIfChanged(projectTsconfig, current);
            }
        } catch (Exception e) {
            // This could be a malformed tsconfig, leave it alone
            log().debug("Unable to modify target version in tsconfig.json", e);
        }
    }

    static boolean isOlder(String esVersion1, String esVersion2) {
        if (esVersion1.startsWith("es") && esVersion2.startsWith("es")) {
            return esVersion1.compareTo(esVersion2) < 0;
        }
        return !esVersion1.equals(esVersion2);
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
        return parsed.getObject(COMPILER_OPTIONS).getString(ES_TARGET_VERSION);
    }

    private JsonObject parseTsConfig(String tsConfig) {
        // remove comments so parser works
        String json = tsConfig.replaceAll("//.*", "");
        return Json.parse(json);
    }

    @Override
    protected File getGeneratedFile() {
        return new File(options.getNpmFolder(), TSCONFIG_JSON);
    }

    @Override
    protected boolean shouldGenerate() {
        return !getGeneratedFile().exists();
    }

    private void overrideIfObsolete() throws ExecutionFailedException {
        try {
            // Project's TS config
            File projectTsConfigFile = new File(
                    options.getNpmFolder().getPath(), TSCONFIG_JSON);
            String projectTsConfigAsString = FileUtils
                    .readFileToString(projectTsConfigFile, UTF_8);

            JsonObject projectTsConfigContent;
            try {
                projectTsConfigContent = parseTsConfig(projectTsConfigAsString);
            } catch (Exception e) {
                // This could be a malformed tsconfig, leave it alone
                log().error("Unable to parse tsconfig.json", e);
                return;
            }

            // Newest TS config template in Flow
            String latestTsConfigTemplate = getFileContent();
            JsonObject latestTsConfigTemplateJson = parseTsConfig(
                    latestTsConfigTemplate);

            String projectTsConfigVersion = getConfigVersion(
                    projectTsConfigContent);
            if (projectTsConfigVersion != null) {
                String templateVersion = getConfigVersion(
                        latestTsConfigTemplateJson);

                // If the project has a newest version of TS config - do nothing
                if (templateVersion.equals(projectTsConfigVersion)) {
                    return;
                }
            }

            // TS config is of an old version
            for (String tsconfigVersion : tsconfigVersions) {
                String oldTsConfigContent = getFileContentForVersion(
                        tsconfigVersion);
                JsonObject tsConfigTemplateJson = parseTsConfig(
                        oldTsConfigContent);
                if (tsConfigsEqual(tsConfigTemplateJson,
                        projectTsConfigContent)) {
                    // Found exact match with the one of templates -
                    // just rewrite silently
                    FileIOUtils.writeIfChanged(projectTsConfigFile,
                            latestTsConfigTemplate);
                    return;
                }
            }

            // Project's TS config has a custom content -
            // rewrite and throw an exception with explanations
            FileIOUtils.writeIfChanged(projectTsConfigFile,
                    latestTsConfigTemplate);
            throw new ExecutionFailedException(String.format(ERROR_MESSAGE));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getConfigVersion(JsonObject projectTsConfigContent) {
        if (projectTsConfigContent.hasKey(VERSION)) {
            return projectTsConfigContent.getString(VERSION);
        }
        if (projectTsConfigContent.hasKey(OLD_VERSION_KEY)) {
            return projectTsConfigContent.getString(OLD_VERSION_KEY);
        }
        return null;
    }

    private boolean tsConfigsEqual(JsonObject template,
            JsonObject projectTsConfig) {
        // exclude ES version from comparison, because it
        // might be different for webpack and vite
        if (template.hasKey(COMPILER_OPTIONS)) {
            template.getObject(COMPILER_OPTIONS).remove(ES_TARGET_VERSION);
        }
        if (projectTsConfig.hasKey(COMPILER_OPTIONS)) {
            projectTsConfig.getObject(COMPILER_OPTIONS)
                    .remove(ES_TARGET_VERSION);
        }

        // exclude tsconfig version, because it's already compared
        template.remove(VERSION);
        projectTsConfig.remove(VERSION);
        return removeWhiteSpaces(template.toJson())
                .equals(removeWhiteSpaces(projectTsConfig.toJson()));
    }

    private String removeWhiteSpaces(String content) {
        return content.replaceAll("\\s", "");
    }
}
