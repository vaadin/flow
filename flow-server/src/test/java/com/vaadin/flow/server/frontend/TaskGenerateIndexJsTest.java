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

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_JS;

public class TaskGenerateIndexJsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File frontendFolder;
    private File outputFolder;
    private File generatedImports;
    private TaskGenerateIndexJs taskGenerateIndexJs;

    @Before
    public void setUp() throws IOException {
        frontendFolder = temporaryFolder.newFolder();
        outputFolder = temporaryFolder.newFolder();
        generatedImports = temporaryFolder.newFile("flow-generated-imports.js");
        taskGenerateIndexJs = new TaskGenerateIndexJs(frontendFolder,
                generatedImports, outputFolder);
    }

    @Test
    public void should_notGenerateIndexJs_IndexJsExists() throws Exception {
        Files.createFile(new File(frontendFolder, INDEX_JS).toPath());
        taskGenerateIndexJs.execute();
        Assert.assertFalse(
                "Should not generate index.js when it exists in"
                        + " the frontend folder",
                taskGenerateIndexJs.shouldGenerate());
        Assert.assertFalse("The generated file should not exists",
                taskGenerateIndexJs.getGeneratedFile().exists());
    }

    @Test
    public void should_generateIndexJs_IndexJsNotExist() throws Exception {

        taskGenerateIndexJs.execute();
        Assert.assertTrue(
                "Should generate index.js when it doesn't exist in"
                        + " the frontend folder",
                taskGenerateIndexJs.shouldGenerate());
        Assert.assertTrue("The generated file should exists",
                taskGenerateIndexJs.getGeneratedFile().exists());

        Assert.assertEquals("Should have default content of index.js",
                taskGenerateIndexJs.getFileContent(),
                IOUtils.toString(taskGenerateIndexJs.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }
@Test
    public void should_notContainWindowSeparator_whenRelativizingPath() throws Exception
    {
        String windowSeparator = TaskGenerateIndexJs
                .ensureValidRelativePath("frontend\\generated-flow-imports.js");
        Assert.assertEquals("Window separator should be replaced",
                "./frontend/generated-flow-imports.js", windowSeparator);

        String unixSeparator = TaskGenerateIndexJs
                .ensureValidRelativePath("frontend/generated-flow-imports.js");
        Assert.assertEquals("Unix separator should be kept",
                "./frontend/generated-flow-imports.js", unixSeparator);

        String customPath = TaskGenerateIndexJs.ensureValidRelativePath(
                "../custom-frontend/generated-flow-imports.js");
        Assert.assertEquals(
                "Should not append './' if it is already a relative path",
                "../custom-frontend/generated-flow-imports.js", customPath);
    }
}
