package com.vaadin.flow.server.frontend;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class AbstractTaskClientGeneratorTest {

    private static final String TEST_STRING = "Hello world";

    @Test
    public void writeIfChanged_writesWithChanges() throws Exception {
        File f = File.createTempFile("writeIfChanged", "aaa");
        FileUtils.write(f, TEST_STRING, StandardCharsets.UTF_8);

        Assert.assertTrue(FileIOUtils.writeIfChanged(f, TEST_STRING + "2"));
    }

    @Test
    public void writeIfChanged_doesNotWriteWithoutChanges() throws Exception {
        File f = File.createTempFile("writeIfChanged", "aaa");
        FileUtils.write(f, TEST_STRING, StandardCharsets.UTF_8);
        Assert.assertFalse(FileIOUtils.writeIfChanged(f, TEST_STRING));
    }
}
