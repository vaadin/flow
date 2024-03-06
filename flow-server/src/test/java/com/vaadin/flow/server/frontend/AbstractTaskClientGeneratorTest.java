/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class AbstractTaskClientGeneratorTest {

    private static final String TEST_STRING = "Hello world";

    @Test
    public void writeIfChanged_writesWithChanges() throws Exception {
        File f = File.createTempFile("writeIfChanged", "aaa");
        FileUtils.write(f, TEST_STRING, StandardCharsets.UTF_8);

        Assert.assertTrue(AbstractTaskClientGenerator.writeIfChanged(f,
                TEST_STRING + "2"));
    }

    @Test
    public void writeIfChanged_doesNotWriteWithoutChanges() throws Exception {
        File f = File.createTempFile("writeIfChanged", "aaa");
        FileUtils.write(f, TEST_STRING, StandardCharsets.UTF_8);
        Assert.assertFalse(
                AbstractTaskClientGenerator.writeIfChanged(f, TEST_STRING));
    }
}
