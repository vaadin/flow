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

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TaskGenerateTsConfigTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File npmFolder;
    private TaskGenerateTsConfig taskGenerateTsConfig;

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();

        taskGenerateTsConfig = new TaskGenerateTsConfig(npmFolder);
    }

    @Test
    public void should_generateTsConfig_TsConfigNotExist() throws Exception {
        taskGenerateTsConfig.execute();
        Assert.assertFalse(
                "Should generate tsconfig.json when "
                        + "tsconfig.json doesn't exist",
                taskGenerateTsConfig.shouldGenerate());
        Assert.assertTrue("The generated tsconfig.json should not exist",
                taskGenerateTsConfig.getGeneratedFile().exists());
        Assert.assertEquals(
                "The generated content should be equals the default content",
                taskGenerateTsConfig.getFileContent(),
                IOUtils.toString(
                        taskGenerateTsConfig.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }

    @Test
    public void should_notGenerateTsConfig_TsConfigExist() throws Exception {
        Files.createFile(new File(npmFolder, "tsconfig.json").toPath());
        taskGenerateTsConfig.execute();
        Assert.assertFalse(
                "Should not generate tsconfig.json when tsconfig.json exists",
                taskGenerateTsConfig.shouldGenerate());
        Assert.assertTrue("The tsconfig.json should already exist",
                taskGenerateTsConfig.getGeneratedFile().exists());
    }

}
