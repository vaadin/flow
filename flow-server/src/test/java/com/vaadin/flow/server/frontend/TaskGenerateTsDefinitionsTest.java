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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;

import static com.vaadin.flow.server.frontend.TaskGenerateTsDefinitions.TS_DEFINITIONS;
import static java.nio.charset.StandardCharsets.UTF_8;

@NotThreadSafe
public class TaskGenerateTsDefinitionsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File outputFolder;
    private TaskGenerateTsDefinitions taskGenerateTsDefinitions;

    @Before
    public void setUp() throws IOException {
        outputFolder = temporaryFolder.newFolder();
        Options options = new Options(Mockito.mock(Lookup.class), outputFolder);

        taskGenerateTsDefinitions = new TaskGenerateTsDefinitions(options);
        taskGenerateTsDefinitions.warningEmitted = false;
    }

    @Test
    public void should_generateTsDefinitions_TsDefinitionsNotExistAndTsConfigExists()
            throws Exception {
        Files.createFile(
                new File(outputFolder, TaskGenerateTsConfig.TSCONFIG_JSON)
                        .toPath());
        taskGenerateTsDefinitions.execute();
        Assert.assertFalse(
                "Should generate types.d.ts when tsconfig.json exists and "
                        + "types.d.ts doesn't exist",
                taskGenerateTsDefinitions.shouldGenerate());
        Assert.assertTrue("The generated types.d.ts should not exist",
                taskGenerateTsDefinitions.getGeneratedFile().exists());
        Assert.assertEquals(
                "The generated content should be equals the default content",
                taskGenerateTsDefinitions.getFileContent(),
                IOUtils.toString(
                        taskGenerateTsDefinitions.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }

    @Test
    public void should_notGenerateTsDefinitions_TsConfigNotExist()
            throws Exception {
        taskGenerateTsDefinitions.execute();
        Assert.assertFalse(
                "Should not generate types.d.ts when tsconfig.json "
                        + "doesn't exist",
                taskGenerateTsDefinitions.shouldGenerate());
        Assert.assertFalse("The types.d.ts should not exist",
                taskGenerateTsDefinitions.getGeneratedFile().exists());
    }

    @Test
    public void tsDefinition_upToDate_tsDefinitionNotUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String expectedContent = readExpectedContent(false);
        Files.writeString(typesTSfile, expectedContent);
        FileTime lastModifiedTime = Files.getLastModifiedTime(typesTSfile);
        taskGenerateTsDefinitions.execute();
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = Files.readString(typesTSfile);
        Assert.assertEquals("types.d.ts should not have been updated",
                lastModifiedTime, Files.getLastModifiedTime(typesTSfile));
        Assert.assertEquals("types.d.ts should have been replaced",
                updatedContent, expectedContent);
    }

    @Test
    public void tsDefinition_oldFlowContents_tsDefinitionUpdated()
            throws Exception {
        tsDefinition_oldFlowContents_tsDefinitionUpdated(".v1");
        tsDefinition_oldFlowContents_tsDefinitionUpdated(".v2");
    }

    private void tsDefinition_oldFlowContents_tsDefinitionUpdated(String suffix)
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        Files.writeString(typesTSfile, readPreviousContent(suffix));
        taskGenerateTsDefinitions.execute();
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = Files.readString(typesTSfile);
        Assert.assertEquals("types.d.ts should have been replaced",
                updatedContent, readExpectedContent(false));
    }

    @Test
    public void tsDefinition_oldFlowContents_missingLastEOL_tsDefinitionUpdated()
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
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = Files.readString(typesTSfile);
        Assert.assertEquals("types.d.ts should have been replaced",
                updatedContent, readExpectedContent(false));
    }

    @Test
    public void tsDefinition_oldHillaContents_tsDefinitionUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String hillaTsDef = IOUtils.toString(
                getClass().getResourceAsStream(TS_DEFINITIONS + ".hilla.v1"),
                UTF_8);
        Files.writeString(typesTSfile, hillaTsDef);
        taskGenerateTsDefinitions.execute();
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                updatedContent,
                CoreMatchers.containsString(readExpectedContent(true)));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    public void tsDefinition_oldHillaContents_ignoringMultilineComments_tsDefinitionUpdated()
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
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                updatedContent,
                CoreMatchers.containsString(readExpectedContent(true)));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    public void tsDefinition_oldHillaContents_ignoringSingleLineComments_tsDefinitionUpdated()
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
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                updatedContent,
                CoreMatchers.containsString(readExpectedContent(true)));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    public void tsDefinition_oldHillaV2Contents_tsDefinitionUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        String hillaTsDef = IOUtils.toString(
                getClass().getResourceAsStream(TS_DEFINITIONS + ".hilla.v2"),
                UTF_8);
        Files.writeString(typesTSfile, hillaTsDef);
        taskGenerateTsDefinitions.execute();
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                stripEmptyLines(updatedContent), CoreMatchers.containsString(
                        stripEmptyLines(readExpectedContent(true))));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    public void tsDefinition_oldHillaV2Contents_ignoringMultilineComments_tsDefinitionUpdated()
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
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                stripEmptyLines(updatedContent), CoreMatchers.containsString(
                        stripEmptyLines(readExpectedContent(true))));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    public void tsDefinition_oldHillaV2Contents_ignoringSingleLineComments_tsDefinitionUpdated()
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
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                stripEmptyLines(updatedContent), CoreMatchers.containsString(
                        stripEmptyLines(readExpectedContent(true))));
        MatcherAssert.assertThat("types.d.ts should contain original content",
                updatedContent, CoreMatchers.containsString(hillaTsDef));
    }

    @Test
    public void customTsDefinition_missingFlowContents_tsDefinitionUpdatedAndWarningLogged()
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
        Assert.assertTrue(logger.getLogs()
                .contains(TaskGenerateTsDefinitions.UPDATE_MESSAGE));
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = Files.readString(typesTSfile);
        MatcherAssert.assertThat("types.d.ts should have been updated",
                updatedContent,
                CoreMatchers.containsString(readExpectedContent(true)));
        assertBackupFileCreated(originalContent);
    }

    @Test
    public void customTsDefinition_v1FlowContents_tsDefinitionUpdatedAndWarningLogged()
            throws Exception {
        customTsDefinition_oldFlowContents_tsDefinitionUpdatedAndWarningLogged(
                ".v1");
    }

    @Test
    public void customTsDefinition_v2FlowContents_tsDefinitionUpdatedAndWarningLogged()
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
        Assert.assertTrue(logger.getLogs()
                .contains(TaskGenerateTsDefinitions.UPDATE_MESSAGE));

        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        String updatedContent = stripEmptyLines(Files.readString(typesTSfile));
        MatcherAssert.assertThat("types.d.ts should have been updated",
                updatedContent, CoreMatchers.containsString(
                        stripEmptyLines(readExpectedContent(true))));
        assertBackupFileCreated(originalContent);
    }

    @Test
    public void contentUpdateForSecondTime_tsDefinitionUpdatedAndWarningLoggedOnce()
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

            Assert.assertTrue(logger.getLogs()
                    .contains(TaskGenerateTsDefinitions.UPDATE_MESSAGE));

            logger.clearLogs();

            Files.writeString(typesTSfile, originalContent);

            taskGenerateTsDefinitions.execute();
        }
        Assert.assertFalse(logger.getLogs()
                .contains(TaskGenerateTsDefinitions.UPDATE_MESSAGE));
    }

    @Test
    public void customTsDefinition_flowContents_tsDefinitionNotUpdated()
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
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        Assert.assertEquals("types.d.ts should not have been updated",
                lastModifiedTime, Files.getLastModifiedTime(typesTSfile));
    }

    @Test
    public void customTsDefinition_flowContentsDifferentWhiteSpace_tsDefinitionNotUpdated()
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
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        Assert.assertEquals("types.d.ts should not have been updated",
                lastModifiedTime, Files.getLastModifiedTime(typesTSfile));
    }

    @Test
    public void customTsDefinition_windowsEOL_flowContents_tsDefinitionNotUpdated()
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
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        Assert.assertEquals("types.d.ts should not have been updated",
                lastModifiedTime, Files.getLastModifiedTime(typesTSfile));
    }

    @Test
    public void customTsDefinition_flowContentsNotMatching_tsDefinitionNotUpdated()
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
        ExecutionFailedException exception = Assert.assertThrows(
                ExecutionFailedException.class,
                taskGenerateTsDefinitions::execute);
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers
                .containsString(TaskGenerateTsDefinitions.CHECK_CONTENT_MESSAGE
                        .substring(0,
                                TaskGenerateTsDefinitions.CHECK_CONTENT_MESSAGE
                                        .indexOf("%s"))));
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        Assert.assertEquals("types.d.ts should not have been updated",
                lastModifiedTime, Files.getLastModifiedTime(typesTSfile));
    }

    @Test
    public void customTsDefinition_differentCSSModuleDefinition_tsDefinitionNotUpdated()
            throws Exception {
        Path typesTSfile = new File(outputFolder, "types.d.ts").toPath();
        Files.writeString(typesTSfile, """
                declare module '*.css?inline' {
                    custom configuration
                }""");
        FileTime lastModifiedTime = Files.getLastModifiedTime(typesTSfile);
        ExecutionFailedException exception = Assert.assertThrows(
                ExecutionFailedException.class,
                taskGenerateTsDefinitions::execute);
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers
                .containsString(TaskGenerateTsDefinitions.CHECK_CONTENT_MESSAGE
                        .substring(0,
                                TaskGenerateTsDefinitions.CHECK_CONTENT_MESSAGE
                                        .indexOf("%s"))));
        Assert.assertFalse(
                "Should not generate types.d.ts when already existing",
                taskGenerateTsDefinitions.shouldGenerate());
        Assert.assertEquals("types.d.ts should not have been updated",
                lastModifiedTime, Files.getLastModifiedTime(typesTSfile));
    }

    private void assertBackupFileCreated(String originalContent)
            throws IOException {
        File[] backups = taskGenerateTsDefinitions.getGeneratedFile()
                .getParentFile()
                .listFiles(file -> file.getName().endsWith(".bak"));
        Assert.assertEquals(1, backups.length);

        File backupFile = backups[0];
        Assert.assertTrue("Original types.d.ts backup should exist",
                backupFile.exists());
        Assert.assertEquals(originalContent,
                Files.readString(backupFile.toPath()));
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
