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

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;

public class TaskGenerateWebComponentHtmlTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File frontendFolder;
    private TaskGenerateWebComponentHtml taskGenerateWebComponentHtml;

    @Before
    public void setup() throws IOException {
        frontendFolder = temporaryFolder.newFolder();
        Options options = new Options(Mockito.mock(Lookup.class), null)
                .withFrontendDirectory(frontendFolder);
        taskGenerateWebComponentHtml = new TaskGenerateWebComponentHtml(
                options);
    }

    @Test
    public void should_loadCorrectContentOfDefaultFile() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass()
                        .getResourceAsStream(FrontendUtils.WEB_COMPONENT_HTML),
                StandardCharsets.UTF_8);

        Assert.assertEquals(
                "Should load correct default content from web-component.html",
                defaultContent, taskGenerateWebComponentHtml.getFileContent());
    }

    @Test
    public void should_notOverwriteWebComponentHtml_webComponentHtmlExists()
            throws Exception {
        File webComponentHtml = new File(frontendFolder, "web-component.html");
        Files.createFile(webComponentHtml.toPath());
        taskGenerateWebComponentHtml.execute();
        Assert.assertFalse(
                "Should not generate web-component.html while it exists in the frontend folder",
                taskGenerateWebComponentHtml.shouldGenerate());
        Assert.assertEquals("", IOUtils.toString(webComponentHtml.toURI(),
                StandardCharsets.UTF_8));
    }

    @Test
    public void should_generateWebComponentHtml_webComponentHtmlNotExist()
            throws Exception {
        String defaultContent = IOUtils.toString(
                getClass()
                        .getResourceAsStream(FrontendUtils.WEB_COMPONENT_HTML),
                StandardCharsets.UTF_8);
        Assert.assertTrue(
                "Should generate web-component.html when it doesn't exists in the frontend folder",
                taskGenerateWebComponentHtml.shouldGenerate());

        taskGenerateWebComponentHtml.execute();

        Assert.assertTrue("The generated file should exists",
                taskGenerateWebComponentHtml.getGeneratedFile().exists());

        Assert.assertEquals("Should have default content of web-component.html",
                defaultContent,
                IOUtils.toString(
                        taskGenerateWebComponentHtml.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }
}
