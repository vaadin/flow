package com.vaadin.base.devserver.editor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;

public abstract class AbstractClassBasedTest {
    Editor editor;
    File testFile;

    @Before
    public void setup() throws Exception {
        editor = new Editor();
        testFile = File.createTempFile("test", ".java");
    }

    protected void setupTestClass(String testClassName) throws IOException {
        copy(testClassName, testFile);

    }

    protected void assertTestFileNotContains(String search) throws IOException {
        Assert.assertFalse(testFileContains(search));
    }

    protected void assertTestFileContains(String search) throws IOException {
        Assert.assertTrue("Expected test file to contain:\n\n" + search
                + "\n\nbut the file contained only\n\n" + getTestFileContents(),
                testFileContains(search));
    }

    private boolean testFileContains(String search) throws IOException {
        return getTestFileContents().contains(search);
    }

    protected String getTestFileContents() throws IOException {
        return IOUtils.toString(testFile.toURI(), StandardCharsets.UTF_8);
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
