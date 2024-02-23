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
            *  The TypeScript type declaration file 'types.d.ts' has been updated     *
            *  to the latest version by Vaadin. Previous content has been backed up   *
            *  on 'types.d.ts.flowBackup' file. Please verify that the updated        *
            *  'types.d.ts' file contains configuration needed for your project, and  *
            *  then delete the backup file.                                           *
            ***************************************************************************


            """;

    static final String CHECK_CONTENT_MESSAGE = """


            ****************************************************************************
            *  The TypeScript type declaration file 'types.d.ts' has been customized.  *
            *  Make sure the exact following configuration is present in that file:    *
            *                                                                          *
            %s
            *                                                                          *
            *  As an alternative you can rename the file, make Vaadin re-generate it,  *
            *  and then update it with your custom contents.                           *
            ****************************************************************************"


            """;

    static final String TS_DEFINITIONS = "types.d.ts";
    static final Pattern COMMENT_LINE = Pattern.compile("(?m)^/[/*].*\\R");
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

            String uncommentedDefaultContent = removeComments(defaultContent);
            if (compareIgnoringIndentationAndEOL(content, defaultContent,
                    String::equals)
                    || compareIgnoringIndentationAndEOL(content,
                            uncommentedDefaultContent, String::contains)) {
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
                UpdateMode updateMode = computeUpdateMode(content);
                if (updateMode == UpdateMode.UPDATE_AND_THROW) {
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
                    if (updateMode == UpdateMode.REPLACE) {
                        Files.writeString(tsDefinitions.toPath(),
                                defaultContent,
                                StandardOpenOption.TRUNCATE_EXISTING);
                    } else {
                        Files.writeString(tsDefinitions.toPath(),
                                uncommentedDefaultContent,
                                StandardOpenOption.APPEND);
                        if (updateMode == UpdateMode.UPDATE_AND_THROW) {
                            throw new ExecutionFailedException(UPDATE_MESSAGE);
                        }
                    }
                } catch (IOException ex) {
                    throw new ExecutionFailedException(
                            "Error updating custom " + TS_DEFINITIONS + " file",
                            ex);
                }
            }
        }
    }

    private enum UpdateMode {
        REPLACE, UPDATE, UPDATE_AND_THROW
    }

    private UpdateMode computeUpdateMode(String content)
            throws ExecutionFailedException {
        try {
            String templateContent = getTemplateContent(".v1");
            if (compareIgnoringIndentationAndEOL(content, templateContent,
                    String::equals)) {
                // Current content has been written by Flow, can be replaced
                return UpdateMode.REPLACE;
            }
        } catch (IOException ex) {
            throw new ExecutionFailedException(
                    "Cannot read default " + TS_DEFINITIONS + ".v1 contents",
                    ex);
        }

        try {
            String templateContent = getTemplateContent(".hilla");
            String uncommentedContent = removeComments(content);
            if (compareIgnoringIndentationAndEOL(uncommentedContent,
                    templateContent, String::equals)) {
                // Current content is compatible with what we expect to be in a
                // Hilla application. Flow contents can be appended silently.
                return UpdateMode.UPDATE;
            }
        } catch (IOException ex) {
            throw new ExecutionFailedException(
                    "Cannot read default " + TS_DEFINITIONS + ".hilla contents",
                    ex);
        }
        // types.d.ts has been customized, but does not seem to contain content
        // required by flow. Append what is needed and throw an exception so
        // that developer can check the changes are ok
        return UpdateMode.UPDATE_AND_THROW;
    }

    private static String removeComments(String content) {
        return COMMENT_LINE.matcher(content).replaceAll("");
    }

    private static boolean compareIgnoringIndentationAndEOL(String content1,
            String content2, BiPredicate<String, String> compareFn) {
        return compareFn.test(replaceIndentationAndEOL(content1),
                replaceIndentationAndEOL(content2));
    }

    // Normalize EOL and removes indentation and potential EOL at the end of the
    // FILE
    static String replaceIndentationAndEOL(String text) {
        return text.replace("\r\n", "\n").replaceFirst("\n$", "")
                .replaceAll("(?m)^\\s+", "");
    }

    private String getTemplateContent(String suffix) throws IOException {
        try (InputStream tsDefinitionStream = getClass()
                .getResourceAsStream(TS_DEFINITIONS + suffix)) {
            return IOUtils.toString(tsDefinitionStream, UTF_8);
        }
    }

}
