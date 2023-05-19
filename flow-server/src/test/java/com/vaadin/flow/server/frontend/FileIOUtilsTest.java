package com.vaadin.flow.server.frontend;

import java.io.File;
import java.net.URL;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.vaadin.open.OSUtils;

public class FileIOUtilsTest {

    @Test
    public void projectFolderOnWindows() throws Exception {
        Assume.assumeTrue(OSUtils.isWindows());

        URL url = new URL(
                "file:/C:/Users/John%20Doe/Downloads/my-app%20(21)/my-app/target/classes/");
        Assert.assertEquals(
                new File("C:\\Users\\John Doe\\Downloads\\my-app (21)\\my-app"),
                FileIOUtils.getProjectFolderFromClasspath(url));
    }

    @Test
    public void projectFolderOnMacOrLinux() throws Exception {
        Assume.assumeFalse(OSUtils.isWindows());

        URL url = new URL(
                "file:/Users/John%20Doe/Downloads/my-app%20(21)/my-app/target/classes/");
        Assert.assertEquals(
                new File("/Users/John Doe/Downloads/my-app (21)/my-app"),
                FileIOUtils.getProjectFolderFromClasspath(url));
    }

    @Test
    public void tempFilesAreTempFiles() {
        Assert.assertTrue(
                FileIOUtils.isProbablyTemporaryFile(new File("foo.txt~")));
        Assert.assertFalse(
                FileIOUtils.isProbablyTemporaryFile(new File("foo.txt")));
    }
}
