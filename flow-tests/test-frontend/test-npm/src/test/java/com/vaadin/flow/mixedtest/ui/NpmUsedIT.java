package com.vaadin.flow.mixedtest.ui;

import java.io.File;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.junit.Assert;
import org.junit.Test;

public class NpmUsedIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/context-path/servlet-path/route-path";
    }

    @Test
    public void npmIsUsed() {
        open(); // This is to wait for "npm install" to complete in dev mode
        assertProjectFileExists("package-lock.json",
                "npm should have been used to install frontend dependencies but no package-lock.json was found");
        assertProjectFileNotExists("pnpm-lock.yaml",
                "npm should have been used to install frontend dependencies but a pnpm-lock.yaml was found");
    }

    private void assertProjectFileExists(String file, String errorMessage) {
        Assert.assertTrue(errorMessage, new File(file).exists());
    }

    private void assertProjectFileNotExists(String file, String errorMessage) {
        Assert.assertFalse(errorMessage, new File(file).exists());
    }
}
