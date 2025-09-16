/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
        assertCopiedFrontendFileAmount(17);

        performPackageClean();
        assertCopiedFrontendFileAmount(0);

        copyResources();
        assertCopiedFrontendFileAmount(17);
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
