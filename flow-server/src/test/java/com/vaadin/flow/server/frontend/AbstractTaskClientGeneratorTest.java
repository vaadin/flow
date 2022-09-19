package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.nimbusds.jose.util.StandardCharset;

public class AbstractTaskClientGeneratorTest {

    private static final String TEST_STRING = "Hello world";

    private static FileTime getModificationTime(File f) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(f.toPath(),
                BasicFileAttributes.class);
        return attr.lastModifiedTime();
    }

    @Test
    public void writeIfChanged_writesWithChanges() throws Exception {
        File f = File.createTempFile("writeIfChanged", "aaa");
        FileUtils.write(f, TEST_STRING, StandardCharset.UTF_8);
        FileTime modTime = getModificationTime(f);

        Thread.sleep(1);

        AbstractTaskClientGenerator.writeIfChanged(f, TEST_STRING + "2");
        Assert.assertNotEquals(modTime, getModificationTime(f));
    }

    @Test
    public void writeIfChanged_doesNotWriteWithoutChanges() throws Exception {
        File f = File.createTempFile("writeIfChanged", "aaa");
        FileUtils.write(f, TEST_STRING, StandardCharset.UTF_8);
        FileTime modTime = getModificationTime(f);

        Thread.sleep(1);

        AbstractTaskClientGenerator.writeIfChanged(f, TEST_STRING);
        Assert.assertEquals(modTime, getModificationTime(f));

    }
}
