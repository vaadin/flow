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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;

import static com.vaadin.flow.internal.FrontendUtils.INDEX_HTML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskGenerateIndexHtmlTest {
    @TempDir
    File temporaryFolder;

    private File frontendFolder;
    private TaskGenerateIndexHtml taskGenerateIndexHtml;

    @BeforeEach
    void setUp() throws IOException {
        frontendFolder = Files
                .createTempDirectory(temporaryFolder.toPath(), "tmp").toFile();
        Options options = new Options(Mockito.mock(Lookup.class), null)
                .withFrontendDirectory(frontendFolder);
        taskGenerateIndexHtml = new TaskGenerateIndexHtml(options);
    }

    @Test
    void should_loadCorrectContentOfDefaultFile() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(INDEX_HTML),
                StandardCharsets.UTF_8);

        assertEquals(defaultContent, taskGenerateIndexHtml.getFileContent(),
                "Should load correct default content from index.html");
    }

    @Test
    void should_notOverwriteIndexHtml_IndexHtmlExists() throws Exception {
        File indexhtml = new File(frontendFolder, "index.html");
        Files.createFile(indexhtml.toPath());
        taskGenerateIndexHtml.execute();
        assertFalse(taskGenerateIndexHtml.shouldGenerate(),
                "Should not generate index.html while it exists in the frontend folder");
        assertEquals("",
                IOUtils.toString(indexhtml.toURI(), StandardCharsets.UTF_8));
    }

    @Test
    void should_generateIndexHtml_IndexHtmlNotExist() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(INDEX_HTML),
                StandardCharsets.UTF_8);
        assertTrue(taskGenerateIndexHtml.shouldGenerate(),
                "Should generate index.html when it doesn't exists in the frontend folder");

        taskGenerateIndexHtml.execute();

        assertTrue(taskGenerateIndexHtml.getGeneratedFile().exists(),
                "The generated file should exists");

        assertEquals(defaultContent,
                IOUtils.toString(
                        taskGenerateIndexHtml.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8),
                "Should have default content of index.html");
    }
}
