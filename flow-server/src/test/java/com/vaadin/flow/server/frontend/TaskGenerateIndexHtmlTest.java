/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_HTML;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TaskGenerateIndexHtmlTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File frontendFolder;
    private File outputFolder;
    private TaskGenerateIndexHtml taskGenerateIndexHtml;

    @Before
    public void setUp() throws IOException {
        frontendFolder = temporaryFolder.newFolder();
        outputFolder = temporaryFolder.newFolder();
        taskGenerateIndexHtml = new TaskGenerateIndexHtml(frontendFolder,
                outputFolder);
    }

    @Test
    public void should_loadCorrectContentOfDefaultFile() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(INDEX_HTML),
                StandardCharsets.UTF_8);

        Assert.assertEquals(
                "Should load correct default content from index.html",
                defaultContent, taskGenerateIndexHtml.getFileContent());
    }

    @Test
    public void should_notGenerateIndexHtml_IndexHtmlExists() throws Exception {
        Files.createFile(new File(frontendFolder, "index.html").toPath());
        taskGenerateIndexHtml.execute();
        Assert.assertFalse(
                "Should not generate index.html while it exists in"
                        + " the frontend folder",
                taskGenerateIndexHtml.shouldGenerate());
        Assert.assertFalse("The generated file should not exists",
                taskGenerateIndexHtml.getGeneratedFile().exists());
    }

    @Test
    public void should_generateIndexHtml_IndexHtmlNotExist() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(INDEX_HTML),
                StandardCharsets.UTF_8);
        taskGenerateIndexHtml.execute();
        Assert.assertTrue(
                "Should generate index.html when it doesn't exists in"
                        + " the frontend folder",
                taskGenerateIndexHtml.shouldGenerate());
        Assert.assertTrue("The generated file should exists",
                taskGenerateIndexHtml.getGeneratedFile().exists());

        Assert.assertEquals("Should have default content of index.html",
                defaultContent,
                IOUtils.toString(
                        taskGenerateIndexHtml.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }
}
