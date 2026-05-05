/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.MockLogger;

import static com.vaadin.flow.server.frontend.TaskGenerateTsDefinitions.TS_DEFINITIONS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
class TaskGenerateTsDefinitionsTest {
    @TempDir
    File temporaryFolder;
    private File outputFolder;
    private TaskGenerateTsDefinitions taskGenerateTsDefinitions;

    @BeforeEach
    void setUp() throws IOException {
        outputFolder = Files
                .createTempDirectory(temporaryFolder.toPath(), "tmp").toFile();
        Options options = new Options(Mockito.mock(Lookup.class), outputFolder);

        taskGenerateTsDefinitions = new TaskGenerateTsDefinitions(options);
        taskGenerateTsDefinitions.warningEmitted = false;
    }

    @Test
    void should_generateTsDefinitions_TsDefinitionsNotExistAndTsConfigExists()
            throws Exception {
        Files.createFile(
                new File(outputFolder, TaskGenerateTsConfig.TSCONFIG_JSON)
                        .toPath());
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should generate types.d.ts when tsconfig.json exists and "
                        + "types.d.ts doesn't exist");
        assertTrue(taskGenerateTsDefinitions.getGeneratedFile().exists(),
                "The generated types.d.ts should not exist");
        assertEquals(taskGenerateTsDefinitions.getFileContent(),
                IOUtils.toString(
                        taskGenerateTsDefinitions.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8),
                "The generated content should be equals the default content");
    }

    @Test
    void should_notGenerateTsDefinitions_TsConfigNotExist() throws Exception {
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when tsconfig.json "
                        + "doesn't exist");
        assertFalse(taskGenerateTsDefinitions.getGeneratedFile().exists(),
                "The types.d.ts should not exist");
    }

    @Test
    void tsDefinition_upToDate_tsDefinitionNotUpdated() throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String expectedContent = readExpectedContent(false);
        Files.writeString(typesTSfile, expectedContent);
        FileTime lastModifiedTime = Files.getLastModifiedTime(typesTSfile);
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = Files.readString(typesTSfile);
        assertEquals(lastModifiedTime, Files.getLastModifiedTime(typesTSfile),
                "types.d.ts should not have been updated");
        assertEquals(updatedContent, expectedContent,
                "types.d.ts should have been replaced");
    }

    @Test
    void tsDefinition_oldFlowContents_tsDefinitionUpdated() throws Exception {
        tsDefinition_oldFlowContents_tsDefinitionUpdated(".v1");
        tsDefinition_oldFlowContents_tsDefinitionUpdated(".v2");
    }

    private void tsDefinition_oldFlowContents_tsDefinitionUpdated(String suffix)
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        Files.writeString(typesTSfile, readPreviousContent(suffix));
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = Files.readString(typesTSfile);
        assertEquals(updatedContent, readExpectedContent(false),
                "types.d.ts should have been replaced");
    }

    @Test
    void tsDefinition_oldFlowContents_missingLastEOL_tsDefinitionUpdated()
            throws Exception {
        tsDefinition_oldFlowContents_missingLastEOL_tsDefinitionUpdated(".v1");
        tsDefinition_oldFlowContents_missingLastEOL_tsDefinitionUpdated(".v2");
    }

    private void tsDefinition_oldFlowContents_missingLastEOL_tsDefinitionUpdated(
            String suffix) throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        Files.writeString(typesTSfile,
                readPreviousContent(suffix).replaceFirst("\r?\n$", ""));
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = Files.readString(typesTSfile);
        assertEquals(updatedContent, readExpectedContent(false),
                "types.d.ts should have been replaced");
    }

    @Test
    void tsDefinition_oldHillaContents_tsDefinitionUpdated() throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String hillaTsDef = IOUtils.toString(
                getClass().getResourceAsStream(TS_DEFINITIONS + ".hilla.v1"),
                UTF_8);
        Files.writeString(typesTSfile, hillaTsDef);
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                updatedContent,
                CoreMatchers.containsString(readExpectedContent(true)));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    void tsDefinition_oldHillaContents_ignoringMultilineComments_tsDefinitionUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String hillaTsDef = IOUtils.toString(
                getClass().getResourceAsStream(TS_DEFINITIONS + ".hilla.v1"),
                UTF_8);
        // do not care about comment type (single or multi line)
        hillaTsDef = hillaTsDef.replaceAll("(?m)^(\\s*declare.*)$",
                "/* a comment */\n$1\n");
        Files.writeString(typesTSfile, hillaTsDef);
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                updatedContent,
                CoreMatchers.containsString(readExpectedContent(true)));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    void tsDefinition_oldHillaContents_ignoringSingleLineComments_tsDefinitionUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String hillaTsDef = IOUtils.toString(
                getClass().getResourceAsStream(TS_DEFINITIONS + ".hilla.v1"),
                UTF_8);
        // do not care about comment type (single or multi line)
        hillaTsDef = hillaTsDef.replaceAll("(?m)^(\\s*declare.*)$",
                "// a comment\n$1\n");
        Files.writeString(typesTSfile, hillaTsDef);
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                updatedContent,
                CoreMatchers.containsString(readExpectedContent(true)));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    void tsDefinition_oldHillaV2Contents_tsDefinitionUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String hillaTsDef = IOUtils.toString(
                getClass().getResourceAsStream(TS_DEFINITIONS + ".hilla.v2"),
                UTF_8);
        Files.writeString(typesTSfile, hillaTsDef);
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                stripEmptyLines(updatedContent), CoreMatchers.containsString(
                        stripEmptyLines(readExpectedContent(true))));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    void tsDefinition_oldHillaV2Contents_ignoringMultilineComments_tsDefinitionUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String hillaTsDef = IOUtils.toString(
                getClass().getResourceAsStream(TS_DEFINITIONS + ".hilla.v2"),
                UTF_8);
        // do not care about comment type (single or multi line)
        hillaTsDef = hillaTsDef.replaceAll("(?m)^(\\s*declare.*)$",
                "/* a comment */\n$1");
        Files.writeString(typesTSfile, hillaTsDef);
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                stripEmptyLines(updatedContent), CoreMatchers.containsString(
                        stripEmptyLines(readExpectedContent(true))));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    void tsDefinition_oldHillaV2Contents_ignoringSingleLineComments_tsDefinitionUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String hillaTsDef = IOUtils.toString(
                getClass().getResourceAsStream(TS_DEFINITIONS + ".hilla.v2"),
                UTF_8);
        // do not care about comment type (single or multi line)
        hillaTsDef = hillaTsDef.replaceAll("(?m)^(\\s*declare.*)$",
                "// a comment\n$1");
        Files.writeString(typesTSfile, hillaTsDef);
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                stripEmptyLines(updatedContent), CoreMatchers.containsString(
                        stripEmptyLines(readExpectedContent(true))));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    void customTsDefinition_missingFlowContents_tsDefinitionUpdatedAndWarningLogged()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String originalContent = """
                import type { SchemaObject } from "../../types";
                export type SchemaObjectMap = {
                    [Ref in string]?: SchemaObject;
                };
                export declare const jtdForms: readonly ["elements", "values", "discriminator", "properties", "optionalProperties", "enum", "type", "ref"];
                export type JTDForm = typeof jtdForms[number];
                """;
        Files.writeString(typesTSfile, originalContent);

        MockLogger logger = new MockLogger();
        try (MockedStatic<AbstractTaskClientGenerator> client = Mockito
                .mockStatic(AbstractTaskClientGenerator.class,
                        Mockito.CALLS_REAL_METHODS)) {
            client.when(() -> AbstractTaskClientGenerator.log())
                    .thenReturn(logger);
            taskGenerateTsDefinitions.execute();
        }
        assertTrue(logger.getLogs()
                .contains(TaskGenerateTsDefinitions.UPDATE_MESSAGE));
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                updatedContent,
                CoreMatchers.containsString(readExpectedContent(true)));
        assertBackupFileCreated(originalContent);
    }

    @Test
    void customTsDefinition_v1FlowContents_tsDefinitionUpdatedAndWarningLogged()
            throws Exception {
        customTsDefinition_oldFlowContents_tsDefinitionUpdatedAndWarningLogged(
                ".v1");
    }

    @Test
    void customTsDefinition_v2FlowContents_tsDefinitionUpdatedAndWarningLogged()
            throws Exception {
        customTsDefinition_oldFlowContents_tsDefinitionUpdatedAndWarningLogged(
                ".v2");
    }

    private void customTsDefinition_oldFlowContents_tsDefinitionUpdatedAndWarningLogged(
            String suffix) throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String originalContent = "import type { SchemaObject } from \"../../types\";"
                + System.lineSeparator() + readPreviousContent(suffix);
        Files.writeString(typesTSfile, originalContent);

        MockLogger logger = new MockLogger();
        try (MockedStatic<AbstractTaskClientGenerator> client = Mockito
                .mockStatic(AbstractTaskClientGenerator.class,
                        Mockito.CALLS_REAL_METHODS)) {
            client.when(() -> AbstractTaskClientGenerator.log())
                    .thenReturn(logger);
            taskGenerateTsDefinitions.execute();
        }
        assertTrue(logger.getLogs()
                .contains(TaskGenerateTsDefinitions.UPDATE_MESSAGE));

        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        String updatedContent = stripEmptyLines(Files.readString(typesTSfile));
        MatcherAssert.assertThat("types.d.ts should have been updated",
                updatedContent, CoreMatchers.containsString(
                        stripEmptyLines(readExpectedContent(true))));
        assertBackupFileCreated(originalContent);
    }

    @Test
    void contentUpdateForSecondTime_tsDefinitionUpdatedAndWarningLoggedOnce()
            throws Exception {
        contentUpdateForSecondTime_tsDefinitionUpdatedAndWarningLoggedOnce(
                ".v1");
        taskGenerateTsDefinitions.warningEmitted = false;
        contentUpdateForSecondTime_tsDefinitionUpdatedAndWarningLoggedOnce(
                ".v2");
    }

    private void contentUpdateForSecondTime_tsDefinitionUpdatedAndWarningLoggedOnce(
            String suffix) throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String originalContent = "import type { SchemaObject } from \"../../types\";"
                + System.lineSeparator() + readPreviousContent(suffix);
        Files.writeString(typesTSfile, originalContent);

        MockLogger logger = new MockLogger();
        try (MockedStatic<AbstractTaskClientGenerator> client = Mockito
                .mockStatic(AbstractTaskClientGenerator.class,
                        Mockito.CALLS_REAL_METHODS)) {
            client.when(() -> AbstractTaskClientGenerator.log())
                    .thenReturn(logger);
            taskGenerateTsDefinitions.execute();

            assertTrue(logger.getLogs()
                    .contains(TaskGenerateTsDefinitions.UPDATE_MESSAGE));

            logger.clearLogs();

            Files.writeString(typesTSfile, originalContent);

            taskGenerateTsDefinitions.execute();
        }
        assertFalse(logger.getLogs()
                .contains(TaskGenerateTsDefinitions.UPDATE_MESSAGE));
    }

    @Test
    void customTsDefinition_flowContents_tsDefinitionNotUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        Files.writeString(typesTSfile,
                """
                        import type { SchemaObject } from "../../types";
                        export type SchemaObjectMap = {
                            [Ref in string]?: SchemaObject;
                        };
                        declare module '*.css?inline' {
                          import type { CSSResultGroup } from 'lit';
                          const content: CSSResultGroup;
                          export default content;
                        }
                        declare module 'csstype' {
                          interface Properties {
                            [index: `--${string}`]: any;
                          }
                        }
                        export declare const jtdForms: readonly ["elements", "values", "discriminator", "properties", "optionalProperties", "enum", "type", "ref"];
                        export type JTDForm = typeof jtdForms[number];
                        }""");
        FileTime lastModifiedTime = Files.getLastModifiedTime(typesTSfile);
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        assertEquals(lastModifiedTime, Files.getLastModifiedTime(typesTSfile),
                "types.d.ts should not have been updated");
    }

    @Test
    void customTsDefinition_flowContentsDifferentWhiteSpace_tsDefinitionNotUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        Files.writeString(typesTSfile,
                """
                        import type { SchemaObject } from "../../types";
                        export type SchemaObjectMap = {
                            [Ref in string]?: SchemaObject;
                        };
                        declare module'*.css?inline'{
                          import type {CSSResultGroup} from 'lit';
                          const content: CSSResultGroup;
                          export default content;
                        }
                        declare module 'csstype' {
                          interface Properties {
                            [index:`--${string}`]: any;
                          }
                        }
                        export declare const jtdForms: readonly ["elements", "values", "discriminator", "properties", "optionalProperties", "enum", "type", "ref"];
                        export type JTDForm = typeof jtdForms[number];
                        }""");
        FileTime lastModifiedTime = Files.getLastModifiedTime(typesTSfile);
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        assertEquals(lastModifiedTime, Files.getLastModifiedTime(typesTSfile),
                "types.d.ts should not have been updated");
    }

    @Test
    void customTsDefinition_windowsEOL_flowContents_tsDefinitionNotUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        Files.writeString(typesTSfile,
                """
                        import type { SchemaObject } from "../../types";
                        export type SchemaObjectMap = {
                            [Ref in string]?: SchemaObject;
                        };
                        declare module '*.css?inline' {
                          import type { CSSResultGroup } from 'lit';
                          const content: CSSResultGroup;
                          export default content;
                        }
                        declare module 'csstype' {
                          interface Properties {
                            [index: `--${string}`]: any;
                          }
                        }
                        export declare const jtdForms: readonly ["elements", "values", "discriminator", "properties", "optionalProperties", "enum", "type", "ref"];
                        export type JTDForm = typeof jtdForms[number];
                        }"""
                        .replace("\n", "\r\n"));
        FileTime lastModifiedTime = Files.getLastModifiedTime(typesTSfile);
        taskGenerateTsDefinitions.execute();
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        assertEquals(lastModifiedTime, Files.getLastModifiedTime(typesTSfile),
                "types.d.ts should not have been updated");
    }

    @Test
    void customTsDefinition_flowContentsNotMatching_tsDefinitionNotUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        Files.writeString(typesTSfile,
                """
                        import type { SchemaObject } from "../../types";
                        export type SchemaObjectMap = {
                            [Ref in string]?: SchemaObject;
                        };
                        declare module '*.css?inline' {
                          something
                          import type { CSSResultGroup } from 'lit';
                          custom
                          const content: CSSResultGroup;
                          added
                          export default content;
                        }
                        export declare const jtdForms: readonly ["elements", "values", "discriminator", "properties", "optionalProperties", "enum", "type", "ref"];
                        export type JTDForm = typeof jtdForms[number];
                        }""");
        FileTime lastModifiedTime = Files.getLastModifiedTime(typesTSfile);
        ExecutionFailedException exception = assertThrows(
                ExecutionFailedException.class,
                taskGenerateTsDefinitions::execute);
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers
                .containsString(TaskGenerateTsDefinitions.CHECK_CONTENT_MESSAGE
                        .substring(0,
                                TaskGenerateTsDefinitions.CHECK_CONTENT_MESSAGE
                                        .indexOf("%s"))));
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        assertEquals(lastModifiedTime, Files.getLastModifiedTime(typesTSfile),
                "types.d.ts should not have been updated");
    }

    @Test
    void customTsDefinition_differentCSSModuleDefinition_tsDefinitionNotUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        Files.writeString(typesTSfile, """
                declare module '*.css?inline' {
                    custom configuration
                }""");
        FileTime lastModifiedTime = Files.getLastModifiedTime(typesTSfile);
        ExecutionFailedException exception = assertThrows(
                ExecutionFailedException.class,
                taskGenerateTsDefinitions::execute);
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers
                .containsString(TaskGenerateTsDefinitions.CHECK_CONTENT_MESSAGE
                        .substring(0,
                                TaskGenerateTsDefinitions.CHECK_CONTENT_MESSAGE
                                        .indexOf("%s"))));
        assertFalse(taskGenerateTsDefinitions.shouldGenerate(),
                "Should not generate types.d.ts when already existing");
        assertEquals(lastModifiedTime, Files.getLastModifiedTime(typesTSfile),
                "types.d.ts should not have been updated");
    }

    private void assertBackupFileCreated(String originalContent)
            throws IOException {
        File[] backups = taskGenerateTsDefinitions.getGeneratedFile()
                .getParentFile()
                .listFiles(file -> file.getName().endsWith(".bak"));
        assertEquals(1, backups.length);

        File backupFile = backups[0];
        assertTrue(backupFile.exists(),
                "Original types.d.ts backup should exist");
        assertEquals(originalContent, Files.readString(backupFile.toPath()));
    }

    private String readExpectedContent(boolean stripComments)
            throws IOException {
        String fileContent = taskGenerateTsDefinitions.getFileContent();
        if (stripComments) {
            fileContent = TaskGenerateTsDefinitions.COMMENT_LINE
                    .matcher(fileContent).replaceAll("");
        }
        return fileContent;
    }

    private String readPreviousContent(String suffix) throws IOException {
        return IOUtils.toString(
                getClass().getResourceAsStream(TS_DEFINITIONS + suffix), UTF_8);
    }

    private String stripEmptyLines(String input) {
        return input.replaceAll("(?m)^[ \t]*\r?\n", "");
    }
}
