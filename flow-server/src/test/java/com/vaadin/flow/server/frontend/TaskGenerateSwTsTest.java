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

import com.vaadin.flow.di.Lookup;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC;
import static com.vaadin.flow.server.frontend.FrontendUtils.SERVICE_WORKER_SRC_JS;

public class TaskGenerateSwTsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File frontendFolder;
    private File outputFolder;
    private TaskGenerateSwTs taskGenerateSwTs;

    @Before
    public void setUp() throws IOException {
        frontendFolder = temporaryFolder.newFolder();
        outputFolder = temporaryFolder.newFolder();
        Options options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withFrontendDirectory(frontendFolder)
                .withBuildDirectory(outputFolder.getName());
        taskGenerateSwTs = new TaskGenerateSwTs(options);
    }

    @Test
    public void should_loadCorrectContentOfDefaultFile() throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(SERVICE_WORKER_SRC),
                StandardCharsets.UTF_8);

        Assert.assertEquals("Should load correct default content from sw.ts",
                defaultContent, taskGenerateSwTs.getFileContent());
    }

    @Test
    public void should_notGenerateSwTs_SwTsExists()
            throws Exception {
        Files.createFile(new File(frontendFolder, SERVICE_WORKER_SRC).toPath());
        taskGenerateSwTs.execute();
        Assert.assertFalse(
                "Should not generate sw.ts while it exists in"
                        + " the frontend folder",
                taskGenerateSwTs.shouldGenerate());
        Assert.assertFalse("The generated file should not exists",
                taskGenerateSwTs.getGeneratedFile().exists());
    }

    @Test
    public void should_notGenerateSwTs_SwJsExists()
            throws Exception {
        Files.createFile(
                new File(frontendFolder, SERVICE_WORKER_SRC_JS).toPath());
        taskGenerateSwTs.execute();
        Assert.assertFalse(
                "Should not generate sw.ts while sw.js exists in"
                        + " the frontend folder",
                taskGenerateSwTs.shouldGenerate());
        Assert.assertFalse("The generated file should not exists",
                taskGenerateSwTs.getGeneratedFile().exists());
    }

    @Test
    public void should_generateSwTs_SwTsNotExist()
            throws Exception {
        String defaultContent = IOUtils.toString(
                getClass().getResourceAsStream(SERVICE_WORKER_SRC),
                StandardCharsets.UTF_8);
        taskGenerateSwTs.execute();
        Assert.assertTrue(
                "Should generate sw.ts when it doesn't exists in"
                        + " the frontend folder",
                taskGenerateSwTs.shouldGenerate());
        Assert.assertTrue("The generated file should exists",
                taskGenerateSwTs.getGeneratedFile().exists());

        Assert.assertEquals("Should have default content of sw.ts",
                defaultContent,
                IOUtils.toString(
                        taskGenerateSwTs.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }
}
