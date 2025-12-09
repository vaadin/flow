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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StringUtil;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>tsconfig.json</code> if it is missing in project folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateTsConfig extends AbstractTaskClientGenerator {

    /**
     * Keeps track of whether a warning update has already been logged. This is
     * used to avoid spamming the log with the same message.
     */
    protected static boolean warningEmitted = false;

    private static final String COMPILER_OPTIONS = "compilerOptions";

    static final String TSCONFIG_JSON = "tsconfig.json";

    private static final String OLD_VERSION_KEY = "flow_version";
    private static final String VERSION = "_version";
    private static final String ES_TARGET_VERSION = "target";
    private static final String TSCONFIG_JSON_OLDER_VERSIONS_TEMPLATE = "tsconfig-%s.json";
    private static final String[] tsconfigVersions = { "latest", "v23.3.0.1",
            "v23.3.0", "v23.2", "v23.1", "v22", "v14", "osgi", "v23.3.4",
            "v23.3.4-hilla", "es2020", "es2022" };

    static final String ERROR_MESSAGE = """

            **************************************************************************
            *  TypeScript config file 'tsconfig.json' has been updated to the latest *
            *  version by Vaadin. Please verify that the updated 'tsconfig.json'     *
            *  file contains configuration needed for your project (add any missing  *
            *  parts from the old file if necessary) and restart the application.    *
            *  Old configuration is stored as a '.bak' file.                         *
            **************************************************************************

            """;

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
            String config = StringUtil.toUTF8String(tsConfStream);

            config = config.replaceAll("%FRONTEND%",
                    options.getNpmFolder().toPath()
                            .relativize(options.getFrontendDirectory().toPath())
                            .toString().replaceAll("\\\\", "/"));
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
            String current = Files.readString(projectTsconfig.toPath(),
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
        JsonNode parsed = parseTsConfig(tsConfig);
        return parsed.get(COMPILER_OPTIONS).get(ES_TARGET_VERSION).asString();
    }

    private ObjectNode parseTsConfig(String tsConfig) {
        // remove comments so parser works
        String json = tsConfig.replaceAll("//.*", "");
        return JacksonUtils.readTree(json);
    }

    @Override
    protected File getGeneratedFile() {
        return new File(options.getNpmFolder(), TSCONFIG_JSON);
    }

    @Override
    protected boolean shouldGenerate() {
        return !getGeneratedFile().exists();
    }

    private void overrideIfObsolete() {
        try {
            // Project's TS config
            File projectTsConfigFile = new File(
                    options.getNpmFolder().getPath(), TSCONFIG_JSON);
            String projectTsConfigAsString = Files
                    .readString(projectTsConfigFile.toPath(), UTF_8);

            ObjectNode projectTsConfigContent;
            try {
                projectTsConfigContent = parseTsConfig(projectTsConfigAsString);
            } catch (Exception e) {
                // This could be a malformed tsconfig, leave it alone
                log().error("Unable to parse tsconfig.json", e);
                return;
            }

            // Newest TS config template in Flow
            String latestTsConfigTemplate = getFileContent();
            JsonNode latestTsConfigTemplateJson = parseTsConfig(
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
                ObjectNode tsConfigTemplateJson = parseTsConfig(
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

            File backupFile = File.createTempFile(
                    projectTsConfigFile.getName() + ".", ".bak",
                    projectTsConfigFile.getParentFile());
            FileIOUtils.writeIfChanged(backupFile, projectTsConfigAsString);
            // Project's TS config has a custom content -
            // rewrite and throw an exception with explanations
            FileIOUtils.writeIfChanged(projectTsConfigFile,
                    latestTsConfigTemplate);
            if (!warningEmitted) {
                log().warn(ERROR_MESSAGE);
                warningEmitted = true;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getConfigVersion(JsonNode projectTsConfigContent) {
        if (projectTsConfigContent.has(VERSION)) {
            return projectTsConfigContent.get(VERSION).asString();
        }
        if (projectTsConfigContent.has(OLD_VERSION_KEY)) {
            return projectTsConfigContent.get(OLD_VERSION_KEY).asString();
        }
        return null;
    }

    private boolean tsConfigsEqual(ObjectNode template,
            ObjectNode projectTsConfig) {
        // exclude ES version from comparison, because it
        // might be different for webpack and vite
        if (template.has(COMPILER_OPTIONS)) {
            ((ObjectNode) template.get(COMPILER_OPTIONS))
                    .remove(ES_TARGET_VERSION);
        }
        if (projectTsConfig.has(COMPILER_OPTIONS)) {
            ((ObjectNode) projectTsConfig.get(COMPILER_OPTIONS))
                    .remove(ES_TARGET_VERSION);
        }

        // exclude tsconfig version, because it's already compared
        template.remove(VERSION);
        projectTsConfig.remove(VERSION);
        return removeWhiteSpaces(template.toString())
                .equals(removeWhiteSpaces(projectTsConfig.toString()));
    }

    private String removeWhiteSpaces(String content) {
        return content.replaceAll("\\s", "");
    }
}
