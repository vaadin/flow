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
import com.vaadin.flow.internal.FrontendUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskGenerateWebComponentHtmlTest {
    @TempDir
    File temporaryFolder;

    private File frontendFolder;
    private TaskGenerateWebComponentHtml taskGenerateWebComponentHtml;

    @BeforeEach
    void setup() throws IOException {
        frontendFolder = Files
                .createTempDirectory(temporaryFolder.toPath(), "tmp").toFile();
        Options options = new Options(Mockito.mock(Lookup.class), null)
                .withFrontendDirectory(frontendFolder);
        taskGenerateWebComponentHtml = new TaskGenerateWebComponentHtml(
                options);
    }

    @Test
    void should_loadCorrectContentOfDefaultFile() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass()
                        .getResourceAsStream(FrontendUtils.WEB_COMPONENT_HTML),
                StandardCharsets.UTF_8);

        assertEquals(defaultContent,
                taskGenerateWebComponentHtml.getFileContent(),
                "Should load correct default content from web-component.html");
    }

    @Test
    void should_notOverwriteWebComponentHtml_webComponentHtmlExists()
            throws Exception {
        File webComponentHtml = new File(frontendFolder, "web-component.html");
        Files.createFile(webComponentHtml.toPath());
        taskGenerateWebComponentHtml.execute();
        assertFalse(taskGenerateWebComponentHtml.shouldGenerate(),
                "Should not generate web-component.html while it exists in the frontend folder");
        assertEquals("", IOUtils.toString(webComponentHtml.toURI(),
                StandardCharsets.UTF_8));
    }

    @Test
    void should_generateWebComponentHtml_webComponentHtmlNotExist()
            throws Exception {
        String defaultContent = IOUtils.toString(
                getClass()
                        .getResourceAsStream(FrontendUtils.WEB_COMPONENT_HTML),
                StandardCharsets.UTF_8);
        assertTrue(taskGenerateWebComponentHtml.shouldGenerate(),
                "Should generate web-component.html when it doesn't exists in the frontend folder");

        taskGenerateWebComponentHtml.execute();

        assertTrue(taskGenerateWebComponentHtml.getGeneratedFile().exists(),
                "The generated file should exists");

        assertEquals(defaultContent,
                IOUtils.toString(
                        taskGenerateWebComponentHtml.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8),
                "Should have default content of web-component.html");
    }
}
