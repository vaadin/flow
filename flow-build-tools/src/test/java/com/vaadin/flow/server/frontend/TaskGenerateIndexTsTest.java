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
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;

import static com.vaadin.flow.internal.FrontendUtils.FRONTEND;
import static com.vaadin.flow.internal.FrontendUtils.INDEX_JS;
import static com.vaadin.flow.internal.FrontendUtils.INDEX_TS;
import static com.vaadin.flow.internal.FrontendUtils.INDEX_TSX;
import static com.vaadin.flow.server.Constants.TARGET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskGenerateIndexTsTest {
    @TempDir
    File temporaryFolder;

    private File frontendFolder;
    private File generatedImports;
    private TaskGenerateIndexTs taskGenerateIndexTs;

    private Options options;

    @BeforeEach
    void setUp() throws IOException {
        frontendFolder = new File(temporaryFolder, FRONTEND);
        File generatedFolder = new File(new File(temporaryFolder, TARGET),
                FRONTEND);
        generatedFolder.mkdirs();
        generatedImports = new File(generatedFolder,
                "flow-generated-imports.js");
        generatedImports.createNewFile();
        options = new Options(Mockito.mock(Lookup.class), temporaryFolder)
                .withFrontendDirectory(frontendFolder)
                .withBuildDirectory(TARGET);

        taskGenerateIndexTs = new TaskGenerateIndexTs(options);
    }

    @Test
    void should_reported_routing_client_when_IndexJsExists() throws Exception {
        Files.createFile(new File(frontendFolder, INDEX_JS).toPath());
        taskGenerateIndexTs.execute();
        assertTrue(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_CLIENT.equals(e.getName())));
    }

    @Test
    void should_reported_routing_client_when_IndexTsExists() throws Exception {
        Files.createFile(new File(frontendFolder, INDEX_TS).toPath());
        taskGenerateIndexTs.execute();
        assertTrue(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_CLIENT.equals(e.getName())));
    }

    @Test
    void should_reported_routing_client_when_IndexTsxExists() throws Exception {
        Files.createFile(new File(frontendFolder, INDEX_TSX).toPath());
        taskGenerateIndexTs.execute();
        assertTrue(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_CLIENT.equals(e.getName())));
    }

    @Test
    void should_not_reported_routing_client() throws Exception {
        taskGenerateIndexTs.execute();
        assertFalse(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_CLIENT.equals(e.getName())));
    }

    @Test
    void should_notGenerateIndexTs_IndexJsExists() throws Exception {
        Files.createFile(new File(frontendFolder, INDEX_JS).toPath());
        taskGenerateIndexTs.execute();
        assertFalse(taskGenerateIndexTs.shouldGenerate(),
                "Should not generate index.ts when index.js exists in"
                        + " the frontend folder");
        assertFalse(taskGenerateIndexTs.getGeneratedFile().exists(),
                "The generated file should not exists");
    }

    @Test
    void should_notGenerateIndexTs_IndexTsExists() throws Exception {
        Files.createFile(new File(frontendFolder, INDEX_TS).toPath());
        taskGenerateIndexTs.execute();
        assertFalse(taskGenerateIndexTs.shouldGenerate(),
                "Should not generate index.ts when index.ts exists in"
                        + " the frontend folder");
        assertFalse(taskGenerateIndexTs.getGeneratedFile().exists(),
                "The generated file should not exists");
    }

    @Test
    void should_generateIndexJs_IndexJsNotExist() throws Exception {

        taskGenerateIndexTs.execute();
        assertTrue(taskGenerateIndexTs.shouldGenerate(),
                "Should generate index.ts when it doesn't exist in"
                        + " the frontend folder");
        assertTrue(taskGenerateIndexTs.getGeneratedFile().exists(),
                "The generated file should exists");

        assertEquals(taskGenerateIndexTs.getFileContent(),
                IOUtils.toString(taskGenerateIndexTs.getGeneratedFile().toURI(),
                        StandardCharsets.UTF_8),
                "Should have default content of index.ts");
    }

    @Test
    void should_ensureValidRelativePath_whenItHasNoRelativePrefix() {
        String customPath = TaskGenerateIndexTs.ensureValidRelativePath(
                "../custom-frontend/generated-flow-imports.js");
        assertEquals("../custom-frontend/generated-flow-imports.js", customPath,
                "Should not append './' if it is already a relative path");

        customPath = TaskGenerateIndexTs.ensureValidRelativePath(
                "custom-frontend/generated-flow-imports.js");
        assertEquals("./custom-frontend/generated-flow-imports.js", customPath,
                "Should append './' if it doesn't start with a relative path");
    }
}
