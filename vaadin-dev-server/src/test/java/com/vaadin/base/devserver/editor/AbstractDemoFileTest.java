package com.vaadin.base.devserver.editor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;

public abstract class AbstractDemoFileTest {
    Editor editor;
    File testFile;

    @Before
    public void setup() throws IOException {
        editor = new Editor();
        testFile = File.createTempFile("test", ".java");
        copy("DemoFile", testFile);
    }

    protected void assertTestFileNotContains(String search) throws IOException {
        Assert.assertFalse(testFileContains(search));
    }

    protected void assertTestFileContains(String search) throws IOException {
        Assert.assertTrue(testFileContains(search));
    }

    private boolean testFileContains(String search) throws IOException {
        String content = IOUtils.toString(testFile.toURI(),
                StandardCharsets.UTF_8);
        return content.contains(search);
    }

    protected int getLineNumber(File testFile, String search)
            throws IOException {
        String content = IOUtils.toString(testFile.toURI(),
                StandardCharsets.UTF_8);
        String[] rows = content.split("\n");
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].contains(search)) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException("Could not find '" + search + "'");
    }

    private void copy(String className, File testFile) throws IOException {
        URL res = getClass().getResource("inputs/" + className + ".java");
        IOUtils.copy(res, testFile);
    }

}
