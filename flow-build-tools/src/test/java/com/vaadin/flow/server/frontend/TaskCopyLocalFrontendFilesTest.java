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
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.tests.util.MockOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskCopyLocalFrontendFilesTest {

    @TempDir
    File temporaryFolder;

    @Test
    void directoryWithReadOnlyFile_copyIsNotReadOnly() throws IOException {
        final File sourceFolder = createReadOnlySource();

        final File outFolder = new File(temporaryFolder, "out");

        TaskCopyLocalFrontendFiles.copyLocalResources(sourceFolder, outFolder);

        final File copiedReadOnly = new File(outFolder, "readOnly.txt");
        assertTrue(copiedReadOnly.canWrite(),
                "Copied files should be writable even when source is readOnly");

    }

    @Test
    void directoryWithReadOnlyFile_canCopyMultipleTimesToSource()
            throws IOException {
        final File sourceFolder = createReadOnlySource();

        final File outFolder = new File(temporaryFolder, "out");

        TaskCopyLocalFrontendFiles.copyLocalResources(sourceFolder, outFolder);

        TaskCopyLocalFrontendFiles.copyLocalResources(sourceFolder, outFolder);
    }

    @Test
    void execute_copiedFilesAreTracked() throws IOException {
        final File sourceFolder = createReadOnlySource();

        final File outFolder = new File(temporaryFolder, "out");

        Options options = new MockOptions(temporaryFolder)
                .withJarFrontendResourcesFolder(outFolder)
                .copyLocalResources(sourceFolder);

        GeneratedFilesSupport generatedFileSupport = new GeneratedFilesSupport();
        TaskCopyLocalFrontendFiles task = new TaskCopyLocalFrontendFiles(
                options);
        task.setGeneratedFileSupport(generatedFileSupport);
        task.execute();

        final File copiedReadOnly = new File(outFolder, "readOnly.txt");
        assertTrue(copiedReadOnly.canWrite(),
                "Copied files should be writable even when source is readOnly");

        assertEquals(Set.of(copiedReadOnly.toPath()),
                generatedFileSupport.getFiles(),
                "Copied files should have been tracked");
    }

    private File createReadOnlySource() throws IOException {
        final File sourceFolder = new File(temporaryFolder, "source");
        File readOnly = new File(sourceFolder, "readOnly.txt");
        readOnly.createNewFile();
        assertTrue(readOnly.setReadOnly(), "Could not make file read-only");

        return sourceFolder;
    }
}
