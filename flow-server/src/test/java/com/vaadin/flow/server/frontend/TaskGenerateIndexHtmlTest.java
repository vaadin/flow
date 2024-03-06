/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
    private TaskGenerateIndexHtml taskGenerateIndexHtml;

    @Before
    public void setUp() throws IOException {
        frontendFolder = temporaryFolder.newFolder();
        taskGenerateIndexHtml = new TaskGenerateIndexHtml(frontendFolder);
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
    public void should_notOverwriteIndexHtml_IndexHtmlExists()
            throws Exception {
        File indexhtml = new File(frontendFolder, "index.html");
        Files.createFile(indexhtml.toPath());
        taskGenerateIndexHtml.execute();
        Assert.assertFalse(
                "Should not generate index.html while it exists in the frontend folder",
                taskGenerateIndexHtml.shouldGenerate());
        Assert.assertEquals("",
                IOUtils.toString(indexhtml.toURI(), StandardCharsets.UTF_8));
    }

    @Test
    public void should_generateIndexHtml_IndexHtmlNotExist() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(INDEX_HTML),
                StandardCharsets.UTF_8);
        Assert.assertTrue(
                "Should generate index.html when it doesn't exists in the frontend folder",
                taskGenerateIndexHtml.shouldGenerate());

        taskGenerateIndexHtml.execute();

        Assert.assertTrue("The generated file should exists",
                taskGenerateIndexHtml.getGeneratedFile().exists());

        Assert.assertEquals("Should have default content of index.html",
                defaultContent,
                IOUtils.toString(
                        taskGenerateIndexHtml.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }
}
