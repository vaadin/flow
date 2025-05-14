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

import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC_JS;

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

import com.vaadin.flow.di.Lookup;;

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
        Options options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withFrontendDirectory(frontendFolder)
                .withBuildDirectory(outputFolder.getName());
        taskGenerateServiceWorker = new TaskGenerateServiceWorker(options);
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
