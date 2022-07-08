/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_JS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;

public class TaskGenerateIndexTsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File frontendFolder;
    private File outputFolder;
    private File generatedImports;
    private TaskGenerateIndexTs taskGenerateIndexTs;

    @Before
    public void setUp() throws IOException {
        frontendFolder = temporaryFolder.newFolder(FRONTEND);
        outputFolder = temporaryFolder.newFolder(TARGET);
        File generatedFolder = temporaryFolder.newFolder(TARGET, FRONTEND);
        generatedImports = new File(generatedFolder,
                "flow-generated-imports.js");
        generatedImports.createNewFile();
        taskGenerateIndexTs = new TaskGenerateIndexTs(frontendFolder,
                generatedImports, outputFolder);
    }

    @Test
    public void should_reported_routing_client_when_IndexJsExists()
            throws Exception {
        Files.createFile(new File(frontendFolder, INDEX_JS).toPath());
        taskGenerateIndexTs.execute();
        Assert.assertTrue(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_CLIENT.equals(e.getName())));
    }

    @Test
    public void should_reported_routing_client_when_IndexTsExists()
            throws Exception {
        Files.createFile(new File(frontendFolder, INDEX_TS).toPath());
        taskGenerateIndexTs.execute();
        Assert.assertTrue(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_CLIENT.equals(e.getName())));
    }

    @Test
    public void should_not_reported_routing_client() throws Exception {
        taskGenerateIndexTs.execute();
        Assert.assertFalse(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_CLIENT.equals(e.getName())));
    }

    @Test
    public void should_notGenerateIndexTs_IndexJsExists() throws Exception {
        Files.createFile(new File(frontendFolder, INDEX_JS).toPath());
        taskGenerateIndexTs.execute();
        Assert.assertFalse(
                "Should not generate index.ts when index.js exists in"
                        + " the frontend folder",
                taskGenerateIndexTs.shouldGenerate());
        Assert.assertFalse("The generated file should not exists",
                taskGenerateIndexTs.getGeneratedFile().exists());
    }

    @Test
    public void should_notGenerateIndexTs_IndexTsExists() throws Exception {
        Files.createFile(new File(frontendFolder, INDEX_TS).toPath());
        taskGenerateIndexTs.execute();
        Assert.assertFalse(
                "Should not generate index.ts when index.ts exists in"
                        + " the frontend folder",
                taskGenerateIndexTs.shouldGenerate());
        Assert.assertFalse("The generated file should not exists",
                taskGenerateIndexTs.getGeneratedFile().exists());
    }

    @Test
    public void should_generateIndexJs_IndexJsNotExist() throws Exception {

        taskGenerateIndexTs.execute();
        Assert.assertTrue(
                "Should generate index.ts when it doesn't exist in"
                        + " the frontend folder",
                taskGenerateIndexTs.shouldGenerate());
        Assert.assertTrue("The generated file should exists",
                taskGenerateIndexTs.getGeneratedFile().exists());

        Assert.assertEquals("Should have default content of index.ts",
                taskGenerateIndexTs.getFileContent(),
                IOUtils.toString(taskGenerateIndexTs.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8));
    }

    @Test
    public void replacedImport_should_beRelativeTo_targetAndFrontend()
            throws Exception {
        String content = taskGenerateIndexTs.getFileContent();
        Assert.assertTrue(content.contains(
                "import('../../target/frontend/flow-generated-imports'"));

        // custom frontend folder
        taskGenerateIndexTs = new TaskGenerateIndexTs(
                temporaryFolder.newFolder("src", "main", FRONTEND),
                generatedImports, outputFolder);
        content = taskGenerateIndexTs.getFileContent();
        Assert.assertTrue(content.contains(
                "import('../../../../target/frontend/flow-generated-imports'"));
    }

    @Test
    public void should_ensureValidRelativePath_whenItHasNoRelativePrefix() {
        String customPath = TaskGenerateIndexTs.ensureValidRelativePath(
                "../custom-frontend/generated-flow-imports.js");
        Assert.assertEquals(
                "Should not append './' if it is already a relative path",
                "../custom-frontend/generated-flow-imports.js", customPath);

        customPath = TaskGenerateIndexTs.ensureValidRelativePath(
                "custom-frontend/generated-flow-imports.js");
        Assert.assertEquals(
                "Should append './' if it doesn't start with a relative path",
                "./custom-frontend/generated-flow-imports.js", customPath);
    }
}
