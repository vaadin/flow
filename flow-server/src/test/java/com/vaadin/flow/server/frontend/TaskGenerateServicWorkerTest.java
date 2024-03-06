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

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC_JS;;

public class TaskGenerateServicWorkerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File frontendFolder;
    private File outputFolder;
    private TaskGenerateServiceWorker taskGenerateServiceWorker;

    @Before
    public void setUp() throws IOException {
        frontendFolder = temporaryFolder.newFolder();
        outputFolder = temporaryFolder.newFolder();
        taskGenerateServiceWorker = new TaskGenerateServiceWorker(
                frontendFolder, outputFolder);
    }

    @Test
    public void should_loadCorrectContentOfDefaultFile() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(SERVICE_WORKER_SRC),
                StandardCharsets.UTF_8);

        Assert.assertEquals("Should load correct default content from sw.ts",
                defaultContent, taskGenerateServiceWorker.getFileContent());
    }

    @Test
    public void should_notGenerateServiceWorker_ServiceWorkerExists()
            throws Exception {
        Files.createFile(new File(frontendFolder, SERVICE_WORKER_SRC).toPath());
        taskGenerateServiceWorker.execute();
        Assert.assertFalse(
                "Should not generate sw.ts while it exists in"
                        + " the frontend folder",
                taskGenerateServiceWorker.shouldGenerate());
        Assert.assertFalse("The generated file should not exists",
                taskGenerateServiceWorker.getGeneratedFile().exists());
    }

    @Test
    public void should_notGenerateServiceWorker_ServiceWorkerJsExists()
            throws Exception {
        Files.createFile(
                new File(frontendFolder, SERVICE_WORKER_SRC_JS).toPath());
        taskGenerateServiceWorker.execute();
        Assert.assertFalse(
                "Should not generate sw.ts while sw.js exists in"
                        + " the frontend folder",
                taskGenerateServiceWorker.shouldGenerate());
        Assert.assertFalse("The generated file should not exists",
                taskGenerateServiceWorker.getGeneratedFile().exists());
    }

    @Test
    public void should_generateServiceWorker_ServiceWorkerNotExist()
            throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(SERVICE_WORKER_SRC),
                StandardCharsets.UTF_8);
        taskGenerateServiceWorker.execute();
        Assert.assertTrue(
                "Should generate sw.ts when it doesn't exists in"
                        + " the frontend folder",
                taskGenerateServiceWorker.shouldGenerate());
        Assert.assertTrue("The generated file should exists",
                taskGenerateServiceWorker.getGeneratedFile().exists());

        Assert.assertEquals("Should have default content of sw.ts",
                defaultContent,
                IOUtils.toString(
                        taskGenerateServiceWorker.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }
}
