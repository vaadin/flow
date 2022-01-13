package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

    private File createReadOnlySource() throws IOException {
        final File sourceFolder = temporaryFolder.newFolder("source");
        File readOnly = new File(sourceFolder, "readOnly.txt");
        readOnly.createNewFile();
        Assert.assertTrue("Could not make file read-only",
                readOnly.setReadOnly());

        return sourceFolder;
    }
}
