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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.server.ExecutionFailedException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate <code>types.d.ts</code> if it is missing in project folder and
 * <code>tsconfig.json</code> exists in project folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateTsDefinitions extends AbstractTaskClientGenerator {

    private static final String DECLARE_CSS_MODULE = "declare module '*.css?inline' {";
    static final String UPDATE_MESSAGE = """


            ***************************************************************************
            *  TypeScript type declaration file 'types.d.ts' has been updated to the  *
            *  latest version by Vaadin. Previous content has been backed up on       *
            *  'types.d.ts.flowBackup' file. Please verify that the updated           *
            *  'types.d.ts' file contains configuration needed for your project, and  *
            *  then delete the backup file.                                           *
            ***************************************************************************


            """;

    static final String CHECK_CONTENT_MESSAGE = """


            ************************************************************************
            *  TypeScript type declaration file 'types.d.ts' have been customized  *
            *  but is seems to contain the configuration required by Vaadin.       *
            *  Please make sure the following configuration is present in the      *
            *  'types.d.ts' file exactly as show below. As an alternative you can  *
            *  rename the 'types.d.ts', make Vaadin re-generate it, and then       *
            *  update it with your custom contents.                                *
            *  ------------------------------------------------------------------  *
            %s
            *  ------------------------------------------------------------------  *
            ************************************************************************"


            """;

    static final String TS_DEFINITIONS = "types.d.ts";
    static final Pattern COMMENT_LINE = Pattern.compile("(?m)//.*\\R");
    private final Options options;

    /**
     * Create a task to generate <code>types.d.ts</code> file.
     *
     * @param options
     *            Task Options.
     */
    TaskGenerateTsDefinitions(Options options) {
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (shouldGenerate()) {
            super.execute();
        } else {
            updateIfContentMissing();
        }
    }

    @Override
    protected String getFileContent() throws IOException {
        return getTemplateContent("");
    }

    @Override
    protected File getGeneratedFile() {
        return new File(options.getNpmFolder(), TS_DEFINITIONS);
    }

    @Override
    protected boolean shouldGenerate() {
        File tsDefinitionsFile = getGeneratedFile();
        return !tsDefinitionsFile.exists() && new File(options.getNpmFolder(),
                TaskGenerateTsConfig.TSCONFIG_JSON).exists();
    }

    private void updateIfContentMissing() throws ExecutionFailedException {
        File tsDefinitions = getGeneratedFile();
        if (tsDefinitions.exists()) {
            String defaultContent;
            try {
                defaultContent = getFileContent();
            } catch (IOException ex) {
                throw new ExecutionFailedException(
                        "Cannot read default " + TS_DEFINITIONS + " contents",
                        ex);
            }

            String content;
            try {
                content = Files.readString(tsDefinitions.toPath());
            } catch (IOException ex) {
                throw new ExecutionFailedException(
                        "Cannot read " + TS_DEFINITIONS + " contents", ex);
            }

            String uncommentedDefaultContent = COMMENT_LINE
                    .matcher(defaultContent).replaceAll("");
            if (compareIgnoringEOL(content, defaultContent, String::equals)
                    || compareIgnoringEOL(content, uncommentedDefaultContent,
                            String::contains)) {
                log().debug("{} is up-to-date", TS_DEFINITIONS);
            } else if (content.contains(DECLARE_CSS_MODULE)) {
                log().debug(
                        "Custom {} not updated because it contains '*.css?inline' module declaration",
                        TS_DEFINITIONS);
                throw new ExecutionFailedException(String.format(
                        CHECK_CONTENT_MESSAGE, uncommentedDefaultContent));
            } else {
                log().debug(
                        "Updating custom {} to add '*.css?inline' module declaration",
                        TS_DEFINITIONS);
                boolean customContent = hasCustomContent(content);
                if (customContent) {
                    try {
                        Path backupFile = tsDefinitions.toPath().getParent()
                                .resolve(TS_DEFINITIONS + ".flowBackup");
                        Files.copy(tsDefinitions.toPath(), backupFile);
                        log().debug("Created {} backup copy on {}",
                                TS_DEFINITIONS, backupFile);
                    } catch (IOException ex) {
                        throw new ExecutionFailedException(
                                "Cannot create backup copy of " + TS_DEFINITIONS
                                        + " file",
                                ex);
                    }
                }
                try {
                    if (customContent) {
                        Files.writeString(tsDefinitions.toPath(),
                                uncommentedDefaultContent,
                                StandardOpenOption.APPEND);
                        throw new ExecutionFailedException(UPDATE_MESSAGE);
                    } else {
                        Files.writeString(tsDefinitions.toPath(),
                                defaultContent,
                                StandardOpenOption.TRUNCATE_EXISTING);
                    }
                } catch (IOException ex) {
                    throw new ExecutionFailedException(
                            "Error updating custom " + TS_DEFINITIONS + " file",
                            ex);
                }
            }
        }
    }

    private boolean hasCustomContent(String content)
            throws ExecutionFailedException {
        try {
            return !compareIgnoringEOL(content, getTemplateContent(".v1"),
                    String::equals);
        } catch (IOException ex) {
            throw new ExecutionFailedException(
                    "Cannot read default " + TS_DEFINITIONS + ".v1 contents",
                    ex);
        }
    }

    // Normalize EOL and removes potential EOL at the end of the FILE
    private static boolean compareIgnoringEOL(String content1, String content2,
            BiPredicate<String, String> compareFn) {
        return compareFn.test(
                content1.replace("\r\n", "\n").replaceFirst("\n$", ""),
                content2.replace("\r\n", "\n").replaceFirst("\n$", ""));
    }

    private String getTemplateContent(String suffix) throws IOException {
        try (InputStream tsDefinitionStream = getClass()
                .getResourceAsStream(TS_DEFINITIONS + suffix)) {
            return IOUtils.toString(tsDefinitionStream, UTF_8);
        }
    }

}
