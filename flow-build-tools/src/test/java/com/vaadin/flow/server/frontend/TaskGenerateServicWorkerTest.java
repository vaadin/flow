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

import static com.vaadin.flow.internal.FrontendUtils.SERVICE_WORKER_SRC;
import static com.vaadin.flow.internal.FrontendUtils.SERVICE_WORKER_SRC_JS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskGenerateServicWorkerTest {
    @TempDir
    File temporaryFolder;

    private File frontendFolder;
    private File outputFolder;
    private TaskGenerateServiceWorker taskGenerateServiceWorker;

    @BeforeEach
    void setUp() throws IOException {
        frontendFolder = Files
                .createTempDirectory(temporaryFolder.toPath(), "tmp").toFile();
        outputFolder = Files
                .createTempDirectory(temporaryFolder.toPath(), "tmp").toFile();
        Options options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder).withFrontendDirectory(frontendFolder)
                .withBuildDirectory(outputFolder.getName());
        taskGenerateServiceWorker = new TaskGenerateServiceWorker(options);
    }

    @Test
    void should_loadCorrectContentOfDefaultFile() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(SERVICE_WORKER_SRC),
                StandardCharsets.UTF_8);

        assertEquals(defaultContent, taskGenerateServiceWorker.getFileContent(),
                "Should load correct default content from sw.ts");
    }

    @Test
    void should_notGenerateServiceWorker_ServiceWorkerExists()
            throws Exception {
        Files.createFile(new File(frontendFolder, SERVICE_WORKER_SRC).toPath());
        taskGenerateServiceWorker.execute();
        assertFalse(taskGenerateServiceWorker.shouldGenerate(),
                "Should not generate sw.ts while it exists in"
                        + " the frontend folder");
        assertFalse(taskGenerateServiceWorker.getGeneratedFile().exists(),
                "The generated file should not exists");
    }

    @Test
    void should_notGenerateServiceWorker_ServiceWorkerJsExists()
            throws Exception {
        Files.createFile(
                new File(frontendFolder, SERVICE_WORKER_SRC_JS).toPath());
        taskGenerateServiceWorker.execute();
        assertFalse(taskGenerateServiceWorker.shouldGenerate(),
                "Should not generate sw.ts while sw.js exists in"
                        + " the frontend folder");
        assertFalse(taskGenerateServiceWorker.getGeneratedFile().exists(),
                "The generated file should not exists");
    }

    @Test
    void should_generateServiceWorker_ServiceWorkerNotExist() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(SERVICE_WORKER_SRC),
                StandardCharsets.UTF_8);
        taskGenerateServiceWorker.execute();
        assertTrue(taskGenerateServiceWorker.shouldGenerate(),
                "Should generate sw.ts when it doesn't exists in"
                        + " the frontend folder");
        assertTrue(taskGenerateServiceWorker.getGeneratedFile().exists(),
                "The generated file should exists");

        assertEquals(defaultContent,
                IOUtils.toString(
                        taskGenerateServiceWorker.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8),
                "Should have default content of sw.ts");
    }
}
