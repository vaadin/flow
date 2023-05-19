package com.vaadin.flow.testutil;

import java.io.File;
import java.nio.file.Files;

import org.junit.Assert;

public class FileTestUtil {

    /**
     * Waits for the given file to be present for up to 5 minutes.
     *
     * @param file
     *            the file to wait for
     */
    public static void waitForFile(File file) {
        long start = System.currentTimeMillis();
        long timeout = 60 * 5;

        while (System.currentTimeMillis() - start < timeout * 1000) {
            if (file.exists()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        throw new IllegalStateException(
                "File " + file.getAbsolutePath() + " does not exist");
    }

    /**
     * Asserts the given file is a directory.
     *
     * @param file
     *            the file to check
     * @param errorMessage
     *            the error message to fail with if the file is not a directory
     */
    public static void assertDirectory(File file, String errorMessage) {
        Assert.assertTrue(errorMessage, file.isDirectory());
        Assert.assertFalse(errorMessage, Files.isSymbolicLink(file.toPath()));
    }

    /**
     * Asserts the given file is a symlink.
     *
     * @param file
     *            the file to check
     * @param errorMessage
     *            the error message to fail with if the file is not a symlink
     */
    public static void assertSymlink(File file, String errorMessage) {
        Assert.assertTrue(errorMessage, Files.isSymbolicLink(file.toPath()));
    }

}
