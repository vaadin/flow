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

import static com.vaadin.flow.server.frontend.FrontendUtils.WEB_COMPONENT_HTML;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TaskGenerateWebComponentHtmlTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File frontendFolder;
    private TaskGenerateWebComponentHtml taskGenerateWebComponentHtml;

    @Before
    public void setup() throws IOException {
        frontendFolder = temporaryFolder.newFolder();
        taskGenerateWebComponentHtml = new TaskGenerateWebComponentHtml(
                frontendFolder);
    }

    @Test
    public void should_loadCorrectContentOfDefaultFile() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(WEB_COMPONENT_HTML),
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
                getClass().getResourceAsStream(WEB_COMPONENT_HTML),
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
