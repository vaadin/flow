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
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.tests.util.MockOptions;

public class TaskCopyLocalFrontendFilesTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void directoryWithReadOnlyFile_copyIsNotReadOnly()
            throws IOException {
        final File sourceFolder = createReadOnlySource();

        final File outFolder = temporaryFolder.newFolder("out");

        TaskCopyLocalFrontendFiles.copyLocalResources(sourceFolder, outFolder);

        final File copiedReadOnly = new File(outFolder, "readOnly.txt");
        Assert.assertTrue(
                "Copied files should be writable even when source is readOnly",
                copiedReadOnly.canWrite());

    }

    @Test
    public void directoryWithReadOnlyFile_canCopyMultipleTimesToSource()
            throws IOException {
        final File sourceFolder = createReadOnlySource();

        final File outFolder = temporaryFolder.newFolder("out");

        TaskCopyLocalFrontendFiles.copyLocalResources(sourceFolder, outFolder);

        TaskCopyLocalFrontendFiles.copyLocalResources(sourceFolder, outFolder);
    }

    @Test
    public void execute_copiedFilesAreTracked() throws IOException {
        final File sourceFolder = createReadOnlySource();

        final File outFolder = temporaryFolder.newFolder("out");

        Options options = new MockOptions(temporaryFolder.getRoot())
                .withJarFrontendResourcesFolder(outFolder)
                .copyLocalResources(sourceFolder);

        GeneratedFilesSupport generatedFileSupport = new GeneratedFilesSupport();
        TaskCopyLocalFrontendFiles task = new TaskCopyLocalFrontendFiles(
                options);
        task.setGeneratedFileSupport(generatedFileSupport);
        task.execute();

        final File copiedReadOnly = new File(outFolder, "readOnly.txt");
        Assert.assertTrue(
                "Copied files should be writable even when source is readOnly",
                copiedReadOnly.canWrite());

        Assert.assertEquals("Copied files should have been tracked",
                Set.of(copiedReadOnly.toPath()),
                generatedFileSupport.getFiles());
    }

    private File createReadOnlySource() throws IOException {
        final File sourceFolder = temporaryFolder.newFolder("source");
        File readOnly = new File(sourceFolder, "readOnly.txt");
        readOnly.createNewFile();
        Assert.assertTrue("Could not make file read-only",
                readOnly.setReadOnly());

        return sourceFolder;
    }
}
