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
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.ExecutionFailedException;

import static com.vaadin.flow.server.frontend.FileIOUtils.compareIgnoringIndentationEOLAndWhiteSpace;

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
    private static final String DECLARE_CSSTYPE_MODULE = "declare module 'csstype' {";
    static final String UPDATE_MESSAGE = """

            ***************************************************************************
            *  The TypeScript type declaration file 'types.d.ts' has been updated     *
            *  to the latest version by Vaadin. Previous content has been backed up   *
            *  as a '.bak' file. Please verify that the updated 'types.d.ts' file    *
            *  contains configuration needed for your project, and then delete the    *
            *  backup file.                                                           *
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
    /**
     * Keeps track of whether a warning update has already been logged. This is
     * used to avoid spamming the log with the same message.
     */
    protected static boolean warningEmitted = false;

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

            String cssModuleContent;
            try {
                cssModuleContent = removeComments(getTemplateContent(".v2"));
            } catch (IOException ex) {
                throw new ExecutionFailedException(
                        "Cannot read " + TS_DEFINITIONS + ".v2 contents", ex);
            }

            String uncommentedDefaultContent = removeComments(defaultContent);
            String cssTypeModuleContent = uncommentedDefaultContent
                    .replace(cssModuleContent, "");
            boolean containsExactCssModule = compareIgnoringIndentationEOLAndWhiteSpace(
                    content, cssModuleContent, String::equals)
                    || compareIgnoringIndentationEOLAndWhiteSpace(content,
                            cssModuleContent, String::contains);
            boolean containsExactCssTypeModule = compareIgnoringIndentationEOLAndWhiteSpace(
                    content, cssTypeModuleContent, String::equals)
                    || compareIgnoringIndentationEOLAndWhiteSpace(content,
                            cssTypeModuleContent, String::contains);
            boolean containsCssType = content.contains(DECLARE_CSSTYPE_MODULE);
            boolean containsCssModule = content.contains(DECLARE_CSS_MODULE);

            if (compareIgnoringIndentationEOLAndWhiteSpace(content,
                    defaultContent, String::equals)
                    || compareIgnoringIndentationEOLAndWhiteSpace(content,
                            uncommentedDefaultContent, String::contains)) {
                log().debug("{} is up-to-date", TS_DEFINITIONS);
            } else if (containsExactCssModule && containsExactCssTypeModule) {
                log().debug("{} is up-to-date", TS_DEFINITIONS);
            } else if (containsCssModule && !containsExactCssModule) {
                log().debug(
                        "Custom {} not updated because it contains '*.css?inline' module declaration",
                        TS_DEFINITIONS);
                throw new ExecutionFailedException(String.format(
                        CHECK_CONTENT_MESSAGE, uncommentedDefaultContent));
            } else if (containsCssType && !containsExactCssTypeModule) {
                log().debug(
                        "Custom {} not updated because it contains 'csstype' module declaration",
                        TS_DEFINITIONS);
                throw new ExecutionFailedException(String.format(
                        CHECK_CONTENT_MESSAGE, uncommentedDefaultContent));
            } else {
                if (containsExactCssModule) {
                    log().debug(
                            "Updating custom {} to add 'csstype' module declaration",
                            TS_DEFINITIONS);
                } else {
                    log().debug(
                            "Updating custom {} to add '*.css?inline' and 'csstype' module declarations",
                            TS_DEFINITIONS);
                }
                UpdateMode updateMode = computeUpdateMode(content);
                if (updateMode == UpdateMode.UPDATE_AND_BACKUP) {
                    try {
                        File backupFile = File.createTempFile(
                                tsDefinitions.getName() + ".", ".bak",
                                tsDefinitions.getParentFile());
                        writeIfChanged(backupFile, content);
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
                        // If correct css module was found, only add csstype
                        if (containsExactCssModule) {
                            uncommentedDefaultContent = uncommentedDefaultContent
                                    .replace(cssModuleContent, "");
                        }
                        if (containsExactCssTypeModule) {
                            uncommentedDefaultContent = uncommentedDefaultContent
                                    .replace(cssTypeModuleContent, "");
                        }
                        Files.writeString(tsDefinitions.toPath(),
                                uncommentedDefaultContent,
                                StandardOpenOption.APPEND);
                        if (updateMode == UpdateMode.UPDATE_AND_BACKUP
                                && !warningEmitted) {
                            log().warn(UPDATE_MESSAGE);
                            warningEmitted = true;
                        }
                    }
                    track(tsDefinitions);
                } catch (IOException ex) {
                    throw new ExecutionFailedException(
                            "Error updating custom " + TS_DEFINITIONS + " file",
                            ex);
                }
            }
        }
    }

    private enum UpdateMode {
        REPLACE, UPDATE, UPDATE_AND_BACKUP
    }

    private UpdateMode computeUpdateMode(String content)
            throws ExecutionFailedException {
        try {
            String templateContent = getTemplateContent(".v1");
            if (compareIgnoringIndentationEOLAndWhiteSpace(content,
                    templateContent, String::equals)) {
                // Current content has been written by Flow, can be replaced
                return UpdateMode.REPLACE;
            }
            templateContent = getTemplateContent(".v2");
            if (compareIgnoringIndentationEOLAndWhiteSpace(content,
                    templateContent, String::equals)) {
                // Current content has been written by Flow, can be replaced
                return UpdateMode.REPLACE;
            }
        } catch (IOException ex) {
            throw new ExecutionFailedException(
                    "Cannot read default " + TS_DEFINITIONS + ".v1 contents",
                    ex);
        }

        try {
            String templateContent = getTemplateContent(".hilla.v1");
            String templateV2Content = getTemplateContent(".hilla.v2");
            String uncommentedContent = removeComments(content);
            if (compareIgnoringIndentationEOLAndWhiteSpace(uncommentedContent,
                    templateContent, String::equals)
                    || compareIgnoringIndentationEOLAndWhiteSpace(
                            uncommentedContent, templateV2Content,
                            String::equals)) {
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
        return UpdateMode.UPDATE_AND_BACKUP;
    }

    private static String removeComments(String content) {
        return COMMENT_LINE.matcher(content).replaceAll("");
    }

    private String getTemplateContent(String suffix) throws IOException {
        try (InputStream tsDefinitionStream = getClass()
                .getResourceAsStream(TS_DEFINITIONS + suffix)) {
            return StringUtil.toUtf8Str(tsDefinitionStream);
        }
    }

}
