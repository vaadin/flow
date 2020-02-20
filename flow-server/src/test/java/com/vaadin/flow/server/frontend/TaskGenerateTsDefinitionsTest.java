/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


public class TaskGenerateTsDefinitionsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File outputFolder;
    private TaskGenerateTsDefinitions taskGenerateTsDefinitions;

    @Before
    public void setUp() throws IOException {
        outputFolder = temporaryFolder.newFolder();
        taskGenerateTsDefinitions = new TaskGenerateTsDefinitions(outputFolder);
    }

    @Test
    public void should_generateTsDefinitions_TsDefinitionsNotExistAndTsConfigExists()
            throws Exception {
        Files.createFile(new File(outputFolder, TaskGenerateTsConfig.TSCONFIG_JSON)
                .toPath());
        taskGenerateTsDefinitions.execute();
        Assert.assertFalse(
                "Should generate types.d.ts when tsconfig.json exists and "
                        + "types.d.ts doesn't exist",
                taskGenerateTsDefinitions.shouldGenerate());
        Assert.assertTrue("The generated types.d.ts should not exist",
                taskGenerateTsDefinitions.getGeneratedFile().exists());
        Assert.assertEquals(
                "The generated content should be equals the default content",
                taskGenerateTsDefinitions.getFileContent(),
                IOUtils.toString(
                        taskGenerateTsDefinitions.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }

    @Test
    public void should_notGenerateTsDefinitions_TsConfigNotExist()
            throws Exception {
        Files.createFile(new File(outputFolder, "types.d.ts").toPath());
        taskGenerateTsDefinitions.execute();
        Assert.assertFalse(
                "Should not generate types.d.ts when tsconfig.json "
                        + "doesn't exist",
                taskGenerateTsDefinitions.shouldGenerate());
        Assert.assertTrue("The types.d.ts should already exist",
                taskGenerateTsDefinitions.getGeneratedFile().exists());
    }

}
