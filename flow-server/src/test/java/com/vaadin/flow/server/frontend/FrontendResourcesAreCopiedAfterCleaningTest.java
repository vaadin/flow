/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

public class FrontendResourcesAreCopiedAfterCleaningTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File npmFolder;

    private File testJar = TestUtils
            .getTestJar("jar-with-frontend-resources.jar");

    @Before
    public void setup() throws IOException, ExecutionFailedException {
        npmFolder = temporaryFolder.getRoot();

    }

    @Test
    public void frontendResources_should_beCopiedFromJars_when_TaskUpdatePackagesRemovesThem()
            throws IOException, ExecutionFailedException {
        copyResources();
        assertCopiedFrontendFileAmount(2);

        performPackageClean();
        assertCopiedFrontendFileAmount(0);

        copyResources();
        assertCopiedFrontendFileAmount(2);
    }

    private void assertCopiedFrontendFileAmount(int fileCount)
            throws IOException {
        File dir = new File(npmFolder, "node_modules/@vaadin/flow-frontend");
        FileUtils.forceMkdir(dir);
        List<String> files = TestUtils.listFilesRecursively(dir);

        Assert.assertEquals("Should have frontend files", fileCount,
                files.size());
    }

    private void copyResources() throws ExecutionFailedException {
        ClassFinder classFinder = new ClassFinder.DefaultClassFinder(
                FrontendResourcesAreCopiedAfterCleaningTest.class
                        .getClassLoader());
        NodeTasks.Builder builder = new NodeTasks.Builder(classFinder,
                npmFolder);
        builder.withEmbeddableWebComponents(false).enableImportsUpdate(false)
                .createMissingPackageJson(true).enableImportsUpdate(true)
                .runNpmInstall(false).enablePackagesUpdate(true)
                .copyResources(Collections.singleton(testJar)).build()
                .execute();
    }

    private void performPackageClean() throws ExecutionFailedException {
        ClassFinder classFinder = new ClassFinder.DefaultClassFinder(
                FrontendResourcesAreCopiedAfterCleaningTest.class
                        .getClassLoader());
        NodeTasks.Builder builder = new NodeTasks.Builder(classFinder,
                npmFolder);
        builder.withEmbeddableWebComponents(false).enableImportsUpdate(false)
                .createMissingPackageJson(true).enableImportsUpdate(true)
                .runNpmInstall(false).enableNpmFileCleaning(true)
                .enablePackagesUpdate(true).build().execute();
    }
}
