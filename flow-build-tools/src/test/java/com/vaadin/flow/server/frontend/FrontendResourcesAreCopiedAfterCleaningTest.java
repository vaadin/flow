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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.testutil.TestUtils;

import static com.vaadin.flow.server.Constants.TARGET;

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
        File dir = getJarFrontendResourcesFolder();
        FileUtils.forceMkdir(dir);
        List<String> files = TestUtils.listFilesRecursively(dir);

        Assert.assertEquals("Should have frontend files", fileCount,
                files.size());
    }

    private File getJarFrontendResourcesFolder() {
        return new File(npmFolder,
                Paths.get(FrontendUtils.DEFAULT_FRONTEND_DIR,
                        FrontendUtils.GENERATED,
                        FrontendUtils.JAR_RESOURCES_FOLDER).toString());
    }

    private void copyResources() throws ExecutionFailedException {
        ClassFinder classFinder = new ClassFinder.DefaultClassFinder(
                FrontendResourcesAreCopiedAfterCleaningTest.class
                        .getClassLoader());
        Lookup mockLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(classFinder).when(mockLookup)
                .lookup(ClassFinder.class);
        Options options = new Options(mockLookup, npmFolder)
                .withBuildDirectory(TARGET);

        new NodeTasks(options.withEmbeddableWebComponents(false)
                .enableImportsUpdate(false).createMissingPackageJson(true)
                .enableImportsUpdate(true).withRunNpmInstall(false)
                .enablePackagesUpdate(true).withFrontendDirectory(npmFolder)
                .withJarFrontendResourcesFolder(getJarFrontendResourcesFolder())
                .copyResources(Collections.singleton(testJar))
                .withBuildResultFolders(npmFolder, npmFolder)).execute();
    }

    private void performPackageClean() throws ExecutionFailedException {
        ClassFinder classFinder = new ClassFinder.DefaultClassFinder(
                FrontendResourcesAreCopiedAfterCleaningTest.class
                        .getClassLoader());
        Lookup mockLookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(classFinder).when(mockLookup)
                .lookup(ClassFinder.class);
        Options options = new Options(mockLookup, npmFolder)
                .withBuildDirectory(TARGET).withEmbeddableWebComponents(false)
                .enableImportsUpdate(false).createMissingPackageJson(true)
                .enableImportsUpdate(true).withRunNpmInstall(false)
                .enableNpmFileCleaning(true).withFrontendDirectory(npmFolder)
                .withJarFrontendResourcesFolder(getJarFrontendResourcesFolder())
                .copyResources(Collections.emptySet())
                .withBuildResultFolders(npmFolder, npmFolder)
                .enablePackagesUpdate(true);
        new NodeTasks(options).execute();
    }
}
